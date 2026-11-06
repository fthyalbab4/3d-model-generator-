package com.example.engine

import kotlin.math.sqrt

/**
 * Marching Cubes 3D Isosurface Extractor.
 * Extracts a watertight manifold 360° polygon mesh from a 3D scalar volume/SDF.
 */
object MarchingCubes {

  // Edge table: indicates which of the 12 edges of a cube intersect the isosurface (256 entries)
  val edgeTable = IntArray(256) { i ->
    var edges = 0
    // Edge flags based on cube configuration
    val v = IntArray(8) { bit -> (i shr bit) and 1 }
    if (v[0] != v[1]) edges = edges or 1
    if (v[1] != v[2]) edges = edges or 2
    if (v[2] != v[3]) edges = edges or 4
    if (v[3] != v[0]) edges = edges or 8
    if (v[4] != v[5]) edges = edges or 16
    if (v[5] != v[6]) edges = edges or 32
    if (v[6] != v[7]) edges = edges or 64
    if (v[7] != v[4]) edges = edges or 128
    if (v[0] != v[4]) edges = edges or 256
    if (v[1] != v[5]) edges = edges or 512
    if (v[2] != v[6]) edges = edges or 1024
    if (v[3] != v[7]) edges = edges or 2048
    edges
  }

  // Edge vertex connections: pairs of vertex indices (0..7) for edges 0..11
  private val edgeVertices = arrayOf(
    intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
    intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
    intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
  )

  // Cube vertex offsets relative to cube origin (0, 0, 0)
  private val cubeOffsets = arrayOf(
    floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 0f, 0f),
    floatArrayOf(1f, 1f, 0f), floatArrayOf(0f, 1f, 0f),
    floatArrayOf(0f, 0f, 1f), floatArrayOf(1f, 0f, 1f),
    floatArrayOf(1f, 1f, 1f), floatArrayOf(0f, 1f, 1f)
  )

  data class RawMesh(
    val vertices: FloatArray,
    val normals: FloatArray,
    val indices: IntArray
  )

  /**
   * Extracts an isosurface mesh from a 3D scalar volume.
   *
   * @param volume 3D scalar field [sizeX][sizeY][sizeZ]
   * @param isovalue Threshold value defining the surface boundary
   * @param boundsMin World coordinate minimums (X, Y, Z)
   * @param boundsMax World coordinate maximums (X, Y, Z)
   */
  fun extractIsosurface(
    volume: Array<Array<FloatArray>>,
    isovalue: Float = 0.0f,
    boundsMin: FloatArray = floatArrayOf(-1.2f, -1.2f, -1.2f),
    boundsMax: FloatArray = floatArrayOf(1.2f, 1.2f, 1.2f)
  ): RawMesh {
    val sizeX = volume.size
    val sizeY = volume[0].size
    val sizeZ = volume[0][0].size

    val stepX = (boundsMax[0] - boundsMin[0]) / (sizeX - 1)
    val stepY = (boundsMax[1] - boundsMin[1]) / (sizeY - 1)
    val stepZ = (boundsMax[2] - boundsMin[2]) / (sizeZ - 1)

    val outVertices = ArrayList<Float>(sizeX * sizeY * 6)
    val outIndices = ArrayList<Int>(sizeX * sizeY * 6)

    val cellVals = FloatArray(8)
    val cellCorners = Array(8) { FloatArray(3) }
    val edgeIntersect = Array(12) { FloatArray(3) }

    for (x in 0 until sizeX - 1) {
      val wx = boundsMin[0] + x * stepX
      for (y in 0 until sizeY - 1) {
        val wy = boundsMin[1] + y * stepY
        for (z in 0 until sizeZ - 1) {
          val wz = boundsMin[2] + z * stepZ

          // Sample 8 corners of the voxel cube
          cellVals[0] = volume[x][y][z]
          cellVals[1] = volume[x + 1][y][z]
          cellVals[2] = volume[x + 1][y + 1][z]
          cellVals[3] = volume[x][y + 1][z]
          cellVals[4] = volume[x][y][z + 1]
          cellVals[5] = volume[x + 1][y][z + 1]
          cellVals[6] = volume[x + 1][y + 1][z + 1]
          cellVals[7] = volume[x][y + 1][z + 1]

          // Compute cube index (8-bit bitmask)
          var cubeIndex = 0
          for (i in 0..7) {
            if (cellVals[i] < isovalue) cubeIndex = cubeIndex or (1 shl i)
          }

          if (cubeIndex == 0 || cubeIndex == 255) continue

          // Populate corner coordinates
          for (i in 0..7) {
            cellCorners[i][0] = wx + cubeOffsets[i][0] * stepX
            cellCorners[i][1] = wy + cubeOffsets[i][1] * stepY
            cellCorners[i][2] = wz + cubeOffsets[i][2] * stepZ
          }

          val edges = edgeTable[cubeIndex]
          if (edges == 0) continue

          // Linear interpolation along intersecting edges
          for (e in 0..11) {
            if ((edges and (1 shl e)) != 0) {
              val v1 = edgeVertices[e][0]
              val v2 = edgeVertices[e][1]
              val val1 = cellVals[v1]
              val val2 = cellVals[v2]
              val t = if (kotlin.math.abs(val2 - val1) > 0.00001f) {
                (isovalue - val1) / (val2 - val1)
              } else 0.5f

              val clampedT = t.coerceIn(0f, 1f)
              edgeIntersect[e][0] = cellCorners[v1][0] + clampedT * (cellCorners[v2][0] - cellCorners[v1][0])
              edgeIntersect[e][1] = cellCorners[v1][1] + clampedT * (cellCorners[v2][1] - cellCorners[v1][1])
              edgeIntersect[e][2] = cellCorners[v1][2] + clampedT * (cellCorners[v2][2] - cellCorners[v1][2])
            }
          }

          // Triangulate using canonical Marching Cubes triangle lookup
          val tris = MarchingCubesTriTable.TRI_TABLE[cubeIndex]
          var tIdx = 0
          while (tIdx < tris.size && tris[tIdx] != -1) {
            val e0 = tris[tIdx]
            val e1 = tris[tIdx + 1]
            val e2 = tris[tIdx + 2]
            tIdx += 3

            val baseIndex = outVertices.size / 3
            // Vertex 0
            outVertices.add(edgeIntersect[e0][0])
            outVertices.add(edgeIntersect[e0][1])
            outVertices.add(edgeIntersect[e0][2])
            // Vertex 1
            outVertices.add(edgeIntersect[e1][0])
            outVertices.add(edgeIntersect[e1][1])
            outVertices.add(edgeIntersect[e1][2])
            // Vertex 2
            outVertices.add(edgeIntersect[e2][0])
            outVertices.add(edgeIntersect[e2][1])
            outVertices.add(edgeIntersect[e2][2])

            outIndices.add(baseIndex)
            outIndices.add(baseIndex + 1)
            outIndices.add(baseIndex + 2)
          }
        }
      }
    }

    val vArray = outVertices.toFloatArray()
    val iArray = outIndices.toIntArray()
    val nArray = computeNormals(vArray, iArray)

    return RawMesh(vArray, nArray, iArray)
  }

  private fun computeNormals(vertices: FloatArray, indices: IntArray): FloatArray {
    val normals = FloatArray(vertices.size)
    for (i in indices.indices step 3) {
      val i0 = indices[i] * 3
      val i1 = indices[i + 1] * 3
      val i2 = indices[i + 2] * 3

      val ax = vertices[i1] - vertices[i0]
      val ay = vertices[i1 + 1] - vertices[i0 + 1]
      val az = vertices[i1 + 2] - vertices[i0 + 2]

      val bx = vertices[i2] - vertices[i0]
      val by = vertices[i2 + 1] - vertices[i0 + 1]
      val bz = vertices[i2 + 2] - vertices[i0 + 2]

      var nx = ay * bz - az * by
      var ny = az * bx - ax * bz
      var nz = ax * by - ay * bx
      val len = sqrt(nx * nx + ny * ny + nz * nz)
      if (len > 0.00001f) {
        nx /= len; ny /= len; nz /= len
      } else {
        nx = 0f; ny = 1f; nz = 0f
      }

      normals[i0] += nx; normals[i0 + 1] += ny; normals[i0 + 2] += nz
      normals[i1] += nx; normals[i1 + 1] += ny; normals[i1 + 2] += nz
      normals[i2] += nx; normals[i2 + 1] += ny; normals[i2 + 2] += nz
    }

    // Normalize all normals
    for (i in 0 until vertices.size step 3) {
      var nx = normals[i]
      var ny = normals[i + 1]
      var nz = normals[i + 2]
      val len = sqrt(nx * nx + ny * ny + nz * nz)
      if (len > 0.00001f) {
        normals[i] = nx / len
        normals[i + 1] = ny / len
        normals[i + 2] = nz / len
      } else {
        normals[i] = 0f; normals[i + 1] = 1f; normals[i + 2] = 0f
      }
    }
    return normals
  }
}
