package app.localbrief.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Forest = Color(0xFF172815)
val WarmWhite = Color(0xFFFFF8F3)
val Sage = Color(0xFF708B75)
val Burgundy = Color(0xFF4C061D)
val Mint = Color(0xFFA9FFCB)
val MutedForest = Color(0xFF526650)
val PaleSage = Color(0xFFDCE5DA)
@Composable fun BriefTheme(mode: String, content: @Composable (Boolean) -> Unit) {
    val dark = when (mode) { "Light" -> false; "Dark" -> true; else -> isSystemInDarkTheme() }
    val colors = lightColorScheme(primary = Forest, onPrimary = WarmWhite, secondary = Burgundy,
        background = if (dark) Forest else WarmWhite, onBackground = if (dark) WarmWhite else Forest,
        surface = if (dark) WarmWhite else Color.White, onSurface = Forest, onSurfaceVariant = MutedForest,
        surfaceVariant = PaleSage, primaryContainer = Mint, onPrimaryContainer = Forest, outline = Sage,
        error = Burgundy, onError = WarmWhite)
    val typography = Typography(
        headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium))
    MaterialTheme(colorScheme = colors, typography = typography) { content(dark) }
}
