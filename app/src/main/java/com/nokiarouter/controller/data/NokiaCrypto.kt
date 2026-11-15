package com.nokiarouter.controller.data

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SessionKeys(
    val decKeyBase64: String,
    val decIvBase64: String,
    val decKeyBytes: ByteArray,
    val decIvBytes: ByteArray
)

data class EncryptedLoginResult(
    val postBody: String,
    val sessionKeys: SessionKeys
)

object NokiaCrypto {

    fun base64urlEscape(str: String): String {
        return str.replace("+", "-")
            .replace("/", "_")
            .replace("=", ".")
    }

    fun base64urlUnescape(str: String): String {
        return str.replace("-", "+")
            .replace("_", "/")
            .replace(".", "=")
    }

    fun sha256(str1: String, str2: String): String {
        val input = "$str1:$str2"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }

    fun sha256url(str1: String, str2: String): String {
        return base64urlEscape(sha256(str1, str2))
    }

    fun parseRsaPublicKey(pem: String): RSAPublicKey {
        val cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(cleanPem)
        val spec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(spec) as RSAPublicKey
    }

    fun rsaEncrypt(publicKey: RSAPublicKey, plainText: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun aesEncryptCbc(key: ByteArray, iv: ByteArray, plainText: String): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
    }

    fun aesDecryptCbc(key: ByteArray, iv: ByteArray, cipherBytes: ByteArray): String {
        val secretKey = SecretKeySpec(key, "AES")
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decrypted = cipher.doFinal(cipherBytes)
        return String(decrypted, Charsets.UTF_8)
    }

    fun buildEncryptedLoginPayload(
        pubKeyPem: String,
        nonce: String,
        randomKey: String,
        username: String,
        password: String
    ): EncryptedLoginResult {
        val secureRandom = SecureRandom()

        // 1. Generate session keys for subsequent decrypts
        val decKeyBytes = ByteArray(16)
        val decIvBytes = ByteArray(16)
        secureRandom.nextBytes(decKeyBytes)
        secureRandom.nextBytes(decIvBytes)

        val decKeyB64 = Base64.getEncoder().encodeToString(decKeyBytes)
        val decIvB64 = Base64.getEncoder().encodeToString(decIvBytes)

        // 2. Assemble raw query string Q
        val escapedNonce = base64urlEscape(nonce)
        val escapedDecKey = base64urlEscape(decKeyB64)
        val escapedDecIv = base64urlEscape(decIvB64)

        val rawQ = StringBuilder()
            .append("userhash=").append(username)
            .append("&RandomKeyhash=").append(randomKey)
            .append("&response=").append(java.net.URLEncoder.encode(password, "UTF-8"))
            .append("&nonce=").append(escapedNonce)
            .append("&enckey=").append(escapedDecKey)
            .append("&enciv=").append(escapedDecIv)
            .append("&nohash=1&hPassword=undefined")
            .toString()

        // 3. Generate ephemeral AES key & IV for encrypting Q
        val aesKeyBytes = ByteArray(16)
        val aesIvBytes = ByteArray(16)
        secureRandom.nextBytes(aesKeyBytes)
        secureRandom.nextBytes(aesIvBytes)

        val ctBuffer = aesEncryptCbc(aesKeyBytes, aesIvBytes, rawQ)
        // base64url encoding for ct (sjcl base64url has no padding '=' and uses '-' and '_')
        val ctB64Url = Base64.getEncoder().withoutPadding().encodeToString(ctBuffer)
            .replace("+", "-")
            .replace("/", "_")

        // 4. RSA Encrypt "aesKeyB64 aesIvB64"
        val aesKeyB64 = Base64.getEncoder().encodeToString(aesKeyBytes)
        val aesIvB64 = Base64.getEncoder().encodeToString(aesIvBytes)
        val rsaPlainText = "$aesKeyB64 $aesIvB64"

        val pubKey = parseRsaPublicKey(pubKeyPem)
        val t0 = rsaEncrypt(pubKey, rsaPlainText)
        val ck = base64urlEscape(t0)

        val postBody = "encrypted=1&ct=$ctB64Url&ck=$ck"
        val sessionKeys = SessionKeys(decKeyB64, decIvB64, decKeyBytes, decIvBytes)

        return EncryptedLoginResult(postBody, sessionKeys)
    }

    fun encryptPostWithCsrf(
        pubKeyPem: String,
        csrfToken: String,
        params: String? = null
    ): String {
        val body = if (params.isNullOrEmpty()) {
            "csrf_token=$csrfToken"
        } else {
            "$params&csrf_token=$csrfToken"
        }

        if (pubKeyPem.isEmpty()) {
            return body
        }

        val secureRandom = SecureRandom()
        val aesKeyBytes = ByteArray(16)
        val aesIvBytes = ByteArray(16)
        secureRandom.nextBytes(aesKeyBytes)
        secureRandom.nextBytes(aesIvBytes)

        val ctBuffer = aesEncryptCbc(aesKeyBytes, aesIvBytes, body)
        val ctB64Url = Base64.getEncoder().withoutPadding().encodeToString(ctBuffer)
            .replace("+", "-")
            .replace("/", "_")

        val aesKeyB64 = Base64.getEncoder().encodeToString(aesKeyBytes)
        val aesIvB64 = Base64.getEncoder().encodeToString(aesIvBytes)
        val rsaPlainText = "$aesKeyB64 $aesIvB64"

        val pubKey = parseRsaPublicKey(pubKeyPem)
        val t0 = rsaEncrypt(pubKey, rsaPlainText)
        val ck = base64urlEscape(t0)

        return "encrypted=1&ct=$ctB64Url&ck=$ck"
    }
}
