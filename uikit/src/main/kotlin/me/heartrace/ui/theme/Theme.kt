package me.heartrace.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class HeartracePalette(
    val ink: Color,
    val panel: Color,
    val panelLight: Color,
    val cream: Color,
    val muted: Color,
    val coral: Color,
    val lemon: Color,
    val mint: Color,
    val sky: Color,
    val violet: Color,
    val amber: Color,
)

private val DarkPalette = HeartracePalette(
    ink = Color(0xFF111918),
    panel = Color(0xFF1A2422),
    panelLight = Color(0xFF22302D),
    cream = Color(0xFFF4F0E7),
    muted = Color(0xFFAAB8AF),
    coral = Color(0xFFFF7257),
    lemon = Color(0xFFF2C94C),
    mint = Color(0xFF56C596),
    sky = Color(0xFF64B5F6),
    violet = Color(0xFFBB86FC),
    amber = Color(0xFFFFA726),
)

private val LightPalette = HeartracePalette(
    ink = Color(0xFFF8F5EE),
    panel = Color(0xFFFFFFFF),
    panelLight = Color(0xFFEBE6DA),
    cream = Color(0xFF1A2624),
    muted = Color(0xFF5C6B65),
    coral = Color(0xFFE0533C),
    lemon = Color(0xFFD99B26),
    mint = Color(0xFF2E9A70),
    sky = Color(0xFF2B82B8),
    violet = Color(0xFF7C52AB),
    amber = Color(0xFFD97706),
)

val LocalHeartracePalette = staticCompositionLocalOf { DarkPalette }

object HeartraceTheme {
    val colors: HeartracePalette
        @Composable get() = LocalHeartracePalette.current
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPalette.coral,
    secondary = DarkPalette.lemon,
    tertiary = DarkPalette.mint,
    background = DarkPalette.ink,
    surface = DarkPalette.panel,
    onPrimary = DarkPalette.ink,
    onSecondary = DarkPalette.ink,
    onTertiary = DarkPalette.ink,
    onBackground = DarkPalette.cream,
    onSurface = DarkPalette.cream,
)

private val LightColorScheme = lightColorScheme(
    primary = LightPalette.coral,
    secondary = LightPalette.lemon,
    tertiary = LightPalette.mint,
    background = LightPalette.ink,
    surface = LightPalette.panel,
    onPrimary = Color.White,
    onSecondary = LightPalette.cream,
    onTertiary = Color.White,
    onBackground = LightPalette.cream,
    onSurface = LightPalette.cream,
)

@Composable
fun HeartraceMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val targetPalette = if (darkTheme) DarkPalette else LightPalette
    val palette = HeartracePalette(
        ink = animateColorAsState(targetPalette.ink, label = "theme ink").value,
        panel = animateColorAsState(targetPalette.panel, label = "theme panel").value,
        panelLight = animateColorAsState(targetPalette.panelLight, label = "theme panel light").value,
        cream = animateColorAsState(targetPalette.cream, label = "theme cream").value,
        muted = animateColorAsState(targetPalette.muted, label = "theme muted").value,
        coral = animateColorAsState(targetPalette.coral, label = "theme coral").value,
        lemon = animateColorAsState(targetPalette.lemon, label = "theme lemon").value,
        mint = animateColorAsState(targetPalette.mint, label = "theme mint").value,
        sky = animateColorAsState(targetPalette.sky, label = "theme sky").value,
        violet = animateColorAsState(targetPalette.violet, label = "theme violet").value,
        amber = animateColorAsState(targetPalette.amber, label = "theme amber").value,
    )
    CompositionLocalProvider(LocalHeartracePalette provides palette) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
