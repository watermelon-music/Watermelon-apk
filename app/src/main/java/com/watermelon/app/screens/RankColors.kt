package com.watermelon.app.screens

import androidx.compose.ui.graphics.Color

val RankColors = mapOf(
    "🌱 Seed Listener" to Color(0xFF4CAF50),
    "🍃 Sprout Wave" to Color(0xFF8BC34A),
    "🎧 Pulse Rider" to Color(0xFF00BCD4),
    "🌊 Echo Drift" to Color(0xFF03A9F4),
    "🎶 Resonance" to Color(0xFF2196F3),
    "📀 Vinyl Hunter" to Color(0xFF9C27B0),
    "🎵 Frequency Soul" to Color(0xFF673AB7),
    "🌌 NovaBeat" to Color(0xFF3F51B5),
    "💿 Harmonic Flow" to Color(0xFF1A237E),
    "🔥 Reverb X" to Color(0xFFFF5722),
    "⚡ Soundrift" to Color(0xFFFF9800),
    "🌠 Celestia Tone" to Color(0xFFFFC107),
    "🎼 Wave Architect" to Color(0xFFE91E63),
    "🌈 Spectrum Lord" to Color(0xFFFF0055),
    "👑 Eternal Echo" to Color(0xFFFFD700),
)

fun getRankColor(rank: String?): Color = RankColors[rank] ?: Color.Gray