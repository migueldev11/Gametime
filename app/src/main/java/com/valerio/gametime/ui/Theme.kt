package com.valerio.gametime.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Steam {
    val Bg = Color(0xFF1B2838)
    val BgDark = Color(0xFF171A21)
    val Panel = Color(0xFF1F3044)
    val Panel2 = Color(0xFF2A475E)
    val Blue = Color(0xFF66C0F4)
    val BlueBtn = Color(0xFF1A9FFF)
    val Green = Color(0xFF5BA32B)
    val Text = Color(0xFFC7D5E0)
    val Muted = Color(0xFF8F98A0)
    val Gold = Color(0xFFFFC857)
    val Danger = Color(0xFFE5645A)
}

@Composable
fun GameTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Steam.Blue,
            onPrimary = Steam.BgDark,
            background = Steam.Bg,
            onBackground = Steam.Text,
            surface = Steam.Panel,
            onSurface = Steam.Text,
            surfaceVariant = Steam.Panel2,
            onSurfaceVariant = Steam.Muted
        ),
        content = content
    )
}
