package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.LibraryBottomSheet
import com.example.ui.components.ModelHubBottomSheet
import com.example.ui.components.SamplesBottomSheet
import com.example.ui.components.StudioControlsSheet
import com.example.ui.components.GaussianViewport
import com.example.ui.components.StudioTopBar
import com.example.ui.components.Viewport3D
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocaleStrings
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyanPrimary
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextPrimary

@Composable
fun StudioScreen(viewModel: StudioViewModel) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val savedModels by viewModel.savedModels.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Gallery Picker Launcher
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let {
      try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val source = ImageDecoder.createSource(context.contentResolver, it)
          ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
          }
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, it)
        }
        viewModel.setImage(bitmap)
      } catch (e: Exception) {
        Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Camera Launcher with Safe Execution
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    bitmap?.let { viewModel.setImage(it) }
  }

  fun launchCameraSafely() {
    try {
      cameraLauncher.launch(null)
    } catch (e: Exception) {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(
          if (uiState.language == AppLanguage.ARABIC)
            "تعذر تشغيل تطبيق الكاميرا، يمكنك اختيار صورة من المعرض"
          else
            "Unable to launch camera app, please select an image from gallery"
        )
      }
    }
  }

  // Camera Permission Handler
  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      launchCameraSafely()
    } else {
      coroutineScope.launch {
        snackbarHostState.showSnackbar(
          if (uiState.language == AppLanguage.ARABIC)
            "إذن الكاميرا مطلوب لالتقاط صورة مباشرة، أو يمكنك الاختيار من المعرض"
          else
            "Camera permission is required to capture photos, or pick from gallery"
        )
      }
    }
  }

  // Handle Toast Messages
  LaunchedEffect(uiState.toastMessage) {
    uiState.toastMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearToast()
    }
  }

  val layoutDir = if (uiState.language == AppLanguage.ARABIC) LayoutDirection.Rtl else LayoutDirection.Ltr

  CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
    Scaffold(
      topBar = {
        StudioTopBar(
          language = uiState.language,
          generationMode = uiState.generationMode,
          onSelectGenerationMode = { viewModel.setGenerationMode(it) },
          onToggleLanguage = { viewModel.toggleLanguage() },
          onOpenModelHub = { viewModel.toggleModelHubSheet(true) },
          onOpenSamples = { viewModel.toggleSamplesSheet(true) },
          onOpenLibrary = { viewModel.toggleLibrarySheet(true) }
        )
      },
      bottomBar = {
        StudioControlsSheet(
          language = uiState.language,
          generationMode = uiState.generationMode,
          engineMode = uiState.engineMode,
          textPrompt = uiState.textPrompt,
          shadingMode = uiState.shadingMode,
          resolution = uiState.resolution,
          smoothing = uiState.smoothingFactor,
          lightAzimuth = uiState.lightAzimuth,
          isProcessing = uiState.isProcessing,
          onSelectEngineMode = { viewModel.setEngineMode(it) },
          onChangeTextPrompt = { viewModel.setTextPrompt(it) },
          onGenerateText = { viewModel.generateFromText() },
          onSelectShading = { viewModel.setShadingMode(it) },
          onSetCameraPreset = { viewModel.setCameraPreset(it) },
          onChangeResolution = { viewModel.setResolution(it) },
          onChangeSmoothing = { viewModel.setSmoothingFactor(it) },
          onChangeLightAzimuth = { viewModel.setLightAngle(it, uiState.lightElevation) },
          onLaunchCamera = {
            val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
              launchCameraSafely()
            } else {
              cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
          },
          onLaunchGallery = {
            galleryLauncher.launch(
              androidx.activity.result.PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
              )
            )
          },
          onReconstructImage = { viewModel.reconstructCurrent() },
          onOpenExport = { viewModel.toggleExportSheet(true) }
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
      containerColor = StudioBackground
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // 3D Viewport Canvas (Real-time 3D Gaussian Splatting .ply renderer when active, fallback to Viewport3D)
        val splatCloud = uiState.activeSplatCloud
        if (splatCloud != null) {
          GaussianViewport(
            cloud = splatCloud,
            pitch = uiState.pitch,
            yaw = uiState.yaw,
            zoom = uiState.zoom,
            panX = uiState.panX,
            panY = uiState.panY,
            onRotate = { dp, dy -> viewModel.rotateCamera(dp, dy) },
            onZoom = { factor -> viewModel.zoomCamera(factor) },
            onPan = { dx, dy -> viewModel.panCamera(dx, dy) },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Viewport3D(
            mesh = uiState.activeMesh,
            shadingMode = uiState.shadingMode,
            pitch = uiState.pitch,
            yaw = uiState.yaw,
            zoom = uiState.zoom,
            panX = uiState.panX,
            panY = uiState.panY,
            lightAzimuth = uiState.lightAzimuth,
            lightElevation = uiState.lightElevation,
            onRotate = { dp, dy -> viewModel.rotateCamera(dp, dy) },
            onZoom = { factor -> viewModel.zoomCamera(factor) },
            onPan = { dx, dy -> viewModel.panCamera(dx, dy) },
            modifier = Modifier.fillMaxSize()
          )
        }

        // Overlay: Model Telemetry HUD (3D Gaussians / Vertices, Faces, Inference time)
        if (splatCloud != null || uiState.activeMesh != null) {
          Surface(
            color = StudioSurface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(14.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
              if (splatCloud != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = StudioCyanPrimary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (uiState.language == AppLanguage.ARABIC)
                      "${splatCloud.count} نقطة غاوسية (.ply)"
                    else
                      "${splatCloud.count} 3D Gaussians (.ply)",
                    color = StudioTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                  )
                }
              } else if (uiState.activeMesh != null) {
                val mesh = uiState.activeMesh!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = StudioCyanPrimary,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${LocaleStrings.verticesCount(uiState.language, mesh.vertexCount)}  •  ${LocaleStrings.trianglesCount(uiState.language, mesh.triangleCount)}",
                    color = StudioTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
              if (uiState.lastInferenceMs > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = StudioEmerald,
                    modifier = Modifier.size(13.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = LocaleStrings.inferenceTime(uiState.language, uiState.lastInferenceMs),
                    color = StudioEmerald,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }

              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = uiState.onnxDeviceStatus,
                color = if (uiState.engineMode == HybridEngineMode.LOCAL_SCULPTOR) StudioEmerald else StudioCyanPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Processing Overlay HUD
        AnimatedVisibility(
          visible = uiState.isProcessing,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier.align(Alignment.Center)
        ) {
          Surface(
            color = StudioSurface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCyanPrimary),
            modifier = Modifier.padding(24.dp)
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(24.dp)
            ) {
              CircularProgressIndicator(
                color = StudioCyanPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = LocaleStrings.processingStages(uiState.language, uiState.processingStage),
                color = StudioTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = LocaleStrings.privacyBadge(uiState.language),
                color = StudioEmerald,
                fontSize = 11.sp
              )
            }
          }
        }
      }

      // Export Bottom Sheet
      if (uiState.showExportSheet) {
        ExportBottomSheet(
          language = uiState.language,
          onDismiss = { viewModel.toggleExportSheet(false) },
          onExport = { format -> viewModel.exportModel(context, format) },
          onSaveToLibrary = { viewModel.saveCurrentModel() }
        )
      }

      // Samples Bottom Sheet
      if (uiState.showSamplesSheet) {
        SamplesBottomSheet(
          language = uiState.language,
          onDismiss = { viewModel.toggleSamplesSheet(false) },
          onSelectSample = { sample -> viewModel.loadSample(sample) }
        )
      }

      // Library Bottom Sheet
      if (uiState.showLibrarySheet) {
        LibraryBottomSheet(
          language = uiState.language,
          models = savedModels,
          onDismiss = { viewModel.toggleLibrarySheet(false) },
          onDelete = { id -> viewModel.deleteModel(id) }
        )
      }

      // Model Hub Bottom Sheet
      if (uiState.showModelHubSheet) {
        ModelHubBottomSheet(
          language = uiState.language,
          selectedModelId = uiState.selectedModelId,
          onSelectModel = { viewModel.setSelectedModelId(it) },
          onDownloadModel = { viewModel.downloadModel(it) },
          onCancelDownload = { viewModel.cancelDownload(it) },
          onDeleteModel = { viewModel.deleteModelWeights(it) },
          onImportCustomModel = { viewModel.importCustomModel(it) },
          onDismiss = { viewModel.toggleModelHubSheet(false) }
        )
      }
    }
  }
}
