package com.example.engine.exporters

import android.content.Context
import com.example.engine.Mesh3D
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * 3D Printable STL Exporter.
 * Generates watertight manifold binary STL files for 3D slicers (Cura, PrusaSlicer, Bambu Studio).
 */
object StlExporter {

  fun export(context: Context, mesh: Mesh3D, baseName: String = "model_3d"): File {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val stlFile = File(exportDir, "$baseName.stl")

    val v = mesh.vertices
    val isClosed360 = mesh.solidBaseIndices.isEmpty()
    val ind = mesh.indices

    val totalTriangles: Int
    if (isClosed360) {
      totalTriangles = ind.size / 3
    } else {
      val gridW = mesh.gridWidth
      val gridH = mesh.gridHeight
      val topTriangles = ind.size / 3
      val sideTriangles = ((gridW - 1) * 2 + (gridH - 1) * 2) * 2
      val bottomTriangles = (gridW - 1) * (gridH - 1) * 2
      totalTriangles = topTriangles + sideTriangles + bottomTriangles
    }

    FileOutputStream(stlFile).use { fos ->
      BufferedOutputStream(fos, 64 * 1024).use { bos ->
        // Binary STL Header: 80 bytes
        val header = ByteArray(80)
        "3D Studio 360 Solid Manifold Mesh".toByteArray(Charsets.US_ASCII).copyInto(header)
        bos.write(header)

        // 4 bytes: Triangle count (Little-Endian UInt32)
        val countBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        countBuf.putInt(totalTriangles)
        bos.write(countBuf.array())

        val facetBuf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)

        fun writeFacet(
          v1x: Float, v1y: Float, v1z: Float,
          v2x: Float, v2y: Float, v2z: Float,
          v3x: Float, v3y: Float, v3z: Float
        ) {
          facetBuf.clear()
          val ax = v2x - v1x; val ay = v2y - v1y; val az = v2z - v1z
          val bx = v3x - v1x; val by = v3y - v1y; val bz = v3z - v1z
          var nx = ay * bz - az * by
          var ny = az * bx - ax * bz
          var nz = ax * by - ay * bx
          val len = sqrt(nx * nx + ny * ny + nz * nz)
          if (len > 0.00001f) {
            nx /= len; ny /= len; nz /= len
          } else {
            nx = 0f; ny = 0f; nz = 1f
          }

          facetBuf.putFloat(nx)
          facetBuf.putFloat(ny)
          facetBuf.putFloat(nz)

          facetBuf.putFloat(v1x)
          facetBuf.putFloat(v1y)
          facetBuf.putFloat(v1z)

          facetBuf.putFloat(v2x)
          facetBuf.putFloat(v2y)
          facetBuf.putFloat(v2z)

          facetBuf.putFloat(v3x)
          facetBuf.putFloat(v3y)
          facetBuf.putFloat(v3z)

          facetBuf.putShort(0.toShort()) // Attribute byte count
          bos.write(facetBuf.array())
        }

        // Write primary surface triangles
        for (i in ind.indices step 3) {
          val i1 = ind[i] * 3
          val i2 = ind[i + 1] * 3
          val i3 = ind[i + 2] * 3
          writeFacet(
            v[i1], v[i1 + 1], v[i1 + 2],
            v[i2], v[i2 + 1], v[i2 + 2],
            v[i3], v[i3 + 1], v[i3 + 2]
          )
        }

        // If open relief grid, write enclosure walls & floor
        if (!isClosed360) {
          val gridW = mesh.gridWidth
          val gridH = mesh.gridHeight
          var minZ = Float.MAX_VALUE
          for (i in 2 until v.size step 3) {
            if (v[i] < minZ) minZ = v[i]
          }
          val baseFloorZ = minZ - 0.25f

          // Top edge: y = 0
          for (x in 0 until gridW - 1) {
            val idx1 = x * 3
            val idx2 = (x + 1) * 3
            val x1 = v[idx1]; val y1 = v[idx1 + 1]; val z1 = v[idx1 + 2]
            val x2 = v[idx2]; val y2 = v[idx2 + 1]; val z2 = v[idx2 + 2]
            writeFacet(x1, y1, z1, x2, y2, z2, x1, y1, baseFloorZ)
            writeFacet(x2, y2, z2, x2, y2, baseFloorZ, x1, y1, baseFloorZ)
          }

          // Bottom edge: y = gridH - 1
          val lastRow = (gridH - 1) * gridW
          for (x in 0 until gridW - 1) {
            val idx1 = (lastRow + x) * 3
            val idx2 = (lastRow + x + 1) * 3
            val x1 = v[idx1]; val y1 = v[idx1 + 1]; val z1 = v[idx1 + 2]
            val x2 = v[idx2]; val y2 = v[idx2 + 1]; val z2 = v[idx2 + 2]
            writeFacet(x1, y1, z1, x1, y1, baseFloorZ, x2, y2, z2)
            writeFacet(x2, y2, z2, x1, y1, baseFloorZ, x2, y2, baseFloorZ)
          }

          // Left edge: x = 0
          for (y in 0 until gridH - 1) {
            val idx1 = (y * gridW) * 3
            val idx2 = ((y + 1) * gridW) * 3
            val x1 = v[idx1]; val y1 = v[idx1 + 1]; val z1 = v[idx1 + 2]
            val x2 = v[idx2]; val y2 = v[idx2 + 1]; val z2 = v[idx2 + 2]
            writeFacet(x1, y1, z1, x1, y1, baseFloorZ, x2, y2, z2)
            writeFacet(x2, y2, z2, x1, y1, baseFloorZ, x2, y2, baseFloorZ)
          }

          // Right edge: x = gridW - 1
          for (y in 0 until gridH - 1) {
            val idx1 = (y * gridW + (gridW - 1)) * 3
            val idx2 = ((y + 1) * gridW + (gridW - 1)) * 3
            val x1 = v[idx1]; val y1 = v[idx1 + 1]; val z1 = v[idx1 + 2]
            val x2 = v[idx2]; val y2 = v[idx2 + 1]; val z2 = v[idx2 + 2]
            writeFacet(x1, y1, z1, x2, y2, z2, x1, y1, baseFloorZ)
            writeFacet(x2, y2, z2, x2, y2, baseFloorZ, x1, y1, baseFloorZ)
          }

          // Bottom plate
          for (y in 0 until gridH - 1) {
            val row = y * gridW
            val nextRow = (y + 1) * gridW
            for (x in 0 until gridW - 1) {
              val iTL = (row + x) * 3
              val iTR = (row + x + 1) * 3
              val iBL = (nextRow + x) * 3
              val iBR = (nextRow + x + 1) * 3
              val xTL = v[iTL]; val yTL = v[iTL + 1]
              val xTR = v[iTR]; val yTR = v[iTR + 1]
              val xBL = v[iBL]; val yBL = v[iBL + 1]
              val xBR = v[iBR]; val yBR = v[iBR + 1]
              writeFacet(xTL, yTL, baseFloorZ, xTR, yTR, baseFloorZ, xBL, yBL, baseFloorZ)
              writeFacet(xTR, yTR, baseFloorZ, xBR, yBR, baseFloorZ, xBL, yBL, baseFloorZ)
            }
          }
        }
      }
    }

    return stlFile
  }
}
