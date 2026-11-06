package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import com.example.data.ModelEntity
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyanPrimary
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBottomSheet(
  language: AppLanguage,
  models: List<ModelEntity>,
  onDismiss: () -> Unit,
  onDelete: (Long) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

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
            text = LocaleStrings.myModels(language),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary
          )
          Text(
            text = "${models.size} models stored locally",
            fontSize = 12.sp,
            color = StudioTextSecondary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (models.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = null,
              tint = StudioTextMuted,
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = LocaleStrings.noModelsYet(language),
              color = StudioTextSecondary,
              fontSize = 13.sp
            )
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(models, key = { it.id }) { model ->
            val thumbBitmap = remember(model.thumbnailBase64) {
              if (model.thumbnailBase64.isNotEmpty()) {
                try {
                  val bytes = Base64.decode(model.thumbnailBase64, Base64.DEFAULT)
                  BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                  null
                }
              } else null
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioSurfaceVariant)
                .border(1.dp, StudioCardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (thumbBitmap != null) {
                Image(
                  bitmap = thumbBitmap.asImageBitmap(),
                  contentDescription = null,
                  modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, StudioCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCardBorder),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.FolderOpen, contentDescription = null, tint = StudioTextSecondary)
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = model.title,
                  color = StudioTextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${LocaleStrings.verticesCount(language, model.vertexCount)} • ${LocaleStrings.trianglesCount(language, model.triangleCount)}",
                  color = StudioCyanPrimary,
                  fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = dateFormat.format(Date(model.timestamp)),
                  color = StudioTextMuted,
                  fontSize = 10.sp
                )
              }

              IconButton(onClick = { onDelete(model.id) }) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = LocaleStrings.deleteModel(language),
                  tint = StudioRose
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
