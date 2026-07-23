package uz.nodirbek.flashcardsapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import uz.nodirbek.flashcardsapp.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFamily = FontFamily(
    Font(GoogleFont("Outfit"), provider, FontWeight.Normal),
    Font(GoogleFont("Outfit"), provider, FontWeight.Medium),
    Font(GoogleFont("Outfit"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Outfit"), provider, FontWeight.Bold),
    Font(GoogleFont("Outfit"), provider, FontWeight.ExtraBold),
    Font(GoogleFont("Outfit"), provider, FontWeight.Black)
)

val InterFamily = FontFamily(
    Font(GoogleFont("Inter"), provider, FontWeight.Normal),
    Font(GoogleFont("Inter"), provider, FontWeight.Medium),
    Font(GoogleFont("Inter"), provider, FontWeight.SemiBold)
)

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
