package uz.nodirbek.flashcardsapp.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import uz.nodirbek.flashcardsapp.composeapp.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

actual val OutfitFamily: FontFamily = FontFamily(
    Font(GoogleFont("Outfit"), provider, FontWeight.Normal),
    Font(GoogleFont("Outfit"), provider, FontWeight.Medium),
    Font(GoogleFont("Outfit"), provider, FontWeight.SemiBold),
    Font(GoogleFont("Outfit"), provider, FontWeight.Bold),
    Font(GoogleFont("Outfit"), provider, FontWeight.ExtraBold),
    Font(GoogleFont("Outfit"), provider, FontWeight.Black)
)

actual val InterFamily: FontFamily = FontFamily(
    Font(GoogleFont("Inter"), provider, FontWeight.Normal),
    Font(GoogleFont("Inter"), provider, FontWeight.Medium),
    Font(GoogleFont("Inter"), provider, FontWeight.SemiBold)
)
