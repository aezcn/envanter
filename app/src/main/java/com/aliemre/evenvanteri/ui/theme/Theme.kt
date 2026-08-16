package com.aliemre.evenvanteri.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Green40 = Color(0xFF3B6939)
private val Green80 = Color(0xFFA1D39A)
private val Sand40 = Color(0xFF54634D)
private val Sand80 = Color(0xFFBBCBB2)
private val Clay40 = Color(0xFF386568)
private val Clay80 = Color(0xFFA0CFD2)

private val LightColors = lightColorScheme(
    primary = Green40,
    secondary = Sand40,
    tertiary = Clay40,
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    secondary = Sand80,
    tertiary = Clay80,
)

@Composable
fun EvEnvanteriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Android 12+ duvar kağıdından renk türetir; kullanıcının telefonuna uyum sağlar.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
