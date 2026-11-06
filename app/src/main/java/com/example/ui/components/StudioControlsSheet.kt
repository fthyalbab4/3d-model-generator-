package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CameraPreset
import com.example.ui.GenerationMode
import com.example.ui.HybridEngineMode
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyanPrimary
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun StudioControlsSheet(
  language: AppLanguage,
  generationMode: GenerationMode,
  engineMode: HybridEngineMode,
  textPrompt: String,
  shadingMode: ShadingMode,
  resolution: Int,
  smoothing: Float,
  lightAzimuth: Float,
  isProcessing: Boolean,
  onSelectEngineMode: (HybridEngineMode) -> Unit,
  onChangeTextPrompt: (String) -> Unit,
  onGenerateText: () -> Unit,
  onSelectShading: (ShadingMode) -> Unit,
  onSetCameraPreset: (CameraPreset) -> Unit,
  onChangeResolution: (Int) -> Unit,
  onChangeSmoothing: (Float) -> Unit,
  onChangeLightAzimuth: (Float) -> Unit,
  onLaunchCamera: () -> Unit,
  onLaunchGallery: () -> Unit,
  onReconstructImage: () -> Unit,
  onOpenExport: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showAdvancedSettings by remember { mutableStateOf(false) }

  val promptSuggestions = remember(language) {
    if (language == AppLanguage.ARABIC) {
      listOf(
        "محارب أسطوري بدرع فولاذي وسيف",
        "روبوت مقاتل سايبربانك بألواح دروع",
        "سيف كاتانا خيالي أسطوري بنصل حاد",
        "درع حربي مقاتل منقوش",
        "تنين مجنح أسطوري ثلاثي الأبعاد",
        "خوذة محارب حديدية منقوشة"
      )
    } else {
      listOf(
        "Legendary knight warrior with steel armor",
        "Cyberpunk combat mech with heavy plating",
        "Epic fantasy katana blade with gold guard",
        "Royal combat shield with steel boss",
        "Mythical winged dragon figurine",
        "Engraved iron warrior helmet"
      )
    }
  }

  Surface(
    color = StudioSurface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {

      // Hybrid Engine Mode Selector Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        EngineModeChip(
          label = if (language == AppLanguage.ARABIC) "⚡ هجين ذكي" else "⚡ Hybrid Auto",
          selected = engineMode == HybridEngineMode.HYBRID_AUTO,
          activeColor = StudioCyanPrimary,
          onClick = { onSelectEngineMode(HybridEngineMode.HYBRID_AUTO) }
        )
        EngineModeChip(
          label = if (language == AppLanguage.ARABIC) "🔒 نحات محلي (بدون نت)" else "🔒 Offline Sculptor",
          selected = engineMode == HybridEngineMode.LOCAL_SCULPTOR,
          activeColor = StudioEmerald,
          onClick = { onSelectEngineMode(HybridEngineMode.LOCAL_SCULPTOR) }
        )
        EngineModeChip(
          label = if (language == AppLanguage.ARABIC) "☁️ سحابي Tripo/Gemini" else "☁️ Cloud AI Studio",
          selected = engineMode == HybridEngineMode.CLOUD_TRIPO_AI,
          activeColor = Color(0xFF818CF8),
          onClick = { onSelectEngineMode(HybridEngineMode.CLOUD_TRIPO_AI) }
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Mode Specific Input Section
      if (generationMode == GenerationMode.TEXT_TO_3D) {
        // Text Prompt Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = textPrompt,
            onValueChange = onChangeTextPrompt,
            placeholder = {
              Text(
                text = LocaleStrings.promptPlaceholder(language),
                fontSize = 12.sp,
                color = StudioTextSecondary
              )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = StudioCyanPrimary,
              unfocusedBorderColor = StudioCardBorder,
              focusedTextColor = StudioTextPrimary,
              unfocusedTextColor = StudioTextPrimary,
              focusedContainerColor = StudioBackground,
              unfocusedContainerColor = StudioBackground
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("prompt_text_field")
          )

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = onGenerateText,
            enabled = !isProcessing && textPrompt.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = StudioCyanPrimary,
              contentColor = Color(0xFF03141A)
            ),
            modifier = Modifier
              .height(52.dp)
              .testTag("generate_text_3d_button")
          ) {
            if (isProcessing) {
              CircularProgressIndicator(color = Color(0xFF03141A), strokeWidth = 2.5.dp, modifier = Modifier.size(18.dp))
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(LocaleStrings.generate3D(language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prompt Inspiration Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          promptSuggestions.forEach { suggestion ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(StudioSurfaceVariant)
                .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp))
                .clickable {
                  onChangeTextPrompt(suggestion)
                  onGenerateText()
                }
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = suggestion,
                color = StudioTextSecondary,
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
      }

      // Shading Mode Selector Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        ShadingChip(
          label = LocaleStrings.shadeTextured(language),
          icon = Icons.Default.Palette,
          selected = shadingMode == ShadingMode.TEXTURED,
          onClick = { onSelectShading(ShadingMode.TEXTURED) }
        )
        ShadingChip(
          label = LocaleStrings.shadeClay(language),
          icon = Icons.Default.FiberManualRecord,
          selected = shadingMode == ShadingMode.CLAY,
          onClick = { onSelectShading(ShadingMode.CLAY) }
        )
        ShadingChip(
          label = LocaleStrings.shadeWireframe(language),
          icon = Icons.Default.GridOn,
          selected = shadingMode == ShadingMode.WIREFRAME,
          onClick = { onSelectShading(ShadingMode.WIREFRAME) }
        )
        ShadingChip(
          label = LocaleStrings.shadeNormals(language),
          icon = Icons.Default.LinearScale,
          selected = shadingMode == ShadingMode.NORMALS,
          onClick = { onSelectShading(ShadingMode.NORMALS) }
        )
        ShadingChip(
          label = LocaleStrings.shadePoints(language),
          icon = Icons.Default.FiberManualRecord,
          selected = shadingMode == ShadingMode.POINT_CLOUD,
          onClick = { onSelectShading(ShadingMode.POINT_CLOUD) }
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Camera 360 Presets & Settings Toggle Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          CameraPresetPill(
            label = LocaleStrings.resetCamera(language),
            onClick = { onSetCameraPreset(CameraPreset.RESET) }
          )
          CameraPresetPill(
            label = LocaleStrings.viewFront(language),
            onClick = { onSetCameraPreset(CameraPreset.FRONT) }
          )
          CameraPresetPill(
            label = LocaleStrings.viewBack(language),
            onClick = { onSetCameraPreset(CameraPreset.BACK) }
          )
          CameraPresetPill(
            label = LocaleStrings.viewTop(language),
            onClick = { onSetCameraPreset(CameraPreset.TOP) }
          )
          CameraPresetPill(
            label = LocaleStrings.viewIso(language),
            onClick = { onSetCameraPreset(CameraPreset.ISO) }
          )
        }

        // Toggle Expand Sliders
        IconButton(
          onClick = { showAdvancedSettings = !showAdvancedSettings },
          modifier = Modifier.testTag("toggle_settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Tune",
            tint = if (showAdvancedSettings) StudioCyanPrimary else StudioTextSecondary
          )
        }
      }

      // Advanced Tuning Sliders
      AnimatedVisibility(visible = showAdvancedSettings) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(StudioSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp)
        ) {
          // Resolution Density
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = LocaleStrings.meshResolution(language),
              color = StudioTextSecondary,
              fontSize = 11.sp
            )
            Text(
              text = "${resolution}³ Voxels",
              color = StudioCyanPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Slider(
            value = resolution.toFloat(),
            onValueChange = { onChangeResolution(it.toInt()) },
            valueRange = 24f..48f,
            steps = 2,
            colors = SliderDefaults.colors(
              thumbColor = StudioCyanPrimary,
              activeTrackColor = StudioCyanPrimary,
              inactiveTrackColor = StudioCardBorder
            ),
            modifier = Modifier.height(26.dp)
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Directional Light Angle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = LocaleStrings.lightAngle(language),
              color = StudioTextSecondary,
              fontSize = 11.sp
            )
            Text(
              text = "${lightAzimuth.toInt()}°",
              color = StudioAmber,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Slider(
            value = lightAzimuth,
            onValueChange = onChangeLightAzimuth,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
              thumbColor = StudioAmber,
              activeTrackColor = StudioAmber,
              inactiveTrackColor = StudioCardBorder
            ),
            modifier = Modifier.height(26.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Bottom Row for Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (generationMode == GenerationMode.IMAGE_TO_3D) {
          OutlinedButton(
            onClick = onLaunchCamera,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioTextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("camera_button")
          ) {
            Icon(Icons.Default.CameraAlt, contentDescription = LocaleStrings.cameraCapture(language), modifier = Modifier.size(18.dp))
          }

          OutlinedButton(
            onClick = onLaunchGallery,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioTextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("gallery_button")
          ) {
            Icon(Icons.Default.Image, contentDescription = LocaleStrings.galleryPick(language), modifier = Modifier.size(18.dp))
          }

          Button(
            onClick = onReconstructImage,
            enabled = !isProcessing,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioCyanPrimary, contentColor = Color(0xFF03141A)),
            modifier = Modifier
              .weight(2f)
              .height(46.dp)
              .testTag("reconstruct_image_button")
          ) {
            if (isProcessing) {
              CircularProgressIndicator(color = Color(0xFF03141A), strokeWidth = 2.5.dp, modifier = Modifier.size(18.dp))
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(LocaleStrings.reconstruct(language), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Export 3D Button
        Button(
          onClick = onOpenExport,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = StudioEmerald,
            contentColor = Color(0xFF022013)
          ),
          modifier = Modifier
            .weight(if (generationMode == GenerationMode.TEXT_TO_3D) 1f else 1.6f)
            .height(46.dp)
            .testTag("export_button")
        ) {
          Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(LocaleStrings.export3d(language), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ShadingChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit
) {
  val bg = if (selected) StudioCyanPrimary.copy(alpha = 0.2f) else StudioSurfaceVariant
  val border = if (selected) StudioCyanPrimary else StudioCardBorder
  val textColor = if (selected) StudioCyanPrimary else StudioTextSecondary

  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(bg)
      .border(1.dp, border, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = textColor,
      modifier = Modifier.size(13.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun CameraPresetPill(
  label: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(StudioSurfaceVariant)
      .border(1.dp, StudioCardBorder, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 5.dp)
  ) {
    Text(
      text = label,
      color = StudioTextPrimary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun EngineModeChip(
  label: String,
  selected: Boolean,
  activeColor: Color,
  onClick: () -> Unit
) {
  val bg = if (selected) activeColor.copy(alpha = 0.2f) else StudioSurfaceVariant
  val border = if (selected) activeColor else StudioCardBorder
  val textColor = if (selected) activeColor else StudioTextSecondary

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(bg)
      .border(1.dp, border, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
    )
  }
}
