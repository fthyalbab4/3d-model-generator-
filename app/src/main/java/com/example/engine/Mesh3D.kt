package com.example.engine

import android.graphics.Bitmap

/**
 * 3D Mesh data container containing indexed vertex attributes,
 * surface normal vectors, texture UV coordinates, and vertex colors.
 */
data class Mesh3D(
  val vertices: FloatArray,       // x, y, z triplets
  val normals: FloatArray,        // nx, ny, nz triplets
  val uvs: FloatArray,            // u, v pairs (0.0 to 1.0)
  val indices: IntArray,          // surface triangle indices
  val vertexColors: IntArray,     // ARGB per vertex
  val solidBaseIndices: IntArray, // additional triangles closing side skirts & bottom base for 3D printing
  val gridWidth: Int,
  val gridHeight: Int,
  val depthScale: Float,
  val textureBitmap: Bitmap? = null
) {
  val vertexCount: Int get() = vertices.size / 3
  val triangleCount: Int get() = indices.size / 3
  val solidTriangleCount: Int get() = (indices.size + solidBaseIndices.size) / 3

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as Mesh3D
    return vertices.contentEquals(other.vertices) &&
      indices.contentEquals(other.indices) &&
      gridWidth == other.gridWidth &&
      gridHeight == other.gridHeight
  }

  override fun hashCode(): Int {
    var result = vertices.contentHashCode()
    result = 31 * result + indices.contentHashCode()
    result = 31 * result + gridWidth
    result = 31 * result + gridHeight
    return result
  }
}
