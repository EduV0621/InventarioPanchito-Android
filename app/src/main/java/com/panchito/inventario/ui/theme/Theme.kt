package com.panchito.inventario.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PanchitoGreen,
    onPrimary = PanchitoOnDark,
    secondary = PanchitoAmber,
    error = PanchitoRed,
    background = PanchitoGray
)

private val DarkColors = darkColorScheme(
    primary = PanchitoGreen,
    onPrimary = PanchitoOnDark,
    secondary = PanchitoAmber,
    error = PanchitoRed
)

@Composable
fun InventarioPanchitoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = PanchitoTypography,
        content = content
    )
}
