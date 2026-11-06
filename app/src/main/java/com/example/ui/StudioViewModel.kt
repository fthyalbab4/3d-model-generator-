package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ModelEntity
import com.example.data.ModelRepository
import com.example.engine.Gemini3DEngine
import com.example.engine.Generative3DEngine
import com.example.engine.GaussianSplatCloud
import com.example.engine.GaussianSplatNative
import com.example.engine.Mesh3D
import com.example.engine.MobileGaussianGenerator
import com.example.engine.ModelDownloadManager
import com.example.engine.NeuralDepthEstimator
import com.example.engine.RealAIModel
import com.example.engine.SampleModels
import com.example.engine.TripoSREngine
import com.example.engine.TripoSROnnxEngine
import com.example.engine.exporters.GltfExporter
import com.example.engine.exporters.ObjExporter
import com.example.engine.exporters.PlyExporter
import com.example.engine.exporters.StlExporter
import com.example.ui.components.ShadingMode
import com.example.ui.localization.AppLanguage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ExportFormat {
  OBJ,
  STL,
  GLTF,
  PLY
}

enum class CameraPreset {
  RESET,
  FRONT,
  BACK,
  TOP,
  ISO
}

enum class GenerationMode {
  IMAGE_TO_3D,
  TEXT_TO_3D
}

enum class HybridEngineMode {
  HYBRID_AUTO,
  LOCAL_SCULPTOR,
  CLOUD_TRIPO_AI
}

data class StudioUiState(
  val generationMode: GenerationMode = GenerationMode.TEXT_TO_3D,
  val engineMode: HybridEngineMode = HybridEngineMode.HYBRID_AUTO,
  val textPrompt: String = "محارب أسطوري بدرع فولاذي وخوذة وسيف",
  val activeBitmap: Bitmap? = null,
  val activeMesh: Mesh3D? = null,
  val activeSplatCloud: GaussianSplatCloud? = null,
  val isProcessing: Boolean = false,
  val processingStage: Int = 1,
  val lastInferenceMs: Long = 0,
  val language: AppLanguage = AppLanguage.ARABIC,
  val selectedModelId: String = "stability_triposr_full",
  val depthScale: Float = 1.0f,
  val resolution: Int = 36,
  val smoothingFactor: Float = 0.5f,
  val shadingMode: ShadingMode = ShadingMode.TEXTURED,
  val pitch: Float = 15f,
  val yaw: Float = -30f,
  val zoom: Float = 1.0f,
  val panX: Float = 0f,
  val panY: Float = 0f,
  val lightAzimuth: Float = 45f,
  val lightElevation: Float = 55f,
  val showExportSheet: Boolean = false,
  val showSamplesSheet: Boolean = false,
  val showLibrarySheet: Boolean = false,
  val showModelHubSheet: Boolean = false,
  val isOnnxModelActive: Boolean = false,
  val onnxDeviceStatus: String = "Offline Character Sculptor (Watertight Manifold)",
  val activeModelFileName: String = "Local Contour Extruder",
  val availableRamMb: Long = 0L,
  val toastMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ModelRepository
  val savedModels: StateFlow<List<ModelEntity>>

  private val _uiState = MutableStateFlow(StudioUiState())
  val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

  init {
    val database = AppDatabase.getDatabase(application)
    repository = ModelRepository(database.modelDao())
    savedModels = repository.allModels.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    // Initialize character models and synthesize initial character mesh
    viewModelScope.launch(Dispatchers.IO) {
      ModelDownloadManager.initModels(application)
      if (_uiState.value.activeBitmap == null) {
        val warriorBmp = SampleModels.samples.first().generator()
        withContext(Dispatchers.Main) {
          _uiState.value = _uiState.value.copy(activeBitmap = warriorBmp)
        }
      }
      generateFromText(_uiState.value.textPrompt)
    }
  }

  fun toggleLanguage() {
    _uiState.value = _uiState.value.copy(
      language = if (_uiState.value.language == AppLanguage.ARABIC) AppLanguage.ENGLISH else AppLanguage.ARABIC
    )
  }

  fun setGenerationMode(mode: GenerationMode) {
    _uiState.value = _uiState.value.copy(generationMode = mode)
  }

  fun setTextPrompt(prompt: String) {
    _uiState.value = _uiState.value.copy(textPrompt = prompt)
  }

  fun setSelectedModelId(id: String) {
    val modelFile = ModelDownloadManager.findModelFile(getApplication(), id)
    val fileName = modelFile?.name ?: "triposr_nerf.onnx"
    _uiState.value = _uiState.value.copy(
      selectedModelId = id,
      activeModelFileName = fileName,
      showModelHubSheet = false,
      toastMessage = if (_uiState.value.language == AppLanguage.ARABIC) "تم تفعيل النموذج: $fileName" else "Model activated: $fileName"
    )
    reconstructCurrent()
  }

  fun downloadModel(model: RealAIModel) {
    viewModelScope.launch(Dispatchers.IO) {
      ModelDownloadManager.downloadModel(getApplication(), model)
    }
  }

  fun cancelDownload(modelId: String) {
    ModelDownloadManager.cancelDownload(modelId)
  }

  fun deleteModelWeights(modelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
      ModelDownloadManager.deleteModel(getApplication(), modelId)
      _uiState.value = _uiState.value.copy(
        toastMessage = if (_uiState.value.language == AppLanguage.ARABIC) "تم حذف ملف النموذج" else "Model file deleted"
      )
    }
  }

  fun importCustomModel(uri: android.net.Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      _uiState.value = _uiState.value.copy(
        toastMessage = if (_uiState.value.language == AppLanguage.ARABIC) "جاري استيراد وتثبيت ملف النموذج..." else "Importing model file..."
      )
      val imported = ModelDownloadManager.importModelFile(getApplication(), uri)
      if (imported != null) {
        val sizeStr = String.format(Locale.US, "%.1f", imported.sizeMb)
        _uiState.value = _uiState.value.copy(
          selectedModelId = imported.id,
          activeModelFileName = imported.fileName,
          isOnnxModelActive = true,
          onnxDeviceStatus = "Active: ${imported.fileName} (${sizeStr}MB)",
          toastMessage = if (_uiState.value.language == AppLanguage.ARABIC)
            "تم استيراد وتفعيل النموذج: ${imported.fileName} (${sizeStr} ميجابايت)"
          else
            "Model imported & activated: ${imported.fileName} (${sizeStr}MB)"
        )
        reconstructCurrent()
      } else {
        _uiState.value = _uiState.value.copy(
          toastMessage = if (_uiState.value.language == AppLanguage.ARABIC) "فشل استيراد ملف النموذج" else "Failed to import model file"
        )
      }
    }
  }

  fun setImage(bitmap: Bitmap) {
    _uiState.value = _uiState.value.copy(activeBitmap = bitmap, generationMode = GenerationMode.IMAGE_TO_3D)
    generateFromImage(bitmap)
  }

  fun loadSample(sample: SampleModels.SampleItem) {
    viewModelScope.launch(Dispatchers.Default) {
      val bmp = sample.generator()
      withContext(Dispatchers.Main) {
        _uiState.value = _uiState.value.copy(
          activeBitmap = bmp,
          generationMode = GenerationMode.IMAGE_TO_3D,
          showSamplesSheet = false
        )
        generateFromImage(bmp)
      }
    }
  }

  fun setEngineMode(mode: HybridEngineMode) {
    val msg = when (mode) {
      HybridEngineMode.HYBRID_AUTO -> if (_uiState.value.language == AppLanguage.ARABIC) "تم تفعيل الوضع الهجين الذكي" else "Hybrid Auto Mode enabled"
      HybridEngineMode.LOCAL_SCULPTOR -> if (_uiState.value.language == AppLanguage.ARABIC) "تم تفعيل النحات المحلي بدون نت" else "Offline Local Sculptor enabled"
      HybridEngineMode.CLOUD_TRIPO_AI -> if (_uiState.value.language == AppLanguage.ARABIC) "تم تفعيل سحابة الذكاء الاصطناعي" else "Cloud AI 3D Studio enabled"
    }
    _uiState.value = _uiState.value.copy(engineMode = mode, toastMessage = msg)
    reconstructCurrent()
  }

  fun generateFromText(prompt: String = _uiState.value.textPrompt) {
    viewModelScope.launch(Dispatchers.Default) {
      _uiState.value = _uiState.value.copy(isProcessing = true, processingStage = 1, textPrompt = prompt)
      val startTime = System.currentTimeMillis()

      val shouldTryCloud = (_uiState.value.engineMode == HybridEngineMode.CLOUD_TRIPO_AI ||
        (_uiState.value.engineMode == HybridEngineMode.HYBRID_AUTO && Gemini3DEngine.hasApiKey()))

      val cloudMesh = if (shouldTryCloud) {
        _uiState.value = _uiState.value.copy(processingStage = 2)
        Gemini3DEngine.generateFromPrompt(prompt)
      } else null

      val isCloudUsed = cloudMesh != null
      val finalMesh = cloudMesh ?: run {
        _uiState.value = _uiState.value.copy(processingStage = 3)
        val res = _uiState.value.resolution
        Generative3DEngine.generateFromText(prompt, resolution = res).mesh
      }

      val elapsed = System.currentTimeMillis() - startTime
      val splatCloud = MobileGaussianGenerator.generateFromMesh(finalMesh, prompt)
      val mem = TripoSROnnxEngine.getSystemMemory(getApplication())
      _uiState.value = _uiState.value.copy(
        activeMesh = finalMesh,
        activeSplatCloud = splatCloud,
        isProcessing = false,
        processingStage = 4,
        lastInferenceMs = elapsed,
        availableRamMb = mem.availableMb,
        isOnnxModelActive = !isCloudUsed,
        onnxDeviceStatus = "Idol-Mobile 3DGS (${splatCloud.count} Gaussians)",
        activeModelFileName = if (isCloudUsed) "Cloud 3DGS" else "Mobile 3DGS (.ply)"
      )
    }
  }

  fun generateFromImage(bitmap: Bitmap) {
    viewModelScope.launch(Dispatchers.Default) {
      _uiState.value = _uiState.value.copy(isProcessing = true, processingStage = 1)
      val startTime = System.currentTimeMillis()

      _uiState.value = _uiState.value.copy(processingStage = 2)
      val density = when (_uiState.value.resolution) {
        in 0..28 -> 96
        in 29..40 -> 128
        else -> 160
      }
      val splatResult = MobileGaussianGenerator.generateFromImage(bitmap, densityLevel = density)

      _uiState.value = _uiState.value.copy(processingStage = 3)
      val elapsed = System.currentTimeMillis() - startTime
      val mem = TripoSROnnxEngine.getSystemMemory(getApplication())
      _uiState.value = _uiState.value.copy(
        activeSplatCloud = splatResult.cloud,
        isProcessing = false,
        processingStage = 4,
        lastInferenceMs = elapsed,
        availableRamMb = mem.availableMb,
        isOnnxModelActive = true,
        onnxDeviceStatus = "Idol-Mobile 3DGS (${splatResult.cloud.count} Gaussians)",
        activeModelFileName = "Mobile 3DGS (.ply)"
      )
    }
  }

  fun exportPlyAndShare(context: Context) {
    val cloud = _uiState.value.activeSplatCloud ?: return
    viewModelScope.launch(Dispatchers.IO) {
      val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
      val outFile = File(outDir, "idol_3dgs_${System.currentTimeMillis()}.ply")
      val ok = GaussianSplatNative.nativeExportPly(
        filePath = outFile.absolutePath,
        positions = cloud.positions,
        scales = cloud.scales,
        rotations = cloud.rotations,
        colors = cloud.colors,
        opacities = cloud.opacities,
        count = cloud.count
      )
      withContext(Dispatchers.Main) {
        if (ok && outFile.exists()) {
          val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(Intent.createChooser(shareIntent, "Export 3D Gaussian Splat (.ply)"))
          _uiState.value = _uiState.value.copy(
            showExportSheet = false,
            toastMessage = if (_uiState.value.language == AppLanguage.ARABIC)
              "تم تصدير ملف 3D Gaussian Splat (.ply) بنجاح"
            else
              "Exported 3D Gaussian Splat (.ply) successfully"
          )
        } else {
          _uiState.value = _uiState.value.copy(
            toastMessage = if (_uiState.value.language == AppLanguage.ARABIC) "فشل تصدير ملف .ply" else "Failed to export .ply"
          )
        }
      }
    }
  }

  fun reconstructCurrent() {
    if (_uiState.value.generationMode == GenerationMode.TEXT_TO_3D) {
      generateFromText(_uiState.value.textPrompt)
    } else {
      _uiState.value.activeBitmap?.let { generateFromImage(it) }
    }
  }

  fun setShadingMode(mode: ShadingMode) {
    _uiState.value = _uiState.value.copy(shadingMode = mode)
  }

  fun setResolution(res: Int) {
    _uiState.value = _uiState.value.copy(resolution = res)
    reconstructCurrent()
  }

  fun setDepthScale(scale: Float) {
    _uiState.value = _uiState.value.copy(depthScale = scale)
    reconstructCurrent()
  }

  fun setSmoothingFactor(factor: Float) {
    _uiState.value = _uiState.value.copy(smoothingFactor = factor)
    reconstructCurrent()
  }

  fun setLightAngle(azimuth: Float, elevation: Float) {
    _uiState.value = _uiState.value.copy(lightAzimuth = azimuth, lightElevation = elevation)
  }

  fun rotateCamera(deltaPitch: Float, deltaYaw: Float) {
    val newPitch = (_uiState.value.pitch + deltaPitch).coerceIn(-89f, 89f)
    val newYaw = (_uiState.value.yaw + deltaYaw) % 360f
    _uiState.value = _uiState.value.copy(pitch = newPitch, yaw = newYaw)
  }

  fun zoomCamera(factor: Float) {
    val newZoom = (_uiState.value.zoom * factor).coerceIn(0.4f, 4.0f)
    _uiState.value = _uiState.value.copy(zoom = newZoom)
  }

  fun panCamera(dx: Float, dy: Float) {
    _uiState.value = _uiState.value.copy(
      panX = _uiState.value.panX + dx,
      panY = _uiState.value.panY + dy
    )
  }

  fun setCameraPreset(preset: CameraPreset) {
    when (preset) {
      CameraPreset.RESET -> _uiState.value = _uiState.value.copy(
        pitch = 20f, yaw = -35f, zoom = 1.0f, panX = 0f, panY = 0f
      )
      CameraPreset.FRONT -> _uiState.value = _uiState.value.copy(
        pitch = 0f, yaw = 0f, zoom = 1.0f, panX = 0f, panY = 0f
      )
      CameraPreset.BACK -> _uiState.value = _uiState.value.copy(
        pitch = 0f, yaw = 180f, zoom = 1.0f, panX = 0f, panY = 0f
      )
      CameraPreset.TOP -> _uiState.value = _uiState.value.copy(
        pitch = 85f, yaw = 0f, zoom = 1.0f, panX = 0f, panY = 0f
      )
      CameraPreset.ISO -> _uiState.value = _uiState.value.copy(
        pitch = 30f, yaw = 45f, zoom = 1.0f, panX = 0f, panY = 0f
      )
    }
  }

  fun toggleExportSheet(show: Boolean) {
    _uiState.value = _uiState.value.copy(showExportSheet = show)
  }

  fun toggleSamplesSheet(show: Boolean) {
    _uiState.value = _uiState.value.copy(showSamplesSheet = show)
  }

  fun toggleLibrarySheet(show: Boolean) {
    _uiState.value = _uiState.value.copy(showLibrarySheet = show)
  }

  fun toggleModelHubSheet(show: Boolean) {
    _uiState.value = _uiState.value.copy(showModelHubSheet = show)
  }

  fun clearToast() {
    _uiState.value = _uiState.value.copy(toastMessage = null)
  }

  fun saveCurrentModel(customTitle: String? = null) {
    val mesh = _uiState.value.activeMesh ?: return

    viewModelScope.launch(Dispatchers.IO) {
      val title = customTitle ?: if (_uiState.value.generationMode == GenerationMode.TEXT_TO_3D) {
        _uiState.value.textPrompt.take(24)
      } else {
        "Image3D_${System.currentTimeMillis() % 10000}"
      }

      val entity = ModelEntity(
        title = title,
        vertexCount = mesh.vertexCount,
        triangleCount = mesh.triangleCount,
        depthScale = _uiState.value.depthScale,
        shadingMode = _uiState.value.shadingMode.name,
        thumbnailBase64 = ""
      )
      repository.insert(entity)
      withContext(Dispatchers.Main) {
        _uiState.value = _uiState.value.copy(toastMessage = "Saved to Library")
      }
    }
  }

  fun deleteModel(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      repository.delete(id)
    }
  }

  fun exportModel(context: Context, format: ExportFormat) {
    val mesh = _uiState.value.activeMesh ?: return
    viewModelScope.launch(Dispatchers.IO) {
      val baseName = "model360_${System.currentTimeMillis() % 10000}"
      val filesToShare = mutableListOf<File>()

      when (format) {
        ExportFormat.OBJ -> {
          val res = ObjExporter.export(context, mesh, baseName)
          filesToShare.add(res.objFile)
          filesToShare.add(res.mtlFile)
          res.textureFile?.let { filesToShare.add(it) }
        }
        ExportFormat.STL -> {
          val file = StlExporter.export(context, mesh, baseName)
          filesToShare.add(file)
        }
        ExportFormat.GLTF -> {
          val file = GltfExporter.export(context, mesh, baseName)
          filesToShare.add(file)
        }
        ExportFormat.PLY -> {
          val file = PlyExporter.export(context, mesh, baseName)
          filesToShare.add(file)
        }
      }

      withContext(Dispatchers.Main) {
        shareFiles(context, filesToShare)
        _uiState.value = _uiState.value.copy(showExportSheet = false)
      }
    }
  }

  private fun shareFiles(context: Context, files: List<File>) {
    if (files.isEmpty()) return
    val authority = "${context.packageName}.fileprovider"

    if (files.size == 1) {
      val uri = FileProvider.getUriForFile(context, authority, files[0])
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "3D Model Export: ${files[0].name}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Share 3D Model"))
    } else {
      val uris = ArrayList(files.map { FileProvider.getUriForFile(context, authority, it) })
      val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        putExtra(Intent.EXTRA_SUBJECT, "3D Model Export Bundle")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Share 3D Model Files"))
    }
  }
}
