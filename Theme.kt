package com.savr.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Colours ───────────────────────────────────────────────────────────────────
object SavrColors {
    val Cream      = Color(0xFFF5F0E8)
    val CreamMid   = Color(0xFFEDE8DF)
    val Dark       = Color(0xFF1A1A1A)
    val DarkMid    = Color(0xFF242424)
    val Dark2      = Color(0xFF1E1E1E)
    val Dark3      = Color(0xFF2C2C2C)
    val Divider    = Color(0x12000000)
    val Sage       = Color(0xFF7A9E7E)
    val SageLight  = Color(0xFFA8C5AC)
    val SagePale   = Color(0xFFC8DECA)
    val SageTint   = Color(0xFFEBF3EC)
    val Terra      = Color(0xFFC4622D)
    val TerraTint  = Color(0xFFFAE9DF)
    val Amber      = Color(0xFFD4860B)
    val AmberTint  = Color(0xFFFEF3D9)
    val White      = Color(0xFFFFFFFF)
    val TextMid    = Color(0xFF6B6B6B)
    val TextMuted  = Color(0xFF9B9B9B)
    val CardShadow = Color(0x0D000000)
}

// ── Typography ────────────────────────────────────────────────────────────────
val SavrTypography = Typography(
    // Page titles (Fraunces-style — use FontFamily.Serif as placeholder)
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize   = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize   = 22.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize   = 17.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize   = 10.sp,
        letterSpacing = 0.5.sp
    )
)

// ── Shapes ────────────────────────────────────────────────────────────────────
val SavrShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

// ── Material theme ────────────────────────────────────────────────────────────
private val ColorScheme = lightColorScheme(
    primary            = SavrColors.Sage,
    onPrimary          = SavrColors.White,
    primaryContainer   = SavrColors.SageTint,
    onPrimaryContainer = SavrColors.Sage,
    secondary          = SavrColors.Terra,
    onSecondary        = SavrColors.White,
    background         = SavrColors.Cream,
    onBackground       = SavrColors.Dark,
    surface            = SavrColors.White,
    onSurface          = SavrColors.Dark,
    surfaceVariant     = SavrColors.CreamMid,
    onSurfaceVariant   = SavrColors.TextMid,
    outline            = SavrColors.CreamMid,
    error              = SavrColors.Terra
)

@Composable
fun SavrTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography  = SavrTypography,
        shapes      = SavrShapes,
        content     = content
    )
}
