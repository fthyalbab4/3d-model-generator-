package com.example.engine

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real AI 3D Mesh Generator powered by Gemini Multimodal API.
 * Synthesizes genuine 3D polygonal geometry (vertices, normals, faces) in standard Wavefront OBJ format.
 */
object Gemini3DEngine {

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

  fun hasApiKey(): Boolean {
    val key = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Exception) {
      ""
    }
    return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
  }

  suspend fun generateFromPrompt(prompt: String): Mesh3D? = withContext(Dispatchers.IO) {
    if (!hasApiKey()) return@withContext null
    val key = BuildConfig.GEMINI_API_KEY

    val systemPrompt = """
      You are an expert 3D Mesh Geometry Synthesizer.
      Generate a complete 3D polygonal mesh for the requested object: "$prompt".
      Requirements:
      1. Output ONLY a valid Wavefront OBJ file format text.
      2. Include vertices (v x y z) centered around (0,0,0) scaled within [-1.0, 1.0].
      3. Include triangular or quad faces (f v1 v2 v3).
      4. Ensure the mesh forms a complete, solid, closed 3D shape (e.g. 50 to 400 vertices).
      5. Output ONLY the raw OBJ string without any markdown code fence or introductory text.
    """.trimIndent()

    try {
      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
      val json = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().put("text", systemPrompt))
            })
          })
        })
      }

      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful || response.body == null) return@withContext null

      val respStr = response.body!!.string()
      val respJson = JSONObject(respStr)
      val candidates = respJson.optJSONArray("candidates") ?: return@withContext null
      if (candidates.length() == 0) return@withContext null
      val text = candidates.getJSONObject(0)
        .getJSONObject("content")
        .getJSONArray("parts")
        .getJSONObject(0)
        .getString("text")

      val cleanObj = text.replace("```obj", "").replace("```", "").trim()
      ObjParser.parse(cleanObj)
    } catch (e: Exception) {
      null
    }
  }

  suspend fun generateFromImage(bitmap: Bitmap): Mesh3D? = withContext(Dispatchers.IO) {
    if (!hasApiKey()) return@withContext null
    val key = BuildConfig.GEMINI_API_KEY

    val stream = ByteArrayOutputStream()
    val scaled = Bitmap.createScaledBitmap(bitmap, 400, 400, true)
    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
    val base64Img = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

    val prompt = """
      You are a 3D Mesh Reconstruction Engine.
      Analyze the foreground object in this image and reconstruct a complete 3D polygonal mesh for it.
      Requirements:
      1. Output ONLY a valid Wavefront OBJ file text.
      2. Coordinates v x y z normalized in [-1.0, 1.0].
      3. Complete 3D volume with front, sides, and back faces (f v1 v2 v3).
      4. Output pure OBJ format text only, no markdown, no explanation.
    """.trimIndent()

    try {
      val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"
      val json = JSONObject().apply {
        put("contents", JSONArray().apply {
          put(JSONObject().apply {
            put("parts", JSONArray().apply {
              put(JSONObject().put("text", prompt))
              put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                  put("mime_type", "image/jpeg")
                  put("data", base64Img)
                })
              })
            })
          })
        })
      }

      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful || response.body == null) return@withContext null

      val respStr = response.body!!.string()
      val respJson = JSONObject(respStr)
      val candidates = respJson.optJSONArray("candidates") ?: return@withContext null
      if (candidates.length() == 0) return@withContext null
      val text = candidates.getJSONObject(0)
        .getJSONObject("content")
        .getJSONArray("parts")
        .getJSONObject(0)
        .getString("text")

      val cleanObj = text.replace("```obj", "").replace("```", "").trim()
      ObjParser.parse(cleanObj)
    } catch (e: Exception) {
      null
    }
  }
}
