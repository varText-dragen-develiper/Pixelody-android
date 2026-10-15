package com.pixelody.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pixelody.app.R

/**
 * Pixelody Cartridge Quest - Authentic 8/16-bit console adventure palette tokens.
 *
 * Sourced directly from `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/themes/cartridge-quest.theme.json` and the
 * desktop isolate `CARTRIDGE_QUEST_EXACT_ISOLATE/styles/cartridge-quest.css`.
 *
 * Core rule: Saturated primary action (#7B68C9) and reward yellow (#F2C94C)
 * live against a deep CRT navy frame (#10131F) with hardware console plastic.
 */
object CartridgeQuestPalette {
    // Primary brand & mechanics
    val ActionViolet = Color(0xFF7B68C9)          // Primary action buttons & active navigation
    val RewardYellow = Color(0xFFF2C94C)          // Playback state, XP reward & currency gold
    val PowerGreen = Color(0xFF55C98A)            // Power lamp LED, battery high, confirmed state (--quest-green)
    val AlertRed = Color(0xFFE34B4B)              // Critical warning, battery low, erase state (--quest-red)

    // Chassis & hardware ground
    val CrtNavy = Color(0xFF10131F)               // CRT glass & night ground
    val ChassisGray = Color(0xFF262A3B)           // Main console molded body (--quest-panel)
    val RecessedBay = Color(0xFF161928)           // Recessed cartridge socket
    val PlasticHighlight = Color(0xFF34384B)      // Molded chassis bevel edge (--quest-panel-2)
    val ContactPinGold = Color(0xFFE5B83B)        // Metallic PCB cartridge connector pins

    // Typography
    val TextWhite = Color(0xFFF4F4F8)             // Pixel typography high-contrast text
    val TextMuted = Color(0xFFB8B5B0)             // Secondary / muted text (--quest-muted #b8b5b0)

    /**
     * The source's `text` is #F0ECE4 at luma 236 - four points short of the ink
     * tier, which starts at 240. Raised to the tier in its own hue, so the
     * warmth survives and the contrast law holds.
     *
     * The source's `muted` #B8B5B0 (luma 181) is deliberately absent. It is a
     * near-neutral grey inside the empty middle band L60-L240, which is the one
     * thing this direction will not carry: a second, quieter ink. Hierarchy
     * comes from mass and position.
     */
    val Ink = Color(0xFFF8F6F2)                   // 18.1:1 at the ramp head, 11.9:1 at its foot

    // Viewport Ground Ramp (Top -> Bottom)
    val GroundTop = Color(0xFF0A0C13)             // --quest-bg #10131F, re-solved to luma 12
    val GroundBottom = Color(0xFF333138)          // surface3 #4D4B55, re-solved to luma 50

    // Trace tier, derived at 1.47:1 against this theme's own ramp head.
    val TraceHead = Color(0xFF272F4B)
    val TraceCeiling = Color(0xFF313B5D)          // luma 59, the last value below the band

    val EditorialBlack = Color(0xFF040508)

    /**
     * Quest Pixel — the theme's authentic pixel font, converted from
     * `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/assets/fonts/GeistPixel-Circle.woff2` to TTF for Compose.
     * Replaces the previous `FontFamily.Monospace` system fallback.
     */
    val QuestPixelFont = FontFamily(
        Font(R.font.geist_pixel_circle, FontWeight.Normal),
        Font(R.font.geist_pixel_circle, FontWeight.Bold)
    )
}

/**
 * Pixelody Obsidian Glass - Liquid black acrylic & floating glass palette tokens.
 *
 * Sourced directly from `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/themes/obsidian-glass.theme.json` and
 * `https://github.com/varText-dragen-develiper/pixelody/blob/main/docs/themes/THEME_CATALOG.md` (§ Obsidian Glass).
 *
 * Core rule: Pure black acrylic with floating smoked glass panels,
 * prism cyan transport actions, and violet/pink edge refraction.
 */
object ObsidianGlassPalette {
    val ObsidianBlack = Color(0xFF020306)         // Deep acrylic vacuum (--obsidian-ink)

    /**
     * The panels are alpha washes in the source, not solids - that translucency
     * IS the glass. Composited over this theme's ramp head they read #0A0D15 and
     * #0F141E, but they must stay alpha so whatever sits behind them shows
     * through, which is the whole effect.
     */
    val GlassPanel = Color(0xA80B0E14)            // Semi-transparent floating panels (--obsidian-panel, .66)
    val SmokedGraphite = Color(0xD111161F)        // Raised floating panels (--obsidian-panel-strong, .82)
    val GlassBorder = Color(0x2EDFE7F3)           // 20% white 1px specular rim (--obsidian-edge, 18% #DFE7F3)
    val GlassHighlight = Color(0x57DFE7F3)        // Top specular reflection (--obsidian-edge-strong, 34%)

    // The source prism is pastel, not saturated. These were Tailwind sky-400,
    // purple-500, pink-500 and slate-100; below are obsidian-glass.css's own
    // values. Mint was missing from this file entirely.
    val PrismCyan = Color(0xFF9FDCFF)             // Primary transport & active signal (--obsidian-blue)
    val PrismViolet = Color(0xFFD8BDFF)           // Refraction accent (--obsidian-violet)
    val PrismPink = Color(0xFFFFC3DF)             // Waveform highlight (--obsidian-pink)
    val PrismMint = Color(0xFFC1FFEC)             // Confirmed playback (--obsidian-mint)
    val FrostWhite = Color(0xFFF7F7F2)            // Primary text (--obsidian-text)

    /**
     * DERIVED. The source declares no error colour at all - eleven custom
     * properties and not one of them is a warning. This is a red-hue member of
     * the same pastel family: the prism accents' own saturation, solved to their
     * luma band, so it alarms without leaving the theme.
     */
    val PrismAlarm = Color(0xFFFFB4B4)            // 12.1:1 on this theme's editorial black

    /**
     * Dark counterpart for type sitting ON a prism accent. Every accent here is
     * pastel at luma 196-240, where white gives 1.1-1.7:1 - unreadable. This
     * gives 12.1-18.2:1 instead.
     */
    val GlassInk = Color(0xFF03050A)

    /**
     * `--obsidian-muted` #9CA7B5 is deliberately absent. In the source it is the
     * second, quieter ink; on Android onSurfaceVariant resolves to full [Ink]
     * instead. Its chroma of 25 clears the near-neutral test by one point, so
     * the gate would pass it - the reason it is gone is the law, not the gate.
     */
    val Ink = Color(0xFFF6F6F1)                   // --obsidian-text, raised to the ink tier

    val GroundTop = Color(0xFF080C18)             // --obsidian-ink, re-solved to luma 12
    val GroundBottom = Color(0xFF28334A)          // surface3 #1A2130, re-solved to luma 50

    val TraceHead = Color(0xFF20305F)
    val TraceCeiling = Color(0xFF273B76)
}

/**
 * Pixelody Lo-Fi Cafe - Rainy walnut listening nook tokens.
 *
 * Source: `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/lo-fi-cafe.css`, sixteen named custom properties, cross-checked
 * against `renderer.js` and the 2026-09-22 Canvas reconstruction. Every value
 * below is sampled; the two marked DERIVED are the exceptions.
 *
 * The only theme in the set with TWO polarities. Its structure is dark walnut,
 * but its content surfaces are light paper with dark type - `--cafe-paper`
 * #D8C49E carrying `--cafe-ink` #211812. That is not decoration, it is the
 * theme's stated identity ("dark wood structure, paper-soft content surfaces"),
 * so it ports as a declared paper plane rather than being flattened to the dark
 * ground. See PaperPlane in PixelodyThemeSpec.kt for what that costs and buys.
 *
 * The source's two cafe photographs do not port. The ground is a ramp bound to
 * the viewport, not a picture, and a photograph behind live type cannot hold a
 * contrast ratio.
 */
object LoFiCafePalette {
    val Amber = Color(0xFFC48A4A)                 // --cafe-amber, playback light & identity
    val Honey = Color(0xFFE1B775)                 // --cafe-honey
    val Moss = Color(0xFF5D704F)                  // --cafe-moss, confirmed playback
    val Sage = Color(0xFF8B9875)                  // --cafe-sage
    val Rain = Color(0xFF71838A)                  // --cafe-rain, muted rain-glass depth

    /** DERIVED. The source palette contains no red at all. */
    val Ember = Color(0xFFB4543A)

    val Night = Color(0xFF100B08)                 // --cafe-night, already luma 12.0 as sampled
    val Espresso = Color(0xFF1D130E)              // --cafe-espresso
    val Wood = Color(0xFF62452B)                  // --cafe-wood
    val Walnut = Color(0xFF342216)                // --cafe-walnut

    // The paper plane. Sampled, not derived - including its trace, which is an
    // alpha wash in the source and stays one here.
    val Paper = Color(0xFFD8C49E)                 // --cafe-paper, luma 197
    val PaperSoft = Color(0xFFECDFC3)             // --cafe-paper-soft
    val PaperInk = Color(0xFF211812)              // --cafe-ink, 10.2:1 on the paper
    val PaperTrace = Color(0x472B1D14)            // --cafe-line rgba(43,29,20,.28), 1.70:1 on the paper

    val Ink = Color(0xFFFAF6EE)                   // --cafe-paper-soft, raised to the ink tier

    val GroundTop = Color(0xFF100B08)             // --cafe-night, sampled unchanged
    val GroundBottom = Color(0xFF462E1E)          // --cafe-walnut, re-solved to luma 50

    val TraceHead = Color(0xFF402C20)
    val TraceCeiling = Color(0xFF503728)

    val EditorialBlack = Color(0xFF070503)
}

/**
 * Pixelody Bulkhead Terminal - Industrial science-fiction terminal HUD tokens.
 *
 * Source: `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/bulkhead-terminal-theme.css`, which declares no custom
 * properties, so these are its most-used literals - mint #78F09A 25 times,
 * amber #F6C945 14, ground #090C0B 12 - cross-checked against `renderer.js`
 * and `playback-performance.css`.
 *
 * The only theme here with no white ink in its source. A terminal's ink is its
 * phosphor, so [Ink] is the pale mint raised to the tier and type reads
 * green-white rather than neutral.
 *
 * Two source values cannot port: #59605C (luma 94) and #737B76 (luma 121) are
 * near-neutral greys inside the empty middle band. Both were doing trace work
 * and become the trace tier.
 */
object BulkheadTerminalPalette {
    val Amber = Color(0xFFF6C945)                 // Identity, selection, command
    val MintPhosphor = Color(0xFF78F09A)          // Confirmed playback
    val PaleMint = Color(0xFFB0FFC3)              // Secondary readout
    val AlertRed = Color(0xFFFF4C35)              // Alert, 5 uses

    val Ground = Color(0xFF090C0B)                // 12 uses, the source ground
    val Panel = Color(0xFF111513)                 // 8 uses
    val PanelDeep = Color(0xFF07110B)             // 5 uses

    val Ink = Color(0xFFDDFFE5)                   // #B0FFC3 raised to the ink tier

    val GroundTop = Color(0xFF0A0D0C)             // #090C0B, re-solved to luma 12
    val GroundBottom = Color(0xFF2E3330)          // #383F3B, re-solved to luma 50

    val TraceHead = Color(0xFF27322E)
    val TraceCeiling = Color(0xFF303E3A)

    val EditorialBlack = Color(0xFF040505)        // #050706, 4 uses
}

/**
 * Pixelody Obsession - Restrained psychological-thriller poster tokens.
 *
 * Source: `renderer.js` theme table, `canvas-theme-ports.css`
 * (--canvas-port-* for this theme) and `playback-performance.css`
 * (--signal-*). Manifest: `https://github.com/varText-dragen-develiper/pixelody/blob/main/src/themes/obsession.theme.json`.
 *
 * Two things shape this port.
 *
 * First, the manifest reserves crimson "for playback and selection", so the
 * ground ramp is derived from the near-neutral base #030203 and NOT from
 * surface3 #21070B. Ramping through the crimson was the obvious reading and it
 * is wrong twice over: it spends the theme's one reserved colour on its largest
 * surface, and it drops ink contrast at the ramp foot to 9.2:1 against the
 * 11.7-12.0 every other theme here holds. Crimson stays for state; the ground
 * stays near-black; the panels carry the crimson tint.
 *
 * Second, this theme has exactly one hue. Material asks for four accent roles
 * plus an error, and they cannot all be crimson and still mean different
 * things, so [Alarm] shares [Signal]'s value. Error is told apart from
 * confirmed playback by plate geometry and copy, never by hue alone - which is
 * what the process doc requires of every theme and what this one enforces by
 * having no alternative.
 *
 * The manifest's `backgroundTexture` is a licensed Unsplash photograph. It does
 * not port: the ground is a viewport-bound ramp, and no asset invents runtime
 * truth. The `heroOverlay` fracture geometry is original and does port, as the
 * pixel-cut chamfer the manifest asks for.
 */
object ObsessionPalette {
    val Intent = Color(0xFFFF5865)                // --canvas-port-intent, selection
    val Confirmed = Color(0xFFC91527)             // --canvas-port-confirmed, playback
    val Signal = Color(0xFFE52B3D)               // --signal-color
    val Glow = Color(0xFF8B0712)                  // --signal-glow
    val Alarm = Color(0xFFE52B3D)                 // shares Signal - see the note above

    val Base = Color(0xFF030203)                  // bg, near-neutral by design
    val Panel = Color(0xFF0B0607)                 // surface
    val PanelDeep = Color(0xFF12080A)             // surface2
    val Hero = Color(0xFF2A070C)                  // hero, the one crimson-dark plate
    val Fracture = Color(0xFF3D1118)              // line, the fracture hairline

    val Ink = Color(0xFFF9F6F4)                   // text #F2ECE8 (luma 237) raised to the tier

    val GroundTop = Color(0xFF100B10)             // #030203, re-solved to luma 12
    val GroundBottom = Color(0xFF422C42)          // #030203, re-solved to luma 50

    val TraceHead = Color(0xFF3D2A3D)
    val TraceCeiling = Color(0xFF4C344C)

    val EditorialBlack = Color(0xFF070407)
}
