package com.pixelody.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelody.app.data.model.DacConnectionState
import com.pixelody.app.data.model.DacHardwareProfile
import com.pixelody.app.data.model.DacTelemetryState
import com.pixelody.app.data.model.DacVolumeMode
import com.pixelody.app.data.model.DsdPlaybackMode
import com.pixelody.app.data.model.HiResBitDepth
import com.pixelody.app.data.model.HiResLosslessSettings
import com.pixelody.app.data.model.HiResSampleRate

/**
 * HiResLosslessHorizonCard: Interactive audiophile DAC inspection rack and signal-chain visualizer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HiResLosslessHorizonCard(
    telemetry: DacTelemetryState,
    settings: HiResLosslessSettings,
    onSettingsChange: (HiResLosslessSettings) -> Unit,
    onSimulateDac: (DacHardwareProfile) -> Unit = {},
    onResetDac: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isBitPerfect = telemetry.signalChain.isBitPerfectDirect
    val connectionState = telemetry.connectionState

    val primaryAccentColor by animateColorAsState(
        targetValue = when (connectionState) {
            DacConnectionState.ConnectedBitPerfect -> Color(0xFFE5A93C) // Audiophile Warm Gold
            DacConnectionState.ConnectedDirectAudio -> Color(0xFF10B981) // Emerald Green
            DacConnectionState.Detecting -> Color(0xFF38BDF8) // Sky Blue
            DacConnectionState.FallbackSystemMixer -> Color(0xFF94A3B8) // Slate
            DacConnectionState.Disconnected -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "dacAccentColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            primaryAccentColor.copy(alpha = if (isBitPerfect) 0.6f else 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Diamond Lossless Status Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(primaryAccentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PixelodyTransportGlyph(
                            glyph = TransportGlyphType.DiamondLossless,
                            color = primaryAccentColor,
                            size = 16.dp
                        )
                    }

                    Column {
                        Text(
                            text = "HI-RES LOSSLESS HORIZON",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccentColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = connectionState.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Bit-Perfect Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryAccentColor.copy(alpha = if (isBitPerfect) 0.25f else 0.12f),
                    border = BorderStroke(
                        1.dp,
                        primaryAccentColor.copy(alpha = if (isBitPerfect) 0.8f else 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(primaryAccentColor)
                        )
                        Text(
                            text = if (isBitPerfect) "1:1 DIRECT" else "MIXER",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = primaryAccentColor
                        )
                    }
                }
            }

            // Signal Chain Pipeline Diagram
            SignalChainDiagram(
                telemetry = telemetry,
                accentColor = primaryAccentColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Hardware Inspector Card
            HardwareInspectorSection(
                profile = telemetry.hardwareProfile,
                activeSampleRate = telemetry.signalChain.dacOutputRate,
                accentColor = primaryAccentColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Audiophile Settings Section
            AudiophileSettingsSection(
                settings = settings,
                onSettingsChange = onSettingsChange,
                accentColor = primaryAccentColor
            )

            // DAC Simulator Preset Bar
            DacSimulatorBar(
                isExternal = telemetry.hardwareProfile.isExternalUsbDac,
                onSimulateDac = { profile ->
                    haptic.performTick()
                    onSimulateDac(profile)
                },
                onResetDac = {
                    haptic.performTick()
                    onResetDac()
                },
                accentColor = primaryAccentColor
            )
        }
    }
}

/**
 * Real-time 4-Stage Audiophile Signal Chain Diagram.
 */
@Composable
private fun SignalChainDiagram(
    telemetry: DacTelemetryState,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val chain = telemetry.signalChain
    val source = chain.sourceSpec

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "AUDIO SIGNAL PATH & DIRECT ROUTING",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stage 1: Source File
            SignalChainNode(
                stageTitle = "SOURCE",
                formatText = source.codec.displayName.substringBefore(" "),
                specText = "${source.sampleRate.displayName} / ${source.bitDepth.bitDepth}-bit",
                isOk = source.isLossless,
                accentColor = accentColor
            )

            // Flow Connector 1
            SignalConnector(isOk = chain.isDecoderLossless, accentColor = accentColor)

            // Stage 2: Decoder Engine
            SignalChainNode(
                stageTitle = "DECODER",
                formatText = if (source.isDsd) "Bitstream" else "Linear PCM",
                specText = if (chain.dspBypassed) "Direct 0dBFS" else "Float Atten",
                isOk = chain.dspBypassed,
                accentColor = accentColor
            )

            // Flow Connector 2
            SignalConnector(isOk = chain.isOsResamplerBypassed, accentColor = accentColor)

            // Stage 3: Direct Buffer
            SignalChainNode(
                stageTitle = "BUFFER",
                formatText = if (chain.isOsResamplerBypassed) "FLAG_DIRECT" else "Resampled",
                specText = if (chain.isOsResamplerBypassed) "Bypass Mixer" else "48kHz System",
                isOk = chain.isOsResamplerBypassed,
                accentColor = accentColor
            )

            // Flow Connector 3
            SignalConnector(isOk = chain.isBitPerfectDirect, accentColor = accentColor)

            // Stage 4: DAC Output Transducer
            SignalChainNode(
                stageTitle = "DAC OUTPUT",
                formatText = if (telemetry.hardwareProfile.isExternalUsbDac) "USB DAC" else "Internal",
                specText = "${chain.dacOutputRate.displayName} / ${chain.dacOutputDepth.bitDepth}-bit",
                isOk = chain.isExactClockMatch && chain.isExactBitDepthMatch,
                accentColor = accentColor
            )
        }
    }
}

@Composable
private fun SignalChainNode(
    stageTitle: String,
    formatText: String,
    specText: String,
    isOk: Boolean,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stageTitle,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isOk) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, if (isOk) accentColor.copy(alpha = 0.5f) else Color.Transparent)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (isOk) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = specText,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SignalConnector(isOk: Boolean, accentColor: Color) {
    Text(
        text = "→",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = if (isOk) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    )
}

/**
 * DAC Hardware Inspection Grid.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HardwareInspectorSection(
    profile: DacHardwareProfile,
    activeSampleRate: HiResSampleRate,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = profile.deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${profile.manufacturer} • USB VID:${profile.usbVendorId} PID:${profile.usbProductId}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            if (profile.supportsDsdDoP) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "DSD / DoP CAPABLE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }

        Text(
            text = "HARDWARE NATIVE SAMPLE RATES",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        // Supported Sample Rates Flow Chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                HiResSampleRate.Rate_44_1kHz,
                HiResSampleRate.Rate_48kHz,
                HiResSampleRate.Rate_88_2kHz,
                HiResSampleRate.Rate_96kHz,
                HiResSampleRate.Rate_176_4kHz,
                HiResSampleRate.Rate_192kHz,
                HiResSampleRate.Rate_352_8kHz,
                HiResSampleRate.Rate_384kHz
            ).forEach { rate ->
                val isSupported = profile.supportedSampleRates.contains(rate)
                val isActive = activeSampleRate == rate

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isActive -> accentColor.copy(alpha = 0.35f)
                        isSupported -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        else -> Color.Black.copy(alpha = 0.2f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isActive -> accentColor
                            isSupported -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }
                    )
                ) {
                    Text(
                        text = rate.displayName.substringBefore(" "),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isActive -> accentColor
                            isSupported -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Audiophile Toggles and Controls.
 */
@Composable
private fun AudiophileSettingsSection(
    settings: HiResLosslessSettings,
    onSettingsChange: (HiResLosslessSettings) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Bit-Perfect Direct Passthrough Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bit-Perfect Direct Passthrough",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Transmit bit-for-bit unadulterated PCM/DoP directly to DAC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.isBitPerfectEnabled,
                onCheckedChange = {
                    haptic.performTick()
                    onSettingsChange(settings.copy(isBitPerfectEnabled = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.3f)
                )
            )
        }

        // Direct PCM Bypass Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bypass Android OS Resampler",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Use FLAG_DIRECT_OUTPUT to bypass 48kHz mixer downsampling",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.directPcmBypass,
                onCheckedChange = {
                    haptic.performTick()
                    onSettingsChange(settings.copy(directPcmBypass = it))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.3f)
                )
            )
        }

        // Volume Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Volume Mode",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair(DacVolumeMode.HardwareDirect, "0dB Direct"),
                    Pair(DacVolumeMode.SoftwareFloat, "32-bit Float")
                ).forEach { (mode, label) ->
                    val isSelected = settings.volumeControlMode == mode
                    Surface(
                        onClick = {
                            haptic.performTick()
                            onSettingsChange(settings.copy(volumeControlMode = mode))
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simulator bar allowing instant 1-tap testing of audiophile DAC models.
 */
@Composable
private fun DacSimulatorBar(
    isExternal: Boolean,
    onSimulateDac: (DacHardwareProfile) -> Unit,
    onResetDac: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "STUDIO DAC SIMULATOR PREVIEWS",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = {
                    onSimulateDac(
                        DacHardwareProfile(
                            deviceName = "FiiO Q3 MQA Hi-Res DAC",
                            manufacturer = "FiiO Electronics",
                            usbVendorId = "2972",
                            usbProductId = "0043",
                            supportedSampleRates = listOf(
                                HiResSampleRate.Rate_44_1kHz,
                                HiResSampleRate.Rate_48kHz,
                                HiResSampleRate.Rate_88_2kHz,
                                HiResSampleRate.Rate_96kHz,
                                HiResSampleRate.Rate_176_4kHz,
                                HiResSampleRate.Rate_192kHz,
                                HiResSampleRate.Rate_384kHz
                            ),
                            supportedBitDepths = listOf(HiResBitDepth.Bit_16, HiResBitDepth.Bit_24, HiResBitDepth.Bit_32_Int),
                            supportsDsdDoP = true,
                            isExternalUsbDac = true,
                            maxSampleRateHz = 384000
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "FiiO Q3 (384k)",
                    modifier = Modifier.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                onClick = {
                    onSimulateDac(
                        DacHardwareProfile(
                            deviceName = "AudioQuest DragonFly Cobalt",
                            manufacturer = "AudioQuest",
                            usbVendorId = "2192",
                            usbProductId = "0053",
                            supportedSampleRates = listOf(
                                HiResSampleRate.Rate_44_1kHz,
                                HiResSampleRate.Rate_48kHz,
                                HiResSampleRate.Rate_88_2kHz,
                                HiResSampleRate.Rate_96kHz
                            ),
                            supportedBitDepths = listOf(HiResBitDepth.Bit_16, HiResBitDepth.Bit_24),
                            supportsDsdDoP = false,
                            isExternalUsbDac = true,
                            maxSampleRateHz = 96000
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "DragonFly (96k)",
                    modifier = Modifier.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isExternal) {
                Surface(
                    onClick = onResetDac,
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Red.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Reset",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
        }
    }
}
