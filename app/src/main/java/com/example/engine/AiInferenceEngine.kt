package com.example.engine

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Standalone Zero-Dependency Android JNI Inference Engine for 3D Generative AI models.
 * Interfaces directly with C/C++ core memory manager (mmap) and hardware accelerator (ARM NEON / Vulkan).
 */
object AiInferenceEngine {
  private const val TAG = "AiInferenceEngine"
  private var isLibraryLoaded = false

  init {
    try {
      System.loadLibrary("ai_inference_engine")
      isLibraryLoaded = true
      Log.i(TAG, "Successfully loaded native ai_inference_engine library.")
    } catch (e: UnsatisfiedLinkError) {
      Log.e(TAG, "Failed to load native ai_inference_engine library", e)
      isLibraryLoaded = false
    }
  }

  /**
   * Loads quantized 3D model weights (.bin or .gguf) into memory using zero-copy mmap().
   */
  external fun nativeLoadModel(modelPath: String): Boolean

  /**
   * Executes 3D object generation given input tensor data and writes output .obj/.glb.
   */
  external fun nativeGenerate3D(inputData: FloatArray, outputObjPath: String): Boolean

  fun loadModel(context: Context, modelFileName: String = "triposr_quantized.gguf"): Boolean {
    if (!isLibraryLoaded) {
      Log.e(TAG, "Native library is not loaded.")
      return false
    }

    val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    val modelFile = File(modelsDir, modelFileName)

    // If model file doesn't exist locally, create a dummy placeholder for skeleton verification
    if (!modelFile.exists()) {
      try {
        modelFile.writeBytes(ByteArray(1024 * 1024)) // 1MB dummy weights for mmap skeleton test
        Log.i(TAG, "Created dummy model weights file at ${modelFile.absolutePath}")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to create dummy model file", e)
      }
    }

    return nativeLoadModel(modelFile.absolutePath)
  }

  fun generate3D(context: Context, inputFeatures: FloatArray, outputFileName: String = "generated_model.obj"): String? {
    if (!isLibraryLoaded) {
      Log.e(TAG, "Native library is not loaded.")
      return null
    }

    val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
    val outputFile = File(outputDir, outputFileName)

    val success = nativeGenerate3D(inputFeatures, outputFile.absolutePath)
    return if (success) outputFile.absolutePath else null
  }
}
