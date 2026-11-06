package com.example.engine

import android.graphics.Color
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Parses standard Wavefront OBJ format into Mesh3D.
 */
object ObjParser {

  fun parse(objText: String): Mesh3D? {
    val tempPositions = ArrayList<Float>()
    val tempNormals = ArrayList<Float>()
    val tempUvs = ArrayList<Float>()

    val outVertices = ArrayList<Float>()
    val outNormals = ArrayList<Float>()
    val outUvs = ArrayList<Float>()
    val outIndices = ArrayList<Int>()

    val reader = BufferedReader(StringReader(objText))
    var line: String?

    while (reader.readLine().also { line = it } != null) {
      val trimmed = line!!.trim()
      if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

      val tokens = trimmed.split("\\s+".toRegex())
      when (tokens[0]) {
        "v" -> {
          if (tokens.size >= 4) {
            tempPositions.add(tokens[1].toFloatOrNull() ?: 0f)
            tempPositions.add(tokens[2].toFloatOrNull() ?: 0f)
            tempPositions.add(tokens[3].toFloatOrNull() ?: 0f)
          }
        }
        "vn" -> {
          if (tokens.size >= 4) {
            tempNormals.add(tokens[1].toFloatOrNull() ?: 0f)
            tempNormals.add(tokens[2].toFloatOrNull() ?: 0f)
            tempNormals.add(tokens[3].toFloatOrNull() ?: 0f)
          }
        }
        "vt" -> {
          if (tokens.size >= 3) {
            tempUvs.add(tokens[1].toFloatOrNull() ?: 0f)
            tempUvs.add(tokens[2].toFloatOrNull() ?: 0f)
          }
        }
        "f" -> {
          // Parse polygon faces (triangulate if > 3 vertices)
          if (tokens.size >= 4) {
            val faceIndices = ArrayList<Int>()
            for (i in 1 until tokens.size) {
              val part = tokens[i].split("/")
              val vIdx = (part[0].toIntOrNull() ?: 1) - 1 // 1-based index to 0-based
              faceIndices.add(vIdx)
            }
            // Fan triangulation
            for (i in 1 until faceIndices.size - 1) {
              outIndices.add(faceIndices[0])
              outIndices.add(faceIndices[i])
              outIndices.add(faceIndices[i + 1])
            }
          }
        }
      }
    }

    if (tempPositions.isEmpty() || outIndices.isEmpty()) return null

    // Normalize positions to fit [-1.0, 1.0]
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

    for (i in 0 until tempPositions.size step 3) {
      val x = tempPositions[i]
      val y = tempPositions[i + 1]
      val z = tempPositions[i + 2]
      if (x < minX) minX = x; if (x > maxX) maxX = x
      if (y < minY) minY = y; if (y > maxY) maxY = y
      if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
    }

    val maxExtent = maxOf(abs(maxX - minX), abs(maxY - minY), abs(maxZ - minZ)).coerceAtLeast(0.001f)
    val midX = (minX + maxX) / 2f
    val midY = (minY + maxY) / 2f
    val midZ = (minZ + maxZ) / 2f

    val vCount = tempPositions.size / 3
    val finalVertices = FloatArray(vCount * 3)
    val finalNormals = FloatArray(vCount * 3)
    val finalUvs = FloatArray(vCount * 2)
    val finalColors = IntArray(vCount)

    for (i in 0 until vCount) {
      val idx = i * 3
      val nx = ((tempPositions[idx] - midX) / maxExtent) * 1.8f
      val ny = ((tempPositions[idx + 1] - midY) / maxExtent) * 1.8f
      val nz = ((tempPositions[idx + 2] - midZ) / maxExtent) * 1.8f

      finalVertices[idx] = nx
      finalVertices[idx + 1] = ny
      finalVertices[idx + 2] = nz

      if (idx + 2 < tempNormals.size) {
        finalNormals[idx] = tempNormals[idx]
        finalNormals[idx + 1] = tempNormals[idx + 1]
        finalNormals[idx + 2] = tempNormals[idx + 2]
      } else {
        // Fallback normal pointing outward
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)
        finalNormals[idx] = nx / len
        finalNormals[idx + 1] = ny / len
        finalNormals[idx + 2] = nz / len
      }

      val uIdx = i * 2
      if (uIdx + 1 < tempUvs.size) {
        finalUvs[uIdx] = tempUvs[uIdx]
        finalUvs[uIdx + 1] = tempUvs[uIdx + 1]
      } else {
        finalUvs[uIdx] = (nx * 0.5f + 0.5f).coerceIn(0f, 1f)
        finalUvs[uIdx + 1] = (ny * 0.5f + 0.5f).coerceIn(0f, 1f)
      }

      // Shaded studio color
      val heightNorm = ((ny + 1f) * 0.5f).coerceIn(0f, 1f)
      val r = (40 + heightNorm * 180).toInt().coerceIn(0, 255)
      val g = (60 + heightNorm * 190).toInt().coerceIn(0, 255)
      val b = (100 + heightNorm * 155).toInt().coerceIn(0, 255)
      finalColors[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    return Mesh3D(
      vertices = finalVertices,
      normals = finalNormals,
      uvs = finalUvs,
      indices = outIndices.toIntArray(),
      vertexColors = finalColors,
      solidBaseIndices = IntArray(0),
      gridWidth = vCount,
      gridHeight = 1,
      depthScale = 1.0f,
      textureBitmap = null
    )
  }
}
