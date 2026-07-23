package uz.nodirbek.flashcardsapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── FlashDeck v2 palette (exact from design) ────────────────────────
// Light mode
val FdBackground   = Color(0xFFF0F0F8)
val FdSurface      = Color(0xFFFFFFFF)
val FdSurface2     = Color(0xFFF4F4FB)
val FdText         = Color(0xFF14142B)
val FdTextSub      = Color(0xFF6B6B80)
val FdBorder       = Color(0xFFE0E0EA)

val FdPrimary      = Color(0xFF4255FF)
val FdPrimaryDark  = Color(0xFF2A3FCC)
val FdPrimaryLight = Color(0xFFEEF0FF)

val FdGreen        = Color(0xFF23B26D)
val FdGreenDark    = Color(0xFF1A8A52)
val FdGreenLight   = Color(0xFFE8F8F0)

val FdRed          = Color(0xFFFF4B4B)
val FdRedDark      = Color(0xFFCC2A2A)
val FdRedLight     = Color(0xFFFFF0F0)

val FdOrange       = Color(0xFFFF9600)
val FdOrangeDark   = Color(0xFFCC7200)
val FdOrangeLight  = Color(0xFFFFF4E5)

val FdPurple       = Color(0xFF8F6CF6)
val FdPurpleDark   = Color(0xFF6A4FCC)

// Dark mode (updated for contrast 4.5:1+)
val FdDarkBackground  = Color(0xFF0F0F14)
val FdDarkSurface     = Color(0xFF1A1815)  // Updated from #1A1A22
val FdDarkSurface2    = Color(0xFF222219)  // Updated to match new surface
val FdDarkText        = Color(0xFFEAE6DF)  // Updated from #EEEEEF8 (spec: #EAE6DF)
val FdDarkTextSub     = Color(0xFF9A9893)  // Updated for better contrast
val FdDarkBorder      = Color(0xFF302D25)  // Updated from #2A2A36 (spec: #302D25)
val FdDarkPrimary     = Color(0xFF7B8AFF)  // Updated for better visibility

// Legacy aliases kept for old code that references them
val Accent500 = FdPrimary
val Neutral900 = FdText
val Neutral200 = FdBorder
val Neutral500 = FdTextSub
val Neutral100 = FdSurface2
val ErrorColor = FdRed
val SuccessColor = FdGreen
