package com.florentrevest.microanki

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Customizable look for the pop-up flashcard.
 *
 * Each theme controls the card background (solid or vertical gradient),
 * text colour, accent (buttons/links) and corner radius. The settings
 * screen stores just the [id]; [resolveCardTheme] maps it to a concrete
 * theme (with `system_auto` following light/dark mode).
 */
data class CardTheme(
    val id: String,
    val name: String,
    val blurb: String,
    val isDark: Boolean,
    val surface: Color,
    val surfaceEnd: Color? = null,
    val onSurface: Color,
    val accent: Color,
    val corner: Dp = 28.dp,
) {
    fun brush(): Brush =
        if (surfaceEnd != null) Brush.verticalGradient(listOf(surface, surfaceEnd))
        else SolidColor(surface)

    fun colorScheme(): ColorScheme =
        if (isDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = Color.White,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceEnd ?: surface,
                onSurfaceVariant = onSurface,
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = Color.White,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = surfaceEnd ?: surface,
                onSurfaceVariant = onSurface,
            )
        }

    /** CSS colours for the WebView fallback (rich cards). */
    fun cssFg(): String = "#%02X%02X%02X".format(
        (onSurface.red * 255).toInt(),
        (onSurface.green * 255).toInt(),
        (onSurface.blue * 255).toInt(),
    )

    fun cssAccent(): String = "#%02X%02X%02X".format(
        (accent.red * 255).toInt(),
        (accent.green * 255).toInt(),
        (accent.blue * 255).toInt(),
    )
}

const val SYSTEM_AUTO_THEME_ID = "system_auto"

val ALL_CARD_THEMES: List<CardTheme> = listOf(
    CardTheme(
        id = SYSTEM_AUTO_THEME_ID,
        name = "System auto",
        blurb = "Follows light / dark mode (Minimal)",
        isDark = false,
        surface = Color(0xFFFFFFFF),
        surfaceEnd = null,
        onSurface = Color(0xFF1A1A1A),
        accent = Color(0xFF2962FF),
        corner = 16.dp,
    ),
    CardTheme(
        id = "minimal_light",
        name = "Minimal Light",
        blurb = "Clean white, sharp blue",
        isDark = false,
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1A1A),
        accent = Color(0xFF2962FF),
        corner = 16.dp,
    ),
    CardTheme(
        id = "minimal_dark",
        name = "Minimal Dark",
        blurb = "Near-black, calm blue",
        isDark = true,
        surface = Color(0xFF121212),
        onSurface = Color(0xFFECEFF1),
        accent = Color(0xFF90CAF9),
        corner = 16.dp,
    ),
    CardTheme(
        id = "pastel_rose",
        name = "Pastel Rose",
        blurb = "Soft pink, cherry accent",
        isDark = false,
        surface = Color(0xFFFDECEF),
        surfaceEnd = Color(0xFFF8D3DA),
        onSurface = Color(0xFF5A2A32),
        accent = Color(0xFFD6336C),
        corner = 28.dp,
    ),
    CardTheme(
        id = "pastel_mint",
        name = "Pastel Mint",
        blurb = "Fresh green, gentle contrast",
        isDark = false,
        surface = Color(0xFFE6F7ED),
        surfaceEnd = Color(0xFFC7EDD4),
        onSurface = Color(0xFF1F3D2B),
        accent = Color(0xFF0CA678),
        corner = 28.dp,
    ),
    CardTheme(
        id = "pastel_lavender",
        name = "Pastel Lavender",
        blurb = "Calm violet haze",
        isDark = false,
        surface = Color(0xFFEDE9FE),
        surfaceEnd = Color(0xFFDAD0FB),
        onSurface = Color(0xFF372A63),
        accent = Color(0xFF7048E8),
        corner = 28.dp,
    ),
    CardTheme(
        id = "pastel_peach",
        name = "Pastel Peach",
        blurb = "Warm apricot glow",
        isDark = false,
        surface = Color(0xFFFFF1E6),
        surfaceEnd = Color(0xFFFFD9BE),
        onSurface = Color(0xFF5C3A1E),
        accent = Color(0xFFE8590C),
        corner = 28.dp,
    ),
    CardTheme(
        id = "gradient_sunset",
        name = "Sunset Gradient",
        blurb = "Peach into coral",
        isDark = false,
        surface = Color(0xFFFFB199),
        surfaceEnd = Color(0xFFFF6A88),
        onSurface = Color(0xFF432027),
        accent = Color(0xFFC2255C),
        corner = 28.dp,
    ),
    CardTheme(
        id = "gradient_ocean",
        name = "Ocean Gradient",
        blurb = "Sky blue into seafoam",
        isDark = false,
        surface = Color(0xFFA1C4FD),
        surfaceEnd = Color(0xFFC2E9FB),
        onSurface = Color(0xFF12395B),
        accent = Color(0xFF1864AB),
        corner = 28.dp,
    ),
    CardTheme(
        id = "gradient_midnight",
        name = "Midnight Gradient",
        blurb = "Deep navy into teal (dark)",
        isDark = true,
        surface = Color(0xFF0F2027),
        surfaceEnd = Color(0xFF2C5364),
        onSurface = Color(0xFFE8F0FE),
        accent = Color(0xFF82AAFF),
        corner = 28.dp,
    ),
    CardTheme(
        id = "paper",
        name = "Warm Paper",
        blurb = "Sepia reader, book-like",
        isDark = false,
        surface = Color(0xFFFAF3E0),
        surfaceEnd = Color(0xFFEADFC2),
        onSurface = Color(0xFF3E3A32),
        accent = Color(0xFF8C6A2B),
        corner = 12.dp,
    ),
    CardTheme(
        id = "forest",
        name = "Sage Forest",
        blurb = "Muted green, earthy calm",
        isDark = false,
        surface = Color(0xFFE8F0E4),
        surfaceEnd = Color(0xFFCFE0C8),
        onSurface = Color(0xFF1B4332),
        accent = Color(0xFF2D6A4F),
        corner = 20.dp,
    ),
)

fun findCardTheme(id: String): CardTheme =
    ALL_CARD_THEMES.firstOrNull { it.id == id } ?: ALL_CARD_THEMES[1]

/**
 * Resolve the stored id to a concrete theme. `system_auto` picks Minimal
 * Light/Dark based on [systemDark].
 */
fun resolveCardTheme(storedId: String, systemDark: Boolean): CardTheme =
    if (storedId == SYSTEM_AUTO_THEME_ID) {
        ALL_CARD_THEMES.first { it.id == if (systemDark) "minimal_dark" else "minimal_light" }
    } else {
        findCardTheme(storedId)
    }
