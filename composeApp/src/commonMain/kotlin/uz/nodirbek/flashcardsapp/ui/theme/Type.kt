package uz.nodirbek.flashcardsapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * На Android грузится через Google Play Services Fonts (downloadable fonts).
 * На iOS пока нет бандла .ttf для честного Res.font.* — временно системный шрифт,
 * см. план Фазы 6 (реальный бандлинг шрифтов вместе со сборкой под iOS на Mac).
 */
expect val OutfitFamily: FontFamily
expect val InterFamily: FontFamily

val Typography = Typography(
    displayLarge  = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Black,     fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
    titleMedium   = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 16.sp),
    titleSmall    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 14.sp),
    bodyLarge     = TextStyle(fontFamily = InterFamily,  fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontFamily = InterFamily,  fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = InterFamily,  fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 14.sp),
    labelMedium   = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = OutfitFamily, fontWeight = FontWeight.Bold,      fontSize = 11.sp)
)
