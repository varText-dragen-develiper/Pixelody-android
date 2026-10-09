package com.pixelody.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.pixelody.app.ui.theme.LocalPixelodyThemeVariant
import com.pixelody.app.ui.theme.PixelodyMobileTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelody.app.ui.navigation.PixelodyTab

internal val LocalPagePosition = staticCompositionLocalOf<State<Float>?> { null }

/** One shape vocabulary for the page header and its navigation target. No invented playback data. */
@Composable
internal fun PageIdentity(tab: PixelodyTab, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onBackground
    val position = LocalPagePosition.current
    Row(
        modifier = modifier.testTag("page-identity:${tab.name.lowercase()}")
            .semantics(mergeDescendants = true) { contentDescription = "${tab.label} page" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DestinationGlyph(tab, accent, size = 40.dp)
        if (LocalPixelodyThemeVariant.current == PixelodyMobileTheme.Studio) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("STUDIO", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
            fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.clearAndSetSemantics {})
        // Mirrors Home -> Search -> Library. Position and length communicate selection without color.
        Canvas(Modifier.size(width = 52.dp, height = 16.dp)) {
            val index = when (tab) { PixelodyTab.Search -> 1; PixelodyTab.Library -> 2; else -> 0 }
            val centers = listOf(size.width * .12f, size.width * .5f, size.width * .88f)
            val y = size.height / 2
            drawLine(ink.copy(alpha = .32f), Offset(centers.first(), y), Offset(centers.last(), y), 1.dp.toPx())
            centers.forEach { x -> drawCircle(ink.copy(alpha = .65f), 2.dp.toPx(), Offset(x, y)) }
            val progress = (position?.value ?: index.toFloat()).coerceIn(0f, 2f)
            val markerX = centers.first() + (centers.last() - centers.first()) * progress / 2f
            drawLine(accent, Offset(markerX, y - 5.dp.toPx()), Offset(markerX, y + 5.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
        }
        }
        }
    }
}

/** Scalable, original linework: shelter + record, a lens, and three record sleeves. */
@Composable
internal fun DestinationGlyph(tab: PixelodyTab, color: Color, size: Dp = 24.dp, selected: Boolean = true) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val line = w * if (selected) .065f else .055f
        val stroke = Stroke(line, cap = StrokeCap.Round)
        fun point(x: Float, y: Float) = Offset(w * x, h * y)
        fun segment(x: Float, y: Float, xx: Float, yy: Float) = drawLine(color, point(x,y), point(xx,yy), line, StrokeCap.Round)
        when (tab) {
            PixelodyTab.Home -> {
                segment(.12f,.43f,.5f,.13f); segment(.5f,.13f,.88f,.43f)
                segment(.23f,.43f,.23f,.86f); segment(.77f,.43f,.77f,.86f); segment(.23f,.86f,.77f,.86f)
                drawCircle(color, w * .15f, point(.5f,.62f), style = stroke)
                drawCircle(color, w * .032f, point(.5f,.62f))
            }
            PixelodyTab.Search -> {
                drawCircle(color, w * .28f, point(.41f,.41f), style = stroke)
                segment(.62f,.62f,.87f,.87f)
            }
            PixelodyTab.Library -> {
                drawRoundRect(color, Offset(w*.12f,h*.2f), Size(w*.16f,h*.64f), style = stroke)
                drawRoundRect(color, Offset(w*.4f,h*.2f), Size(w*.16f,h*.64f), style = stroke)
                segment(.68f,.23f,.84f,.2f); segment(.84f,.2f,.95f,.81f)
                segment(.95f,.81f,.79f,.84f); segment(.79f,.84f,.68f,.23f)
                segment(.12f,.65f,.28f,.65f); segment(.4f,.65f,.56f,.65f)
            }
            else -> Unit
        }
    }
}
