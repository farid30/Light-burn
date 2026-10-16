package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LaserDarkColorScheme = darkColorScheme(
  primary = LaserAmber,
  onPrimary = Color.Black,
  primaryContainer = SurfaceElevatedDark,
  onPrimaryContainer = LaserAmberGlow,
  secondary = LayerC01Score,
  onSecondary = Color.Black,
  secondaryContainer = SurfaceVariantDark,
  onSecondaryContainer = Color.White,
  tertiary = LaserRed,
  onTertiary = Color.White,
  background = CarbonDark,
  onBackground = TextPrimary,
  surface = SurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondary,
  outline = BorderDark,
  outlineVariant = BedGridMinor
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = LaserDarkColorScheme,
    typography = Typography,
    content = content
  )
}
