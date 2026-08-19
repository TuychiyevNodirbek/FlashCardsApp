package uz.nodirbek.flashcardsapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Dynamic Color Functions (React to Theme) ──────────────────────────────

@Composable
fun dynamicBackground(): Color =
    if (isSystemInDarkTheme()) FdDarkBackground else FdBackground

@Composable
fun dynamicSurface(): Color =
    if (isSystemInDarkTheme()) FdDarkSurface else FdSurface

@Composable
fun dynamicSurface2(): Color =
    if (isSystemInDarkTheme()) FdDarkSurface2 else FdSurface2

@Composable
fun dynamicText(): Color =
    if (isSystemInDarkTheme()) FdDarkText else FdText

@Composable
fun dynamicTextSub(): Color =
    if (isSystemInDarkTheme()) FdDarkTextSub else FdTextSub

@Composable
fun dynamicBorder(): Color =
    if (isSystemInDarkTheme()) FdDarkBorder else FdBorder

@Composable
fun dynamicPrimary(): Color =
    if (isSystemInDarkTheme()) FdDarkPrimary else FdPrimary

@Composable
fun dynamicPrimaryLight(): Color =
    if (isSystemInDarkTheme()) Color(0xFF2F3D7A) else FdPrimaryLight

@Composable
fun dynamicGreen(): Color =
    if (isSystemInDarkTheme()) Color(0xFF4FD897) else FdGreen

@Composable
fun dynamicGreenLight(): Color =
    if (isSystemInDarkTheme()) Color(0xFF1A3D2A) else FdGreenLight

@Composable
fun dynamicRed(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFF8F8F) else FdRed

@Composable
fun dynamicRedLight(): Color =
    if (isSystemInDarkTheme()) Color(0xFF3D1A1A) else FdRedLight

@Composable
fun dynamicOrange(): Color =
    if (isSystemInDarkTheme()) Color(0xFFFFB347) else FdOrange

@Composable
fun dynamicOrangeLight(): Color =
    if (isSystemInDarkTheme()) Color(0xFF3D2A1A) else FdOrangeLight

// ── AppBar Specific Colors ────────────────────────────────────────────────

@Composable
fun appBarBackground(): Color = dynamicSurface()

@Composable
fun appBarTint(): Color = dynamicText()

@Composable
fun appBarBorder(): Color = dynamicBorder()

// ── Card Specific ─────────────────────────────────────────────────────────

@Composable
fun cardBackground(): Color = dynamicSurface()

@Composable
fun cardBorder(): Color = dynamicBorder()

@Composable
fun cardText(): Color = dynamicText()

// ── Button & Interactive ──────────────────────────────────────────────────

@Composable
fun buttonBackground(): Color = dynamicPrimary()

@Composable
fun buttonTint(): Color = dynamicPrimary()

@Composable
fun badgeBackground(): Color = dynamicSurface2()

// ── Convenience: Get all colors at once ───────────────────────────────────

data class DynamicColorPalette(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val textSub: Color,
    val border: Color,
    val primary: Color,
    val primaryLight: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
)

@Composable
fun getDynamicPalette(): DynamicColorPalette = DynamicColorPalette(
    background = dynamicBackground(),
    surface = dynamicSurface(),
    surface2 = dynamicSurface2(),
    text = dynamicText(),
    textSub = dynamicTextSub(),
    border = dynamicBorder(),
    primary = dynamicPrimary(),
    primaryLight = dynamicPrimaryLight(),
    green = dynamicGreen(),
    red = dynamicRed(),
    orange = dynamicOrange(),
)
