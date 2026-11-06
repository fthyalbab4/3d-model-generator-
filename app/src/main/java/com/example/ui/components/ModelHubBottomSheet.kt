package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DownloadState
import com.example.engine.LocalModelFile
import com.example.engine.ModelDownloadManager
import com.example.engine.RealAIModel
import com.example.engine.TripoSROnnxEngine
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyanPrimary
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelHubBottomSheet(
  language: AppLanguage,
  selectedModelId: String,
  onSelectModel: (String) -> Unit,
  onDownloadModel: (RealAIModel) -> Unit,
  onCancelDownload: (String) -> Unit = {},
  onDeleteModel: (String) -> Unit,
  onImportCustomModel: (Uri) -> Unit = {},
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val downloadStates by ModelDownloadManager.downloadStates.collectAsState()
  val localModels by ModelDownloadManager.localModels.collectAsState()
  val memoryStatus = remember { TripoSROnnxEngine.getSystemMemory(context) }
  val clipboardManager = LocalClipboardManager.current
  val modelsDirPath = remember { ModelDownloadManager.getModelsDirPath(context) }
  var copiedNotice by remember { mutableStateOf(false) }

  // Custom ONNX/CKPT model file picker using OpenDocument contract
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { onImportCustomModel(it) }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = StudioSurface,
    contentColor = StudioTextPrimary
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = LocaleStrings.modelHub(language),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = StudioTextPrimary
          )
          Text(
            text = if (language == AppLanguage.ARABIC)
              "إدارة وتنزيل واستيراد نماذج الذكاء الاصطناعي الحقيقية (On-Device)"
            else
              "Manage, Download & Import Authentic AI Models (On-Device)",
            fontSize = 12.sp,
            color = StudioTextSecondary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 1. Persistent Models Directory Path Card
      Surface(
        color = StudioSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyanPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Folder, contentDescription = null, tint = StudioCyanPrimary, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (language == AppLanguage.ARABIC) "مجلد النماذج المحدد على ذاكرة الهاتف:" else "Designated Models Storage Directory:",
                color = StudioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            TextButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(modelsDirPath))
                copiedNotice = true
              },
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
              modifier = Modifier.height(26.dp)
            ) {
              Text(
                text = if (copiedNotice)
                  (if (language == AppLanguage.ARABIC) "تم النسخ ✓" else "Copied ✓")
                else
                  (if (language == AppLanguage.ARABIC) "نسخ المسار" else "Copy Path"),
                fontSize = 11.sp,
                color = if (copiedNotice) StudioEmerald else StudioCyanPrimary,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = modelsDirPath,
            color = StudioTextPrimary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
          )

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (language == AppLanguage.ARABIC)
              "يتم حفظ جميع النماذج المحملة والمستوردة في هذا المجلد. يمكنك أيضاً نسخ أي ملف .onnx أو .ckpt إلى هذا المسار مباشرة وسيتعرف عليه التطبيق فوراً."
            else
              "All downloaded and imported models are saved in this folder. You can also place any .onnx or .ckpt file into this path directly.",
            color = StudioTextMuted,
            fontSize = 9.5.sp,
            lineHeight = 13.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 2. Import Button
      Button(
        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = StudioCyanPrimary.copy(alpha = 0.15f),
          contentColor = StudioCyanPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyanPrimary),
        modifier = Modifier
          .fillMaxWidth()
          .height(40.dp)
      ) {
        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (language == AppLanguage.ARABIC)
            "استيراد وتفعيل ملف نموذج (.onnx أو .ckpt) من الهاتف"
          else
            "Import & Activate Model File (.onnx or .ckpt) from Device",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // RAM & Hardware Guard Banner
      Surface(
        color = StudioBackground,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Memory, contentDescription = null, tint = StudioCyanPrimary, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${if (language == AppLanguage.ARABIC) "الذاكرة المتاحة:" else "Available RAM:"} ${String.format(Locale.US, "%.1f", memoryStatus.availableMb / 1024f)} GB / ${String.format(Locale.US, "%.1f", memoryStatus.totalMb / 1024f)} GB • Memory-Mapped MMap",
            color = StudioTextSecondary,
            fontSize = 10.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Scrollable List of Models
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Section A: Locally Discovered & Imported Models
        if (localModels.isNotEmpty()) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${if (language == AppLanguage.ARABIC) "النماذج الجاهزة والمستوردة محلياً" else "Ready & Imported Models on Storage"} (${localModels.size})",
                color = StudioEmerald,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          items(localModels) { local ->
            val isSelected = local.id == selectedModelId || local.fileName == selectedModelId
            val bg = if (isSelected) StudioCyanPrimary.copy(alpha = 0.14f) else StudioSurfaceVariant
            val border = if (isSelected) StudioCyanPrimary else StudioEmerald.copy(alpha = 0.5f)

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(12.dp))
                .clickable { onSelectModel(local.id) }
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = local.name,
                      color = StudioTextPrimary,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = StudioEmerald.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = local.formatTag,
                        color = StudioEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = "${if (local.isCustom) (if (language == AppLanguage.ARABIC) "ملف مستورد محلياً" else "Imported File") else (if (language == AppLanguage.ARABIC) "نموذج رسمي مثبت" else "Official Model")} • ${String.format(Locale.US, "%.1f", local.sizeMb)} MB",
                    color = StudioTextSecondary,
                    fontSize = 10.sp
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  if (isSelected) {
                    Surface(
                      color = StudioCyanPrimary,
                      shape = RoundedCornerShape(10.dp)
                    ) {
                      Text(
                        text = if (language == AppLanguage.ARABIC) "النموذج النشط ✓" else "Active ✓",
                        color = Color(0xFF03141A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      )
                    }
                  } else {
                    OutlinedButton(
                      onClick = { onSelectModel(local.id) },
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyanPrimary),
                      border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyanPrimary),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                      modifier = Modifier.height(30.dp)
                    ) {
                      Text(if (language == AppLanguage.ARABIC) "تفعيل" else "Activate", fontSize = 11.sp)
                    }
                  }

                  Spacer(modifier = Modifier.width(6.dp))

                  IconButton(
                    onClick = { onDeleteModel(local.fileName) },
                    modifier = Modifier.size(30.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete model",
                      tint = StudioRose,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }
        }

        // Section B: Official Direct HuggingFace Downloads
        item {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = StudioCyanPrimary, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (language == AppLanguage.ARABIC) "النماذج الرسمية المتاحة للتحميل المباشر" else "Official Models Available for Direct Download",
              color = StudioCyanPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        items(ModelDownloadManager.officialModels) { model ->
          val isSelected = model.id == selectedModelId || model.fileName == selectedModelId
          val state = downloadStates[model.id] ?: DownloadState.NotDownloaded
          val isDownloaded = state is DownloadState.Downloaded
          val isDownloading = state is DownloadState.Downloading
          val isFailed = state is DownloadState.Failed

          val bg = if (isSelected) StudioCyanPrimary.copy(alpha = 0.12f) else StudioSurfaceVariant
          val border = if (isSelected) StudioCyanPrimary else if (isFailed) StudioRose else StudioCardBorder
          val desc = if (language == AppLanguage.ARABIC) model.descriptionAr else model.descriptionEn

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(bg)
              .border(1.dp, border, RoundedCornerShape(14.dp))
              .clickable(enabled = isDownloaded) { onSelectModel(model.id) }
              .padding(14.dp)
          ) {
            // Header: Name & Badges
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = model.name,
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  if (model.isRecommendedQuickDownload) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      color = StudioEmerald.copy(alpha = 0.2f),
                      shape = RoundedCornerShape(4.dp)
                    ) {
                      Text(
                        text = if (language == AppLanguage.ARABIC) "موصى به" else "Recommended",
                        color = StudioEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Text(
                  text = "By ${model.author} • ${model.architecture}",
                  color = StudioCyanPrimary,
                  fontSize = 10.sp
                )
              }

              val formattedSize = if (model.sizeMb >= 1000f) {
                "${String.format(Locale.US, "%.2f", model.sizeMb / 1024f)} GB"
              } else if (model.sizeMb < 1f) {
                "${(model.sizeMb * 1024).toInt()} KB"
              } else {
                "${String.format(Locale.US, "%.1f", model.sizeMb)} MB"
              }

              Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
              ) {
                Text(
                  text = formattedSize,
                  color = StudioTextPrimary,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = desc,
              color = StudioTextSecondary,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Capabilities tags
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              model.capabilities.forEach { cap ->
                Surface(
                  color = StudioBackground,
                  shape = RoundedCornerShape(4.dp)
                ) {
                  Text(
                    text = cap,
                    color = StudioTextMuted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Download Status Actions
            when (state) {
              is DownloadState.Downloading -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (language == AppLanguage.ARABIC) "جاري التحميل إلى مجلد النماذج..." else "Downloading to models directory...",
                      color = StudioCyanPrimary,
                      fontSize = 11.sp
                    )

                    val dlText = if (state.totalMb >= 1000f) {
                      "${String.format(Locale.US, "%.2f", state.downloadedMb / 1024f)} GB / ${String.format(Locale.US, "%.2f", state.totalMb / 1024f)} GB"
                    } else {
                      "${String.format(Locale.US, "%.1f", state.downloadedMb)} MB / ${String.format(Locale.US, "%.1f", state.totalMb)} MB"
                    }

                    Text(
                      text = "${state.progressPercent}% ($dlText)",
                      color = StudioTextPrimary,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  LinearProgressIndicator(
                    progress = { state.progressPercent / 100f },
                    color = StudioCyanPrimary,
                    trackColor = StudioCardBorder,
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp))
                  )

                  Spacer(modifier = Modifier.height(6.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    val speedText = if (state.speedKbps >= 1024f) {
                      "${String.format(Locale.US, "%.2f", state.speedKbps / 1024f)} MB/s"
                    } else {
                      "${state.speedKbps.toInt()} KB/s"
                    }
                    Text(
                      text = "${if (language == AppLanguage.ARABIC) "السرعة:" else "Speed:"} $speedText",
                      color = StudioTextMuted,
                      fontSize = 10.sp
                    )

                    OutlinedButton(
                      onClick = { onCancelDownload(model.id) },
                      shape = RoundedCornerShape(6.dp),
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRose),
                      border = androidx.compose.foundation.BorderStroke(1.dp, StudioRose),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp)
                    ) {
                      Text(if (language == AppLanguage.ARABIC) "إلغاء التحميل" else "Cancel", fontSize = 10.sp)
                    }
                  }
                }
              }

              is DownloadState.Downloaded -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Installed",
                      tint = StudioEmerald,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = "${if (language == AppLanguage.ARABIC) "مثبت في مجلد النماذج" else "Installed"} (${String.format(Locale.US, "%.1f", state.sizeMb)} MB)",
                      color = StudioEmerald,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                      Surface(
                        color = StudioCyanPrimary,
                        shape = RoundedCornerShape(10.dp)
                      ) {
                        Text(
                          text = if (language == AppLanguage.ARABIC) "النموذج النشط ✓" else "Active ✓",
                          color = Color(0xFF03141A),
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                    } else {
                      OutlinedButton(
                        onClick = { onSelectModel(model.id) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioCyanPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyanPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                      ) {
                        Text(if (language == AppLanguage.ARABIC) "تفعيل" else "Select", fontSize = 11.sp)
                      }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                      onClick = { onDeleteModel(model.id) },
                      modifier = Modifier.size(30.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete model",
                        tint = StudioRose,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }

              is DownloadState.Failed -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Surface(
                    color = StudioRose.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioRose.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StudioRose, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = state.error,
                        color = StudioRose,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (language == AppLanguage.ARABIC) "يمكنك إعادة المحاولة" else "You can retry",
                      color = StudioTextMuted,
                      fontSize = 10.sp
                    )

                    Button(
                      onClick = { onDownloadModel(model) },
                      shape = RoundedCornerShape(6.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = StudioCyanPrimary, contentColor = Color(0xFF03141A)),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp)
                    ) {
                      Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(if (language == AppLanguage.ARABIC) "إعادة المحاولة" else "Retry", fontSize = 10.sp)
                    }
                  }
                }
              }

              is DownloadState.NotDownloaded -> {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = if (language == AppLanguage.ARABIC) "متاح للتنزيل المباشر" else "Direct Download Available",
                      color = StudioTextSecondary,
                      fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                      onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(model.repositoryUrl))
                        context.startActivity(browserIntent)
                      },
                      modifier = Modifier.size(24.dp)
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "HuggingFace Repo",
                        tint = StudioCyanPrimary,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                  }

                  Button(
                    onClick = { onDownloadModel(model) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (model.isRecommendedQuickDownload) StudioEmerald else StudioCyanPrimary,
                      contentColor = Color(0xFF03141A)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (language == AppLanguage.ARABIC) "تنزيل الآن" else "Download",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
