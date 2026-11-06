package com.example.engine

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed class DownloadState {
  object NotDownloaded : DownloadState()
  data class Downloading(
    val progressPercent: Int,
    val downloadedMb: Float,
    val totalMb: Float,
    val speedKbps: Float = 0f
  ) : DownloadState()
  data class Downloaded(val localPath: String, val sizeMb: Float, val verified: Boolean = true) : DownloadState()
  data class Failed(val error: String, val canRetry: Boolean = true) : DownloadState()
}

data class RealAIModel(
  val id: String,
  val name: String,
  val author: String,
  val license: String,
  val sizeMb: Float,
  val fileName: String,
  val architecture: String,
  val repositoryUrl: String,
  val downloadUrl: String,
  val descriptionAr: String,
  val descriptionEn: String,
  val capabilities: List<String>,
  val isRecommendedQuickDownload: Boolean = false
)

data class LocalModelFile(
  val id: String,
  val name: String,
  val fileName: String,
  val filePath: String,
  val sizeMb: Float,
  val isCustom: Boolean,
  val formatTag: String,
  val lastModified: Long
)

object ModelDownloadManager {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(45, TimeUnit.SECONDS)
    .readTimeout(600, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .retryOnConnectionFailure(true)
    .build()

  private val activeJobs = ConcurrentHashMap<String, Job>()

  val officialModels: List<RealAIModel> = listOf(
    RealAIModel(
      id = "stability_triposr_full",
      name = "TripoSR Official Full (1.65 GB)",
      author = "Stability AI & Tripo (Official)",
      license = "MIT License",
      sizeMb = 1650.0f,
      fileName = "triposr_fp32.ckpt",
      architecture = "LRM Triplane Transformer (FP32 Official HuggingFace)",
      repositoryUrl = "https://huggingface.co/stabilityai/TripoSR",
      downloadUrl = "https://huggingface.co/stabilityai/TripoSR/resolve/main/model.ckpt",
      descriptionAr = "أوزان نموذج TripoSR الرسمي الحقيقي الكامل (1.65 جيجابايت) مباشرة من مستودع Stability AI على Hugging Face.",
      descriptionEn = "Authentic official 1.65 GB full model weights directly from Stability AI repository on Hugging Face.",
      capabilities = listOf("Full 1.65GB FP32", "Memory-Mapped MMap", "Hugging Face", "Stability AI", "360° Geometry", "Marching Cubes")
    ),
    RealAIModel(
      id = "stability_triposr",
      name = "TripoSR Mobile Lite (185 MB)",
      author = "Stability AI & VAST (TripoSR Team)",
      license = "MIT License",
      sizeMb = 185.0f,
      fileName = "triposr_int8.onnx",
      architecture = "LRM Triplane Transformer (ONNX Quantized / Low RAM)",
      repositoryUrl = "https://huggingface.co/stabilityai/TripoSR",
      downloadUrl = "https://huggingface.co/Heliosoph/triposr-onnx/resolve/main/nerf.onnx",
      descriptionAr = "النسخة الخفيفة السريعة للهواتف ذات الذاكرة المحدودة مع سرعة استجابة فورية.",
      descriptionEn = "Lightweight mobile-quantized version for rapid inference on devices with constrained RAM.",
      capabilities = listOf("Mobile Lite 185MB", "Hugging Face", "Low RAM", "Triplane Decoding"),
      isRecommendedQuickDownload = true
    ),
    RealAIModel(
      id = "bytedance_depth_anything",
      name = "Depth Anything V2 Mobile ONNX (27 MB)",
      author = "ByteDance & onnx-community",
      license = "Apache 2.0",
      sizeMb = 27.2f,
      fileName = "depth_anything_v2_int8.onnx",
      architecture = "Vision Transformer (ViT-Small) Quantized ONNX INT8",
      repositoryUrl = "https://huggingface.co/onnx-community/depth-anything-v2-small",
      downloadUrl = "https://huggingface.co/onnx-community/depth-anything-v2-small/resolve/main/onnx/model_int8.onnx",
      descriptionAr = "النموذج الأسرع للتحميل (27 ميجابايت) بتقنية ONNX INT8 لتنفيذ فوري فائق السرعة على المعالج والـ GPU.",
      descriptionEn = "Ultra-fast neural depth download (27 MB) in ONNX INT8 for instant neural depth inference.",
      capabilities = listOf("Fast 27 MB", "ONNX INT8", "Hugging Face", "High Precision", "Real ONNX")
    ),
    RealAIModel(
      id = "intel_midas_small",
      name = "Intel MiDaS v2.1 Mobile ONNX (66.7 MB)",
      author = "Intel Intelligent Systems Lab",
      license = "MIT License",
      sizeMb = 66.7f,
      fileName = "midas_model_small.onnx",
      architecture = "EfficientNet-Lite + Dense Decoder (ONNX Mobile)",
      repositoryUrl = "https://github.com/isl-org/MiDaS",
      downloadUrl = "https://github.com/isl-org/MiDaS/releases/download/v2_1/model-small.onnx",
      descriptionAr = "نموذج إنتل الرسمي المحول لصيغة ONNX من مستودع GitHub الرسمي لتقدير التضاريس ثلاثية الأبعاد.",
      descriptionEn = "Official Intel MiDaS model converted to ONNX from official GitHub releases for on-device 3D relief.",
      capabilities = listOf("Intel Official", "GitHub Releases", "ONNX Mobile", "Relief Mesh")
    )
  )

  private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
  val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

  private val _localModels = MutableStateFlow<List<LocalModelFile>>(emptyList())
  val localModels: StateFlow<List<LocalModelFile>> = _localModels.asStateFlow()

  fun getModelsDir(context: Context): File {
    val ext = context.getExternalFilesDir("models")
    val dir = ext ?: File(context.filesDir, "models")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun getModelsDirPath(context: Context): String {
    return getModelsDir(context).absolutePath
  }

  fun initModels(context: Context) {
    val modelsDir = getModelsDir(context)
    val initialMap = mutableMapOf<String, DownloadState>()

    for (m in officialModels) {
      val file = File(modelsDir, m.fileName)
      if (file.exists() && file.length() > 1000) {
        val sizeMb = file.length() / (1024f * 1024f)
        initialMap[m.id] = DownloadState.Downloaded(file.absolutePath, sizeMb, verified = true)
      } else {
        initialMap[m.id] = DownloadState.NotDownloaded
      }
    }
    _downloadStates.value = initialMap

    // Scan all model files in directory
    val foundList = mutableListOf<LocalModelFile>()
    val files = modelsDir.listFiles() ?: emptyArray()
    for (f in files) {
      if (!f.isFile || f.name.endsWith(".tmp")) continue
      val ext = f.extension.lowercase()
      if (ext in listOf("onnx", "ckpt", "pt", "bin", "tflite")) {
        val official = officialModels.find { it.fileName.equals(f.name, ignoreCase = true) }
        val sizeMb = f.length() / (1024f * 1024f)
        val id = official?.id ?: "custom_${f.name}"
        val name = official?.name ?: f.name
        foundList.add(
          LocalModelFile(
            id = id,
            name = name,
            fileName = f.name,
            filePath = f.absolutePath,
            sizeMb = sizeMb,
            isCustom = official == null,
            formatTag = ext.uppercase(),
            lastModified = f.lastModified()
          )
        )
      }
    }
    _localModels.value = foundList.sortedByDescending { it.lastModified }
  }

  fun isModelDownloaded(context: Context, modelId: String): Boolean {
    val model = officialModels.find { it.id == modelId }
    if (model != null) {
      val file = File(getModelsDir(context), model.fileName)
      return file.exists() && file.length() > 1000
    }
    return _localModels.value.any { it.id == modelId && File(it.filePath).exists() }
  }

  fun getModelFile(context: Context, modelId: String): File? {
    val model = officialModels.find { it.id == modelId }
    if (model != null) {
      val file = File(getModelsDir(context), model.fileName)
      if (file.exists() && file.length() > 1000) return file
    }
    val local = _localModels.value.find { it.id == modelId }
    if (local != null) {
      val file = File(local.filePath)
      if (file.exists() && file.length() > 1000) return file
    }
    return null
  }

  fun findModelFile(context: Context, modelIdOrFileName: String): File? {
    val modelsDir = getModelsDir(context)
    val official = officialModels.find { it.id == modelIdOrFileName }
    if (official != null) {
      val f = File(modelsDir, official.fileName)
      if (f.exists() && f.length() > 1000) return f
    }

    val local = _localModels.value.find { it.id == modelIdOrFileName || it.fileName.equals(modelIdOrFileName, ignoreCase = true) }
    if (local != null) {
      val f = File(local.filePath)
      if (f.exists() && f.length() > 1000) return f
    }

    val direct = File(modelsDir, modelIdOrFileName)
    if (direct.exists() && direct.length() > 1000) return direct

    for (loc in _localModels.value) {
      val f = File(loc.filePath)
      if (f.exists() && f.length() > 1000) return f
    }
    return null
  }

  fun getActiveAnyModelFile(context: Context): File? {
    for (m in officialModels) {
      val f = getModelFile(context, m.id)
      if (f != null) return f
    }
    for (loc in _localModels.value) {
      val f = File(loc.filePath)
      if (f.exists() && f.length() > 1000) return f
    }
    return null
  }

  suspend fun importModelFile(context: Context, uri: android.net.Uri): LocalModelFile? = withContext(Dispatchers.IO) {
    try {
      val modelsDir = getModelsDir(context)
      var extractedName = "imported_model.onnx"

      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
          val n = cursor.getString(nameIndex)
          if (!n.isNullOrBlank()) extractedName = n
        }
      }

      val destFile = File(modelsDir, extractedName)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destFile).use { output ->
          input.copyTo(output)
        }
      }

      initModels(context)

      val sizeMb = destFile.length() / (1024f * 1024f)
      val formatTag = if (extractedName.contains('.')) extractedName.substringAfterLast('.').uppercase() else "ONNX"
      LocalModelFile(
        id = "custom_${extractedName}",
        name = extractedName,
        fileName = extractedName,
        filePath = destFile.absolutePath,
        sizeMb = sizeMb,
        isCustom = true,
        formatTag = formatTag,
        lastModified = destFile.lastModified()
      )
    } catch (e: Exception) {
      null
    }
  }

  fun deleteLocalModel(context: Context, fileNameOrId: String) {
    val modelsDir = getModelsDir(context)
    val target = _localModels.value.find { it.id == fileNameOrId || it.fileName.equals(fileNameOrId, ignoreCase = true) }
    val fileName = target?.fileName ?: fileNameOrId
    val file = File(modelsDir, fileName)
    if (file.exists()) file.delete()
    val tmp = File(modelsDir, "$fileName.tmp")
    if (tmp.exists()) tmp.delete()
    initModels(context)
  }

  suspend fun downloadModel(context: Context, model: RealAIModel) {
    val modelsDir = getModelsDir(context)
    val targetFile = File(modelsDir, model.fileName)
    val tempFile = File(modelsDir, "${model.fileName}.tmp")

    _downloadStates.value = _downloadStates.value.toMutableMap().apply {
      put(model.id, DownloadState.Downloading(0, 0f, model.sizeMb, 0f))
    }

    withContext(Dispatchers.IO) {
      val currentJob = coroutineContext[Job]
      if (currentJob != null) {
        activeJobs[model.id] = currentJob
      }
      try {
        val request = Request.Builder()
          .url(model.downloadUrl)
          .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
          .header("Accept", "*/*")
          .header("Connection", "keep-alive")
          .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
          _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(model.id, DownloadState.Failed("فشل الاتصال بالخادم (HTTP ${response.code})"))
          }
          return@withContext
        }

        val body = response.body
        if (body == null) {
          _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(model.id, DownloadState.Failed("استجابة الخادم فارغة"))
          }
          return@withContext
        }

        val totalBytes = if (body.contentLength() > 0) body.contentLength() else (model.sizeMb * 1024 * 1024).toLong()
        var downloadedBytes = 0L
        val startTime = System.currentTimeMillis()
        var lastReportTime = startTime
        var lastDownloadedBytes = 0L

        body.byteStream().use { input ->
          FileOutputStream(tempFile).use { output ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var lastReportedPercent = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {
              output.write(buffer, 0, bytesRead)
              downloadedBytes += bytesRead

              val now = System.currentTimeMillis()
              if (now - lastReportTime >= 250 || downloadedBytes == totalBytes) {
                val percent = ((downloadedBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
                val durationSec = (now - lastReportTime) / 1000f
                val speed = if (durationSec > 0) ((downloadedBytes - lastDownloadedBytes) / 1024f) / durationSec else 0f
                lastReportTime = now
                lastDownloadedBytes = downloadedBytes

                if (percent != lastReportedPercent) {
                  lastReportedPercent = percent
                  val downloadedMb = downloadedBytes / (1024f * 1024f)
                  _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                    put(model.id, DownloadState.Downloading(percent, downloadedMb, model.sizeMb, speed))
                  }
                }
              }
            }
          }
        }

        // Verify downloaded file size
        if (tempFile.length() < 1000) {
          tempFile.delete()
          _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(model.id, DownloadState.Failed("الملف المحمل غير صالح أو صغير جداً"))
          }
          return@withContext
        }

        // Rename temp to target
        if (targetFile.exists()) targetFile.delete()
        tempFile.renameTo(targetFile)

        initModels(context)

        val sizeMb = targetFile.length() / (1024f * 1024f)
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
          put(model.id, DownloadState.Downloaded(targetFile.absolutePath, sizeMb, verified = true))
        }

      } catch (e: CancellationException) {
        tempFile.delete()
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
          put(model.id, DownloadState.NotDownloaded)
        }
      } catch (e: Exception) {
        tempFile.delete()
        val msg = e.localizedMessage ?: "فشل التنزيل بسبب انقطاع الاتصال"
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
          put(model.id, DownloadState.Failed("خطأ في التحميل: $msg"))
        }
      } finally {
        activeJobs.remove(model.id)
      }
    }
  }

  fun cancelDownload(modelId: String) {
    activeJobs[modelId]?.cancel()
    activeJobs.remove(modelId)
    _downloadStates.value = _downloadStates.value.toMutableMap().apply {
      put(modelId, DownloadState.NotDownloaded)
    }
  }

  fun deleteModel(context: Context, modelId: String) {
    deleteLocalModel(context, modelId)
    _downloadStates.value = _downloadStates.value.toMutableMap().apply {
      put(modelId, DownloadState.NotDownloaded)
    }
  }
}
