package com.pixelody.app.feature.connection

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.pixelody.app.data.model.HostConnectionDetails
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class QrDecoderTest {
    private val payload = "pxd1|http%3A%2F%2F192.168.1.45%3A4822|042917|pairing-secret"

    private fun qr(inverted: Boolean = false): ByteArray {
        val bits = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240)
        return ByteArray(240 * 240) { i ->
            if (bits[i % 240, i / 240] != inverted) 0 else 255.toByte()
        }
    }

    @Test fun readsNormalAndInvertedCodesAtEveryCameraRotation() {
        val decoder = QrDecoder()
        for (inverted in listOf(false, true)) for (rotation in listOf(0, 90, 180, 270)) {
            assertEquals("inverted=$inverted rotation=$rotation", payload, decoder.decodeLuminance(qr(inverted), 240, 240, rotation))
        }
    }

    @Test fun ignoresBlankFramesAndRecoversOnTheNextFrame() {
        val decoder = QrDecoder()
        assertNull(decoder.decodeLuminance(ByteArray(240 * 240) { 255.toByte() }, 240, 240))
        assertEquals(payload, decoder.decodeLuminance(qr(), 240, 240))
    }

    @Test fun copiesCroppedPaddedFramesWithoutMovingTheCameraBuffer() {
        val data = qr()
        // Nonzero buffer origin, row padding, crop origin and interleaved samples.
        val rowStride = 512
        val bytes = ByteArray(7 + rowStride * 244) { 127 }
        for (y in 0 until 240) for (x in 0 until 240) {
            bytes[7 + (y + 2) * rowStride + (x + 3) * 2] = data[y * 240 + x]
        }
        val buffer = ByteBuffer.wrap(bytes).apply { position(7) }
        val luma = copyQrLuminance(buffer, rowStride, 2, 3, 2, 240, 240)
        assertEquals(7, buffer.position())
        assertArrayEquals(data, luma)
        assertEquals(payload, QrDecoder().decodeLuminance(luma, 240, 240))
    }

    @Test fun copiesContiguousPixelsWithRowPaddingAndNoFinalPadding() {
        val buffer = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 99, 4, 5, 6))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6), copyQrLuminance(buffer, 4, 1, 0, 0, 3, 2))
        assertEquals(0, buffer.position())
    }

    @Test fun decodesDesktopGeneratedInviteAndKeepsEveryAdvertisedRoute() {
        val lines = requireNotNull(javaClass.getResourceAsStream("/pairing-desktop-qr.txt"))
            .bufferedReader().use { it.readLines() }
        val width = lines[1].length * 4
        val pixels = lines.drop(1).flatMap { row ->
            val expanded = row.flatMap { List(4) { _ -> if (it == '1') 0.toByte() else 255.toByte() } }
            List(4) { expanded }.flatten()
        }.toByteArray()
        val text = QrDecoder().decodeLuminance(pixels, width, (lines.size - 1) * 4)
        assertEquals(lines[0], text)
        val details = requireNotNull(HostConnectionDetails.fromText(requireNotNull(text)))
        assertEquals(listOf("http://100.72.0.9:4822", "http://192.168.1.45:4822", "http://127.0.0.1:4822"), details.baseUrls)
        assertEquals("042917", details.pairingCode)
        assertEquals("pairing-secret", details.pairingSecret)
    }
}
