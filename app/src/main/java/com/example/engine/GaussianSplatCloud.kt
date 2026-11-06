package com.example.engine

/**
 * Represents a 3D Gaussian Splatting cloud (compatible with Idol-SDK & 3DGS .ply format).
 */
data class GaussianSplatCloud(
  val count: Int,
  val positions: FloatArray,   // 3 floats per splat (x, y, z)
  val scales: FloatArray,      // 3 floats per splat (sx, sy, sz)
  val rotations: FloatArray,   // 4 floats per splat (qw, qx, qy, qz)
  val colors: FloatArray,      // 3 floats per splat (r, g, b in 0..1)
  val opacities: FloatArray,   // 1 float per splat (0..1)
  val sourceName: String = "GaussianSplat"
)

object GaussianSplatNative {
  init {
    try {
      System.loadLibrary("ai_inference_engine")
    } catch (e: UnsatisfiedLinkError) {
      e.printStackTrace()
    }
  }

  external fun nativeSortGaussians(
    positions: FloatArray,
    count: Int,
    viewMatrix: FloatArray,
    outSortedIndices: IntArray
  )

  external fun nativeExportPly(
    filePath: String,
    positions: FloatArray,
    scales: FloatArray,
    rotations: FloatArray,
    colors: FloatArray,
    opacities: FloatArray,
    count: Int
  ): Boolean

  external fun nativeImportPly(filePath: String): FloatArray?

  fun importPly(filePath: String, name: String = "Imported.ply"): GaussianSplatCloud? {
    val packed = nativeImportPly(filePath) ?: return null
    val stride = 14
    val count = packed.size / stride
    if (count <= 0) return null

    val positions = FloatArray(count * 3)
    val scales = FloatArray(count * 3)
    val rotations = FloatArray(count * 4)
    val colors = FloatArray(count * 3)
    val opacities = FloatArray(count)

    for (i in 0 until count) {
      val base = i * stride
      positions[i * 3 + 0] = packed[base + 0]
      positions[i * 3 + 1] = packed[base + 1]
      positions[i * 3 + 2] = packed[base + 2]

      scales[i * 3 + 0] = packed[base + 3]
      scales[i * 3 + 1] = packed[base + 4]
      scales[i * 3 + 2] = packed[base + 5]

      rotations[i * 4 + 0] = packed[base + 6]
      rotations[i * 4 + 1] = packed[base + 7]
      rotations[i * 4 + 2] = packed[base + 8]
      rotations[i * 4 + 3] = packed[base + 9]

      colors[i * 3 + 0] = packed[base + 10]
      colors[i * 3 + 1] = packed[base + 11]
      colors[i * 3 + 2] = packed[base + 12]

      opacities[i] = packed[base + 13]
    }

    return GaussianSplatCloud(
      count = count,
      positions = positions,
      scales = scales,
      rotations = rotations,
      colors = colors,
      opacities = opacities,
      sourceName = name
    )
  }
}
