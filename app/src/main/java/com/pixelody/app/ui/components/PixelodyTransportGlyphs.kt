package com.pixelody.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified icon types for all transport, navigation, habituation, and source lens controls.
 * Guarantees zero emoji dependency with high-precision vector rendering.
 */
enum class TransportGlyphType {
    Play,
    Pause,
    Previous,
    Next,
    Shuffle,
    Repeat,
    RepeatOne,
    Queue,
    PlayNext,
    AddToQueue,
    Equalizer,
    FlowShuffle,
    Heart,
    HeartFilled,
    Sparkle,
    FlameStreak,
    LightningCheck,
    OmniSource,
    PhoneDevice,
    DesktopHost,
    MeshNetwork,
    OfflineCheck,
    Download,
    Search,
    Close,
    ChevronRight,
    ChevronDown,
    Sliders,
    Settings,
    Share,
    QrScan,
    Lock,
    WaveformBars,
    DiamondLossless,
    VinylDisc,
    PixelodyEmblem,
    Refresh,
    Folder,
    Broadcast,
    Checkmark,
    MoreVertical
}

fun HapticFeedback.performTick() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

fun HapticFeedback.performConfirm() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * High-precision vector canvas glyph rendering with consistent stroke weight, cap, and geometry.
 */
@Composable
fun PixelodyTransportGlyph(
    glyph: TransportGlyphType,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 24.dp,
    sizeDp: Int = 0
) {
    val finalSize = if (sizeDp > 0) sizeDp.dp else size
    Canvas(modifier = modifier.size(finalSize)) {
        val w = drawContext.size.width
        val h = drawContext.size.height
        val strokeWidth = (w * 0.085f).coerceIn(1.5f, 4f)
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (glyph) {
            TransportGlyphType.Play -> {
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.50f)
                    lineTo(w * 0.28f, h * 0.82f)
                    close()
                }
                drawPath(path, color = color, style = Fill)
            }
            TransportGlyphType.Pause -> {
                val barWidth = w * 0.18f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.24f, h * 0.18f),
                    size = Size(barWidth, h * 0.64f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.58f, h * 0.18f),
                    size = Size(barWidth, h * 0.64f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
            TransportGlyphType.Previous -> {
                val barWidth = w * 0.10f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.20f),
                    size = Size(barWidth, h * 0.60f),
                    cornerRadius = CornerRadius(1.5f.dp.toPx())
                )
                val path = Path().apply {
                    moveTo(w * 0.80f, h * 0.20f)
                    lineTo(w * 0.34f, h * 0.50f)
                    lineTo(w * 0.80f, h * 0.80f)
                    close()
                }
                drawPath(path, color = color, style = Fill)
            }
            TransportGlyphType.Next -> {
                val path = Path().apply {
                    moveTo(w * 0.20f, h * 0.20f)
                    lineTo(w * 0.66f, h * 0.50f)
                    lineTo(w * 0.20f, h * 0.80f)
                    close()
                }
                drawPath(path, color = color, style = Fill)
                val barWidth = w * 0.10f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.72f, h * 0.20f),
                    size = Size(barWidth, h * 0.60f),
                    cornerRadius = CornerRadius(1.5f.dp.toPx())
                )
            }
            TransportGlyphType.PlayNext -> {
                // Two forward carets + bottom queue step
                val path1 = Path().apply {
                    moveTo(w * 0.15f, h * 0.25f)
                    lineTo(w * 0.48f, h * 0.50f)
                    lineTo(w * 0.15f, h * 0.75f)
                    close()
                }
                val path2 = Path().apply {
                    moveTo(w * 0.48f, h * 0.25f)
                    lineTo(w * 0.80f, h * 0.50f)
                    lineTo(w * 0.48f, h * 0.75f)
                    close()
                }
                drawPath(path1, color = color, style = Fill)
                drawPath(path2, color = color, style = Fill)
            }
            TransportGlyphType.AddToQueue -> {
                // Queue lines + plus badge in corner
                drawLine(color, Offset(w * 0.15f, h * 0.30f), Offset(w * 0.55f, h * 0.30f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.15f, h * 0.50f), Offset(w * 0.55f, h * 0.50f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.15f, h * 0.70f), Offset(w * 0.85f, h * 0.70f), strokeWidth, StrokeCap.Round)
                // Plus
                drawLine(color, Offset(w * 0.72f, h * 0.25f), Offset(w * 0.72f, h * 0.55f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.57f, h * 0.40f), Offset(w * 0.87f, h * 0.40f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.Shuffle -> {
                drawLine(color, Offset(w * 0.18f, h * 0.30f), Offset(w * 0.44f, h * 0.30f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.44f, h * 0.30f), Offset(w * 0.68f, h * 0.70f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.68f, h * 0.70f), Offset(w * 0.82f, h * 0.70f), strokeWidth, StrokeCap.Round)

                drawLine(color, Offset(w * 0.18f, h * 0.70f), Offset(w * 0.44f, h * 0.70f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.44f, h * 0.70f), Offset(w * 0.68f, h * 0.30f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.68f, h * 0.30f), Offset(w * 0.82f, h * 0.30f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.Repeat, TransportGlyphType.RepeatOne -> {
                drawCircle(color, radius = w * 0.30f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                if (glyph == TransportGlyphType.RepeatOne) {
                    drawLine(color, Offset(w * 0.50f, h * 0.36f), Offset(w * 0.50f, h * 0.64f), strokeWidth, StrokeCap.Round)
                }
            }
            TransportGlyphType.Queue -> {
                drawLine(color, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.20f, h * 0.72f), Offset(w * 0.80f, h * 0.72f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.Equalizer -> {
                drawLine(color, Offset(w * 0.28f, h * 0.20f), Offset(w * 0.28f, h * 0.80f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.35f), Offset(w * 0.50f, h * 0.80f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.72f, h * 0.15f), Offset(w * 0.72f, h * 0.80f), strokeWidth, StrokeCap.Round)
                drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.28f, h * 0.40f))
                drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.60f))
                drawCircle(color, radius = w * 0.07f, center = Offset(w * 0.72f, h * 0.30f))
            }
            TransportGlyphType.FlowShuffle -> {
                // Harmonic ribbon wave
                val wavePath = Path().apply {
                    moveTo(w * 0.18f, h * 0.50f)
                    cubicTo(w * 0.35f, h * 0.20f, w * 0.65f, h * 0.80f, w * 0.82f, h * 0.50f)
                }
                drawPath(wavePath, color = color, style = stroke)
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.18f, h * 0.50f))
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.82f, h * 0.50f))
            }
            TransportGlyphType.Heart, TransportGlyphType.HeartFilled -> {
                val heartPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.82f)
                    cubicTo(w * 0.15f, h * 0.55f, w * 0.10f, h * 0.28f, w * 0.30f, h * 0.20f)
                    cubicTo(w * 0.42f, h * 0.16f, w * 0.48f, h * 0.25f, w * 0.50f, h * 0.32f)
                    cubicTo(w * 0.52f, h * 0.25f, w * 0.58f, h * 0.16f, w * 0.70f, h * 0.20f)
                    cubicTo(w * 0.90f, h * 0.28f, w * 0.85f, h * 0.55f, w * 0.50f, h * 0.82f)
                    close()
                }
                if (glyph == TransportGlyphType.HeartFilled) {
                    drawPath(heartPath, color = color, style = Fill)
                } else {
                    drawPath(heartPath, color = color, style = stroke)
                }
            }
            TransportGlyphType.Sparkle -> {
                // 4-point precision sparkle diamond
                val sparklePath = Path().apply {
                    moveTo(w * 0.50f, h * 0.15f)
                    cubicTo(w * 0.50f, h * 0.38f, w * 0.62f, h * 0.50f, w * 0.85f, h * 0.50f)
                    cubicTo(w * 0.62f, h * 0.50f, w * 0.50f, h * 0.62f, w * 0.50f, h * 0.85f)
                    cubicTo(w * 0.50f, h * 0.62f, w * 0.38f, h * 0.50f, w * 0.15f, h * 0.50f)
                    cubicTo(w * 0.38f, h * 0.50f, w * 0.50f, h * 0.38f, w * 0.50f, h * 0.15f)
                    close()
                }
                drawPath(sparklePath, color = color, style = Fill)
            }
            TransportGlyphType.FlameStreak -> {
                val flamePath = Path().apply {
                    moveTo(w * 0.50f, h * 0.15f)
                    cubicTo(w * 0.65f, h * 0.35f, w * 0.82f, h * 0.50f, w * 0.75f, h * 0.72f)
                    cubicTo(w * 0.68f, h * 0.88f, w * 0.32f, h * 0.88f, w * 0.25f, h * 0.72f)
                    cubicTo(w * 0.18f, h * 0.55f, w * 0.35f, h * 0.38f, w * 0.50f, h * 0.15f)
                    close()
                }
                drawPath(flamePath, color = color, style = Fill)
            }
            TransportGlyphType.LightningCheck -> {
                val boltPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.15f)
                    lineTo(w * 0.25f, h * 0.52f)
                    lineTo(w * 0.48f, h * 0.52f)
                    lineTo(w * 0.42f, h * 0.85f)
                    lineTo(w * 0.75f, h * 0.45f)
                    lineTo(w * 0.52f, h * 0.45f)
                    close()
                }
                drawPath(boltPath, color = color, style = Fill)
            }
            TransportGlyphType.OmniSource -> {
                // Layered concentric lens with 4-point cross-anchor
                drawCircle(color, radius = w * 0.32f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawCircle(color, radius = w * 0.12f, center = Offset(w * 0.50f, h * 0.50f), style = Fill)
                drawLine(color, Offset(w * 0.50f, h * 0.10f), Offset(w * 0.50f, h * 0.24f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.50f, h * 0.76f), Offset(w * 0.50f, h * 0.90f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.10f, h * 0.50f), Offset(w * 0.24f, h * 0.50f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.76f, h * 0.50f), Offset(w * 0.90f, h * 0.50f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.PhoneDevice -> {
                // Modern phone body + home notch indicator
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.26f, h * 0.14f),
                    size = Size(w * 0.48f, h * 0.72f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.58f, h * 0.78f), strokeWidth * 0.9f, StrokeCap.Round)
            }
            TransportGlyphType.DesktopHost -> {
                // Display monitor + base stand
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.48f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(color, Offset(w * 0.50f, h * 0.66f), Offset(w * 0.50f, h * 0.80f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.32f, h * 0.80f), Offset(w * 0.68f, h * 0.80f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.MeshNetwork -> {
                // 3 connected node mesh
                val n1 = Offset(w * 0.50f, h * 0.22f)
                val n2 = Offset(w * 0.22f, h * 0.75f)
                val n3 = Offset(w * 0.78f, h * 0.75f)
                drawLine(color, n1, n2, strokeWidth * 0.8f, StrokeCap.Round)
                drawLine(color, n2, n3, strokeWidth * 0.8f, StrokeCap.Round)
                drawLine(color, n3, n1, strokeWidth * 0.8f, StrokeCap.Round)
                drawCircle(color, radius = w * 0.11f, center = n1)
                drawCircle(color, radius = w * 0.11f, center = n2)
                drawCircle(color, radius = w * 0.11f, center = n3)
            }
            TransportGlyphType.Download -> {
                // Down arrow into tray
                drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.62f), strokeWidth, StrokeCap.Round)
                val arrowHead = Path().apply {
                    moveTo(w * 0.32f, h * 0.46f)
                    lineTo(w * 0.50f, h * 0.64f)
                    lineTo(w * 0.68f, h * 0.46f)
                }
                drawPath(arrowHead, color = color, style = stroke)
                drawLine(color, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.80f, h * 0.80f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.OfflineCheck -> {
                // Storage disc with check badge
                drawCircle(color, radius = w * 0.35f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                val checkPath = Path().apply {
                    moveTo(w * 0.32f, h * 0.50f)
                    lineTo(w * 0.46f, h * 0.64f)
                    lineTo(w * 0.68f, h * 0.36f)
                }
                drawPath(checkPath, color = color, style = stroke)
            }
            TransportGlyphType.Search -> {
                drawCircle(color, radius = w * 0.26f, center = Offset(w * 0.42f, h * 0.42f), style = stroke)
                drawLine(color, Offset(w * 0.60f, h * 0.60f), Offset(w * 0.82f, h * 0.82f), strokeWidth * 1.2f, StrokeCap.Round)
            }
            TransportGlyphType.Close -> {
                drawLine(color, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth, StrokeCap.Round)
            }
            TransportGlyphType.ChevronRight -> {
                val path = Path().apply {
                    moveTo(w * 0.35f, h * 0.22f)
                    lineTo(w * 0.65f, h * 0.50f)
                    lineTo(w * 0.35f, h * 0.78f)
                }
                drawPath(path, color = color, style = stroke)
            }
            TransportGlyphType.ChevronDown -> {
                val path = Path().apply {
                    moveTo(w * 0.22f, h * 0.35f)
                    lineTo(w * 0.50f, h * 0.65f)
                    lineTo(w * 0.78f, h * 0.35f)
                }
                drawPath(path, color = color, style = stroke)
            }
            TransportGlyphType.Sliders -> {
                drawLine(color, Offset(w * 0.20f, h * 0.35f), Offset(w * 0.80f, h * 0.35f), strokeWidth, StrokeCap.Round)
                drawLine(color, Offset(w * 0.20f, h * 0.65f), Offset(w * 0.80f, h * 0.65f), strokeWidth, StrokeCap.Round)
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.40f, h * 0.35f))
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.65f, h * 0.65f))
            }
            TransportGlyphType.Settings -> {
                drawCircle(color, radius = w * 0.20f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                val teeth = 6
                for (i in 0 until teeth) {
                    val rad = (i * 60.0 * Math.PI / 180.0).toFloat()
                    val x1 = w * 0.50f + w * 0.30f * Math.cos(rad.toDouble()).toFloat()
                    val y1 = h * 0.50f + h * 0.30f * Math.sin(rad.toDouble()).toFloat()
                    val x2 = w * 0.50f + w * 0.44f * Math.cos(rad.toDouble()).toFloat()
                    val y2 = h * 0.50f + h * 0.44f * Math.sin(rad.toDouble()).toFloat()
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeWidth * 1.1f, StrokeCap.Round)
                }
            }
            TransportGlyphType.Share -> {
                val p1 = Offset(w * 0.75f, h * 0.25f)
                val p2 = Offset(w * 0.25f, h * 0.50f)
                val p3 = Offset(w * 0.75f, h * 0.75f)
                drawLine(color, p2, p1, strokeWidth * 0.8f, StrokeCap.Round)
                drawLine(color, p2, p3, strokeWidth * 0.8f, StrokeCap.Round)
                drawCircle(color, radius = w * 0.11f, center = p1)
                drawCircle(color, radius = w * 0.11f, center = p2)
                drawCircle(color, radius = w * 0.11f, center = p3)
            }
            TransportGlyphType.QrScan -> {
                val s = w * 0.24f
                // 4 corner reticles
                val r = 2.dp.toPx()
                // TL
                drawRoundRect(color, Offset(w * 0.15f, h * 0.15f), Size(s, s), CornerRadius(r), style = stroke)
                // TR
                drawRoundRect(color, Offset(w * 0.61f, h * 0.15f), Size(s, s), CornerRadius(r), style = stroke)
                // BL
                drawRoundRect(color, Offset(w * 0.15f, h * 0.61f), Size(s, s), CornerRadius(r), style = stroke)
                // Center data dot
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.73f, h * 0.73f))
            }
            TransportGlyphType.Lock -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.24f, h * 0.44f),
                    size = Size(w * 0.52f, h * 0.44f),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                val shackle = Path().apply {
                    moveTo(w * 0.34f, h * 0.44f)
                    lineTo(w * 0.34f, h * 0.28f)
                    cubicTo(w * 0.34f, h * 0.16f, w * 0.66f, h * 0.16f, w * 0.66f, h * 0.28f)
                    lineTo(w * 0.66f, h * 0.44f)
                }
                drawPath(shackle, color = color, style = stroke)
            }
            TransportGlyphType.WaveformBars -> {
                val barW = w * 0.12f
                val r = CornerRadius(2.dp.toPx())
                drawRoundRect(color, Offset(w * 0.16f, h * 0.35f), Size(barW, h * 0.50f), r)
                drawRoundRect(color, Offset(w * 0.36f, h * 0.15f), Size(barW, h * 0.70f), r)
                drawRoundRect(color, Offset(w * 0.56f, h * 0.45f), Size(barW, h * 0.40f), r)
                drawRoundRect(color, Offset(w * 0.76f, h * 0.25f), Size(barW, h * 0.60f), r)
            }
            TransportGlyphType.DiamondLossless -> {
                val diamond = Path().apply {
                    moveTo(w * 0.50f, h * 0.12f)
                    lineTo(w * 0.88f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.88f)
                    lineTo(w * 0.12f, h * 0.50f)
                    close()
                }
                drawPath(diamond, color = color, style = stroke)
                drawLine(color, Offset(w * 0.50f, h * 0.12f), Offset(w * 0.50f, h * 0.88f), strokeWidth * 0.6f)
                drawLine(color, Offset(w * 0.12f, h * 0.50f), Offset(w * 0.88f, h * 0.50f), strokeWidth * 0.6f)
            }
            TransportGlyphType.VinylDisc -> {
                drawCircle(color, radius = w * 0.42f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawCircle(color, radius = w * 0.28f, center = Offset(w * 0.50f, h * 0.50f), style = Stroke(width = strokeWidth * 0.6f))
                drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.50f), style = Fill)
            }
            TransportGlyphType.PixelodyEmblem -> {
                // Iconic brutalist sound lens
                drawCircle(color, radius = w * 0.42f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                val path = Path().apply {
                    moveTo(w * 0.30f, h * 0.32f)
                    lineTo(w * 0.72f, h * 0.50f)
                    lineTo(w * 0.30f, h * 0.68f)
                    close()
                }
                drawPath(path, color = color, style = Fill)
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.75f, h * 0.25f))
            }
            TransportGlyphType.Refresh -> {
                drawArc(
                    color = color,
                    startAngle = 45f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    style = stroke
                )
                val arrow = Path().apply {
                    moveTo(w * 0.70f, h * 0.12f)
                    lineTo(w * 0.86f, h * 0.24f)
                    lineTo(w * 0.70f, h * 0.36f)
                }
                drawPath(arrow, color = color, style = stroke)
            }
            TransportGlyphType.Folder -> {
                val folder = Path().apply {
                    moveTo(w * 0.15f, h * 0.28f)
                    lineTo(w * 0.40f, h * 0.28f)
                    lineTo(w * 0.50f, h * 0.38f)
                    lineTo(w * 0.85f, h * 0.38f)
                    lineTo(w * 0.85f, h * 0.78f)
                    lineTo(w * 0.15f, h * 0.78f)
                    close()
                }
                drawPath(folder, color = color, style = stroke)
            }
            TransportGlyphType.Broadcast -> {
                drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.50f, h * 0.50f), style = Fill)
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.28f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(w * 0.76f, h * 0.76f),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(w * 0.76f, h * 0.76f),
                    style = stroke
                )
            }
            TransportGlyphType.Checkmark -> {
                val check = Path().apply {
                    moveTo(w * 0.22f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.74f)
                    lineTo(w * 0.80f, h * 0.28f)
                }
                drawPath(check, color = color, style = stroke)
            }
            TransportGlyphType.MoreVertical -> {
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.25f))
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.50f))
                drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.75f))
            }
        }
    }
}
