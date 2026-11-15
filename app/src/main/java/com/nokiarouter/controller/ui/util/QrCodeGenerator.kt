package com.nokiarouter.controller.ui.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeGenerator {

    fun generateWifiQrBitmap(ssid: String, password: String, isOpen: Boolean): ImageBitmap? {
        val qrContent = if (isOpen || password.isEmpty()) {
            "WIFI:T:nopass;S:$ssid;;"
        } else {
            "WIFI:T:WPA;S:$ssid;P:$password;;"
        }

        return try {
            val size = 512
            val bitMatrix = QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) -0x1000000 else -0x1)
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
