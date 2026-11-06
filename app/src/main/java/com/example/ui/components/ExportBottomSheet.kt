package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ExportFormat
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
  language: AppLanguage,
  onDismiss: () -> Unit,
  onExport: (ExportFormat) -> Unit,
  onSaveToLibrary: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedFormat by remember { mutableStateOf(ExportFormat.OBJ) }

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
            text = LocaleStrings.exportTitle(language),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary
          )
          Text(
            text = "Select industry-standard format",
            fontSize = 12.sp,
            color = StudioTextSecondary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Format Choice Cards
      FormatCard(
        title = "OBJ (.obj + .mtl + .png)",
        desc = LocaleStrings.objDesc(language),
        icon = Icons.Default.FolderZip,
        selected = selectedFormat == ExportFormat.OBJ,
        onClick = { selectedFormat = ExportFormat.OBJ }
      )

      Spacer(modifier = Modifier.height(10.dp))

      FormatCard(
        title = "STL (3D Printable Solid)",
        desc = LocaleStrings.stlDesc(language),
        icon = Icons.Default.Print,
        selected = selectedFormat == ExportFormat.STL,
        onClick = { selectedFormat = ExportFormat.STL }
      )

      Spacer(modifier = Modifier.height(10.dp))

      FormatCard(
        title = "glTF 2.0 (.gltf)",
        desc = LocaleStrings.gltfDesc(language),
        icon = Icons.Default.ViewInAr,
        selected = selectedFormat == ExportFormat.GLTF,
        onClick = { selectedFormat = ExportFormat.GLTF }
      )

      Spacer(modifier = Modifier.height(10.dp))

      FormatCard(
        title = "PLY Point Cloud (.ply)",
        desc = LocaleStrings.plyDesc(language),
        icon = Icons.Default.Description,
        selected = selectedFormat == ExportFormat.PLY,
        onClick = { selectedFormat = ExportFormat.PLY }
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = {
            onSaveToLibrary()
            onDismiss()
          },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyanPrimary),
          border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyanPrimary),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("save_library_button")
        ) {
          Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(LocaleStrings.saveToLibrary(language), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = { onExport(selectedFormat) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = StudioEmerald,
            contentColor = Color(0xFF022013)
          ),
          modifier = Modifier
            .weight(1.3f)
            .height(48.dp)
            .testTag("share_file_button")
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(LocaleStrings.shareButton(language), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun FormatCard(
  title: String,
  desc: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit
) {
  val bg = if (selected) StudioCyanPrimary.copy(alpha = 0.12f) else StudioSurfaceVariant
  val border = if (selected) StudioCyanPrimary else StudioCardBorder

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .border(1.dp, border, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (selected) StudioCyanPrimary else StudioCardBorder),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) Color(0xFF03141A) else StudioTextSecondary,
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = StudioTextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = desc,
        color = StudioTextSecondary,
        fontSize = 11.sp,
        lineHeight = 15.sp
      )
    }

    if (selected) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(StudioCyanPrimary),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Selected",
          tint = Color(0xFF03141A),
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
