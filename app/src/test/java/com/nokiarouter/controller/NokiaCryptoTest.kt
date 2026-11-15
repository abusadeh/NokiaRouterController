package com.nokiarouter.controller

import com.nokiarouter.controller.data.NokiaCrypto
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator

class NokiaCryptoTest {

    @Test
    fun testBase64UrlEscaping() {
        val input = "abc+def/ghi=="
        val escaped = NokiaCrypto.base64urlEscape(input)
        assertEquals("abc-def_ghi..", escaped)

        val unescaped = NokiaCrypto.base64urlUnescape(escaped)
        assertEquals(input, unescaped)
    }

    @Test
    fun testSha256Digest() {
        val hash = NokiaCrypto.sha256("admin", "123456")
        assertNotNull(hash)
        assertTrue(hash.isNotEmpty())

        val urlHash = NokiaCrypto.sha256url("admin", "123456")
        assertFalse(urlHash.contains("+"))
        assertFalse(urlHash.contains("/"))
    }

    @Test
    fun testAesEncryptDecrypt() {
        val key = ByteArray(16) { 0x01 }
        val iv = ByteArray(16) { 0x02 }
        val message = "userhash=admin&random=123"

        val cipherBytes = NokiaCrypto.aesEncryptCbc(key, iv, message)
        assertNotNull(cipherBytes)
        assertTrue(cipherBytes.isNotEmpty())

        val decrypted = NokiaCrypto.aesDecryptCbc(key, iv, cipherBytes)
        assertEquals(message, decrypted)
    }

    @Test
    fun testBuildEncryptedLoginPayloadWithGeneratedRsaKey() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(1024)
        val keyPair = keyGen.generateKeyPair()

        val pubKeyBytes = keyPair.public.encoded
        val pubKeyB64 = java.util.Base64.getEncoder().encodeToString(pubKeyBytes)
        val pem = "-----BEGIN PUBLIC KEY-----\n$pubKeyB64\n-----END PUBLIC KEY-----"

        val result = NokiaCrypto.buildEncryptedLoginPayload(
            pubKeyPem = pem,
            nonce = "sample_test_nonce_123==",
            randomKey = "999",
            username = "admin",
            password = "secret_password"
        )

        assertNotNull(result.postBody)
        assertTrue(result.postBody.startsWith("encrypted=1&ct="))
        assertTrue(result.postBody.contains("&ck="))
        assertNotNull(result.sessionKeys)
        assertEquals(16, result.sessionKeys.decKeyBytes.size)
        assertEquals(16, result.sessionKeys.decIvBytes.size)
    }
}
