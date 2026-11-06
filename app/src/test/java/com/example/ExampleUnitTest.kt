package com.example

import com.example.engine.MarchingCubes
import com.example.engine.MarchingCubesTriTable
import com.example.engine.Mesh3D
import com.example.engine.ModelDownloadManager
import com.example.engine.ObjParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun modelDownloadManager_configuresRealTripoSrFullAndLiteModels() {
    // Full 1.65 GB Official Model
    val triposrFull = ModelDownloadManager.officialModels.find { it.id == "stability_triposr_full" }
    assertNotNull(triposrFull)
    assertEquals("triposr_fp32.ckpt", triposrFull!!.fileName)
    assertEquals(1650.0f, triposrFull.sizeMb)
    assertTrue("Download URL must point to Hugging Face", triposrFull.downloadUrl.contains("huggingface.co"))
    assertTrue("Capabilities must include Memory-Mapped MMap", triposrFull.capabilities.contains("Memory-Mapped MMap"))
    assertTrue("Capabilities must include Full 1.65GB FP32", triposrFull.capabilities.contains("Full 1.65GB FP32"))

    // Lite 185 MB Model
    val triposrLite = ModelDownloadManager.officialModels.find { it.id == "stability_triposr" }
    assertNotNull(triposrLite)
    assertEquals("triposr_int8.onnx", triposrLite!!.fileName)
    assertEquals(185.0f, triposrLite.sizeMb)
    assertTrue("Download URL must point to Hugging Face", triposrLite.downloadUrl.contains("huggingface.co"))
    assertTrue("Capabilities must include Mobile Lite 185MB", triposrLite.capabilities.contains("Mobile Lite 185MB"))
  }

  @Test
  fun marchingCubes_triTableHas256Cases() {
    assertEquals(256, MarchingCubesTriTable.TRI_TABLE.size)
    // Case 0 (all outside) and Case 255 (all inside) have no triangles
    assertEquals(-1, MarchingCubesTriTable.TRI_TABLE[0][0])
    assertEquals(-1, MarchingCubesTriTable.TRI_TABLE[255][0])
  }

  @Test
  fun marchingCubes_extractsIsosurfaceFromVolumetricDensityField() {
    val res = 8
    val volume = Array(res) { Array(res) { FloatArray(res) } }
    val step = 2.0f / (res - 1)

    // Sphere density field
    for (x in 0 until res) {
      val nx = -1.0f + x * step
      for (y in 0 until res) {
        val ny = -1.0f + y * step
        for (z in 0 until res) {
          val nz = -1.0f + z * step
          val distSq = nx * nx + ny * ny + nz * nz
          volume[x][y][z] = 0.5f - distSq
        }
      }
    }

    val mesh = MarchingCubes.extractIsosurface(
      volume = volume,
      isovalue = 0.0f,
      boundsMin = floatArrayOf(-1f, -1f, -1f),
      boundsMax = floatArrayOf(1f, 1f, 1f)
    )

    assertTrue("Marching cubes must extract vertices", mesh.vertices.isNotEmpty())
    assertTrue("Marching cubes must extract indices", mesh.indices.isNotEmpty())
    assertEquals(0, mesh.vertices.size % 3)
    assertEquals(0, mesh.indices.size % 3)
  }

  @Test
  fun objParser_parsesValidWavefrontGeometry() {
    val sampleObj = """
      # OBJ geometry
      v -0.5 -0.5 0.0
      v 0.5 -0.5 0.0
      v 0.0 0.5 0.0
      f 1 2 3
    """.trimIndent()
    val mesh = ObjParser.parse(sampleObj)
    assertNotNull(mesh)
    assertEquals(3, mesh!!.vertexCount)
    assertEquals(1, mesh.triangleCount)
  }

  @Test
  fun mesh3D_countsVerticesAndTrianglesCorrectly() {
    val vertices = floatArrayOf(
      0f, 0f, 0f,
      1f, 0f, 0f,
      0f, 1f, 0f
    )
    val normals = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f)
    val uvs = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    val indices = intArrayOf(0, 1, 2)
    val colors = intArrayOf(-1, -1, -1)
    val mesh = Mesh3D(
      vertices = vertices,
      normals = normals,
      uvs = uvs,
      indices = indices,
      vertexColors = colors,
      solidBaseIndices = intArrayOf(0, 2, 1),
      gridWidth = 2,
      gridHeight = 2,
      depthScale = 1.0f
    )

    assertEquals(3, mesh.vertexCount)
    assertEquals(1, mesh.triangleCount)
    assertEquals(2, mesh.solidTriangleCount)
  }
}
