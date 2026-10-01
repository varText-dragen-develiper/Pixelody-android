package com.pixelody.app.feature.connection

import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

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
        val width = image.width
        val height = image.height
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val luma = ByteArray(width * height)
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(luma, row * width, width)
        }
        val (data, w, h) = rotate(luma, width, height, image.imageInfo.rotationDegrees)
        val source = PlanarYUVLuminanceSource(data, w, h, 0, 0, w, h, false)
        return try {
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: NotFoundException) {
            null
        } catch (_: com.google.zxing.ReaderException) {
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

internal fun String.payloadCandidates(): List<String> =
    listOf(trim().trim('﻿')).filter { it.isNotBlank() }
