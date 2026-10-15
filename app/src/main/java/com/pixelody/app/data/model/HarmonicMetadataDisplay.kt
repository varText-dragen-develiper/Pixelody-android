package com.pixelody.app.data.model

import androidx.compose.ui.graphics.Color

val CamelotKey?.knownCode: String get() = this?.code ?: "Unknown"
val CamelotKey?.knownTitle: String get() = this?.fullTitle ?: "Key unavailable"
val CamelotKey?.knownMusicalKey: String get() = this?.musicalKey ?: "Key unavailable"
val CamelotKey?.knownColor: Color get() = this?.harmonicColor ?: Color(0xFF94A3B8)
val Float?.knownBpm: String get() = this?.let { "$it BPM" } ?: "Tempo unavailable"
val Int?.knownPercent: String get() = this?.let { "$it%" } ?: "Unknown"
