package com.pixelody.app.feature.connection

import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.LuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.common.GlobalHistogramBinarizer
import java.nio.ByteBuffer

/** Reads QR codes from camera frames with ZXing. Not thread-safe; use from one analysis thread. */
internal class QrDecoder {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true,
            )
        )
    }

    /** Returns the decoded QR text, or null when no QR code is in the frame. */
    fun decode(image: ImageProxy): String? {
        val plane = image.planes.firstOrNull() ?: return null
        val crop = image.cropRect
        val luma = copyQrLuminance(
            plane.buffer, plane.rowStride, plane.pixelStride,
            crop.left, crop.top, crop.width(), crop.height()
        )
        return decodeLuminance(luma, crop.width(), crop.height(), image.imageInfo.rotationDegrees)
    }

    internal fun decodeLuminance(luma: ByteArray, width: Int, height: Int, rotationDegrees: Int = 0): String? {
        val (data, w, h) = rotate(luma, width, height, rotationDegrees)
        val source = PlanarYUVLuminanceSource(data, w, h, 0, 0, w, h, false)
        // Local thresholding handles shadows; global thresholding helps a flat
        // monitor image. Inversion also permits light modules on dark surfaces.
        return decodeSource(source) ?: decodeSource(source.invert())
    }

    private fun decodeSource(source: LuminanceSource): String? =
        decodeBitmap(BinaryBitmap(HybridBinarizer(source)))
            ?: decodeBitmap(BinaryBitmap(GlobalHistogramBinarizer(source)))

    private fun decodeBitmap(bitmap: BinaryBitmap): String? {
        return try {
            reader.decodeWithState(bitmap).text
        } catch (_: ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }

    private fun rotate(src: ByteArray, w: Int, h: Int, degrees: Int): Triple<ByteArray, Int, Int> {
        if (degrees % 360 == 0) return Triple(src, w, h)
        val out = ByteArray(src.size)
        return when (degrees % 360) {
            90 -> {
                for (y in 0 until h) for (x in 0 until w) out[x * h + (h - 1 - y)] = src[y * w + x]
                Triple(out, h, w)
            }
            180 -> {
                for (i in src.indices) out[src.size - 1 - i] = src[i]
                Triple(out, w, h)
            }
            270 -> {
                for (y in 0 until h) for (x in 0 until w) out[(w - 1 - x) * h + y] = src[y * w + x]
                Triple(out, h, w)
            }
            else -> Triple(src, w, h)
        }
    }
}

/** Copy the visible Y plane without changing the camera buffer's position. */
internal fun copyQrLuminance(
    buffer: ByteBuffer, rowStride: Int, pixelStride: Int,
    left: Int, top: Int, width: Int, height: Int
): ByteArray {
    require(rowStride > 0 && pixelStride > 0 && left >= 0 && top >= 0 && width > 0 && height > 0)
    val source = buffer.duplicate()
    val start = source.position()
    val luma = ByteArray(width * height)
    for (y in 0 until height) {
        val offset = start + (top + y) * rowStride + left * pixelStride
        if (pixelStride == 1) {
            source.position(offset)
            source.get(luma, y * width, width)
        } else {
            for (x in 0 until width) luma[y * width + x] = source.get(offset + x * pixelStride)
        }
    }
    return luma
}

internal fun String.payloadCandidates(): List<String> =
    listOf(trim().trim('\uFEFF')).filter { it.isNotBlank() }
