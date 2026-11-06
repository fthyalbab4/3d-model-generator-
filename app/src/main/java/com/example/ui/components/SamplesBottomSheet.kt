package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SampleModels
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyanPrimary
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplesBottomSheet(
  language: AppLanguage,
  onDismiss: () -> Unit,
  onSelectSample: (SampleModels.SampleItem) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = StudioSurface,
    contentColor = StudioTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = LocaleStrings.demoPresets(language),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary
          )
          Text(
            text = "Instant 1-tap testing models",
            fontSize = 12.sp,
            color = StudioTextSecondary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(SampleModels.samples) { sample ->
          val sampleBitmap = remember(sample.id) { sample.generator() }
          val title = if (language == AppLanguage.ARABIC) sample.titleAr else sample.titleEn
          val desc = if (language == AppLanguage.ARABIC) sample.descriptionAr else sample.descriptionEn

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(StudioSurfaceVariant)
              .border(1.dp, StudioCardBorder, RoundedCornerShape(14.dp))
              .clickable { onSelectSample(sample) }
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Image(
              bitmap = sampleBitmap.asImageBitmap(),
              contentDescription = title,
              modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, StudioCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                color = StudioTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = desc,
                color = StudioTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
