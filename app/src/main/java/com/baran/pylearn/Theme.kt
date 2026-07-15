package com.baran.pylearn

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

object Pal {
    val bg = Color(0xFF131620)
    val bgSoft = Color(0xFF191D2A)
    val surface = Color(0xFF1E2230)
    val surfaceHi = Color(0xFF262B3B)
    val track = Color(0xFF2A3040)
    val line = Color(0xFF323949)

    val blue = Color(0xFF4B8BBE)
    val blueEdge = Color(0xFF33607F)
    val green = Color(0xFF58CC02)
    val greenEdge = Color(0xFF46A302)
    val locked = Color(0xFF2A3040)
    val lockedEdge = Color(0xFF1E2431)

    val yellow = Color(0xFFFFD43B)
    val purple = Color(0xFF9D7CFF)
    val orange = Color(0xFFFF9F45)
    val red = Color(0xFFFF4B4B)

    val text = Color(0xFFF5F6FA)
    val textDim = Color(0xFF8A90A0)
    val onBlue = Color(0xFFFFFFFF)
}

private fun t(w: FontWeight) = TextStyle(fontFamily = Nunito, fontWeight = w)

private val PyTypography = Typography(
    displayLarge = t(FontWeight.Black),
    headlineLarge = t(FontWeight.ExtraBold),
    headlineMedium = t(FontWeight.ExtraBold),
    titleLarge = t(FontWeight.Bold),
    titleMedium = t(FontWeight.Bold),
    bodyLarge = t(FontWeight.Normal),
    bodyMedium = t(FontWeight.Normal),
    labelLarge = t(FontWeight.ExtraBold),
    labelMedium = t(FontWeight.Bold),
)

@Composable
fun PyLearnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Pal.blue,
            background = Pal.bg,
            surface = Pal.surface,
            onPrimary = Pal.onBlue,
            onBackground = Pal.text,
            onSurface = Pal.text,
        ),
        typography = PyTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = Nunito, color = Pal.text),
            content = content,
        )
    }
}
