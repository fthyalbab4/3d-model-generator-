package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GenerationMode
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyanPrimary
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun StudioTopBar(
  language: AppLanguage,
  generationMode: GenerationMode,
  onSelectGenerationMode: (GenerationMode) -> Unit,
  onToggleLanguage: () -> Unit,
  onOpenModelHub: () -> Unit,
  onOpenSamples: () -> Unit,
  onOpenLibrary: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = StudioSurface.copy(alpha = 0.95f),
    tonalElevation = 6.dp,
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // App Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(StudioCyanPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.ViewInAr,
              contentDescription = "3D Studio Logo",
              tint = StudioCyanPrimary,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = LocaleStrings.appTitle(language),
              color = StudioTextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(StudioEmerald)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = LocaleStrings.privacyBadge(language),
                color = StudioEmerald,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        // Action Buttons: Model Hub, Samples, Library, Language Toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onOpenModelHub,
            modifier = Modifier.testTag("open_model_hub_button")
          ) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = LocaleStrings.modelHub(language),
              tint = StudioCyanPrimary
            )
          }

          IconButton(
            onClick = onOpenSamples,
            modifier = Modifier.testTag("open_samples_button")
          ) {
            Icon(
              imageVector = Icons.Default.Collections,
              contentDescription = LocaleStrings.demoPresets(language),
              tint = StudioTextSecondary
            )
          }

          IconButton(
            onClick = onOpenLibrary,
            modifier = Modifier.testTag("open_library_button")
          ) {
            Icon(
              imageVector = Icons.Default.FolderSpecial,
              contentDescription = LocaleStrings.myModels(language),
              tint = StudioTextSecondary
            )
          }

          // Language Switch Pill
          Surface(
            onClick = onToggleLanguage,
            shape = RoundedCornerShape(20.dp),
            color = StudioBackground,
            modifier = Modifier.testTag("toggle_language_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = StudioCyanPrimary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (language == AppLanguage.ARABIC) "English" else "العربية",
                color = StudioTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Mode Switcher Tabs: Text to 3D & Image to 3D
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(StudioBackground)
          .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val textSelected = generationMode == GenerationMode.TEXT_TO_3D
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (textSelected) StudioCyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onSelectGenerationMode(GenerationMode.TEXT_TO_3D) }
            .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = null,
              tint = if (textSelected) StudioCyanPrimary else StudioTextSecondary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = LocaleStrings.tabTextTo3D(language),
              color = if (textSelected) StudioCyanPrimary else StudioTextSecondary,
              fontSize = 12.sp,
              fontWeight = if (textSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }

        val imgSelected = generationMode == GenerationMode.IMAGE_TO_3D
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (imgSelected) StudioCyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onSelectGenerationMode(GenerationMode.IMAGE_TO_3D) }
            .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Image,
              contentDescription = null,
              tint = if (imgSelected) StudioCyanPrimary else StudioTextSecondary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = LocaleStrings.tabImageTo3D(language),
              color = if (imgSelected) StudioCyanPrimary else StudioTextSecondary,
              fontSize = 12.sp,
              fontWeight = if (imgSelected) FontWeight.Bold else FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
