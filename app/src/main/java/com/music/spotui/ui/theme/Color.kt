package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val AppBackground: Color get() = if (AppearanceSettings.effectiveDark) Color.Black else Color(0xFFF7F7F7)
val GridBackground: Color get() = if (AppearanceSettings.effectiveDark) Color(0xFF2A2A2A) else Color(0xFFEAEAEA)
val AppPalette: Color get() = if (AppearanceSettings.effectiveDark) Color(0xFF1ED760) else Color(0xFF087B35)
val AppText: Color get() = if (AppearanceSettings.effectiveDark) Color.White else Color(0xFF171717)
val AppSecondary: Color get() = if (AppearanceSettings.effectiveDark) Color(0xFFB3B3B3) else Color(0xFF626262)
val AppSurface: Color get() = if (AppearanceSettings.effectiveDark) Color(0xFF202020) else Color.White
val AppRaised: Color get() = if (AppearanceSettings.effectiveDark) Color(0xFF333333) else Color(0xFFE5E5E5)

fun artworkHeader(color: Color): Color = if (AppearanceSettings.effectiveDark) color else androidx.compose.ui.graphics.lerp(AppBackground, color, 0.16f)

val PlayerText: Color get() = if (AppearanceSettings.artworkScreen.value) Color.White else AppText
val PlayerSecondary: Color get() = if (AppearanceSettings.artworkScreen.value) Color(0xFFB3B3B3) else AppSecondary
