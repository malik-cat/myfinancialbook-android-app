package com.myfinancialbook.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OrangePrimary = Color(0xFFE65100)
val OrangeSecondary = Color(0xFFFF9800)
val OrangeLight = Color(0xFFFFF3E0)
val Red = Color(0xFFD32F2F)
val Green = Color(0xFF388E3C)
val LedgerGreen = Green
val Background = Color(0xFFF5F5F5)
val Surface = Color(0xFFFFFFFF)
val Muted = Color(0xFF757575)
val LineColor = Color(0xFFEEEEEE)
val PrimaryBlue = Color(0xFF0D1B2A)
val AccentTeal = Color(0xFF00B4D8)
val TealLight = Color(0xFFB2EBF2)
val BillsBlue = Color(0xFF1E88E5)
val StaffOrange = Color(0xFFF4511E)
val InvoicePurple = Color(0xFF673AB7)
val CalculatorBg = Color(0xFFF5F5F5)

private val LightColors = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    secondary = OrangeSecondary,
    background = Background,
    surface = Surface,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Red
)

@Composable
fun MyFinancialBookTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}