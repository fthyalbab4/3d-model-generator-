package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioColorScheme = darkColorScheme(
  primary = StudioCyanPrimary,
  onPrimary = Color(0xFF03141A),
  primaryContainer = Color(0xFF004D5A),
  onPrimaryContainer = Color(0xFFB5F5FF),

  secondary = StudioEmerald,
  onSecondary = Color(0xFF022013),
  secondaryContainer = Color(0xFF064E3B),
  onSecondaryContainer = Color(0xFFA7F3D0),

  tertiary = StudioAmber,
  onTertiary = Color(0xFF281700),
  tertiaryContainer = Color(0xFF78350F),
  onTertiaryContainer = Color(0xFFFDE68A),

  background = StudioBackground,
  onBackground = StudioTextPrimary,
  surface = StudioSurface,
  onSurface = StudioTextPrimary,
  surfaceVariant = StudioSurfaceVariant,
  onSurfaceVariant = StudioTextSecondary,

  outline = StudioCardBorder,
  outlineVariant = Color(0xFF1E293B)
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StudioColorScheme,
    typography = Typography,
    content = content
  )
}
