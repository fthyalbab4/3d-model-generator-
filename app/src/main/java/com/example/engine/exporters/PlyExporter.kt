package com.example.engine.exporters

import android.content.Context
import android.graphics.Color
import com.example.engine.Mesh3D
import java.io.File
import java.io.PrintWriter
import java.util.Locale

/**
 * Stanford PLY Exporter.
 * Generates standard .ply files containing vertex geometry, surface normals, colors, and face indices.
 */
object PlyExporter {

  fun export(context: Context, mesh: Mesh3D, baseName: String = "model_3d"): File {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val plyFile = File(exportDir, "$baseName.ply")

    val vCount = mesh.vertexCount
    val fCount = mesh.indices.size / 3

    PrintWriter(plyFile).use { writer ->
      // Header
      writer.println("ply")
      writer.println("format ascii 1.0")
      writer.println("comment 3D Studio Stanford Polygon File")
      writer.println("element vertex $vCount")
      writer.println("property float x")
      writer.println("property float y")
      writer.println("property float z")
      writer.println("property float nx")
      writer.println("property float ny")
      writer.println("property float nz")
      writer.println("property uchar red")
      writer.println("property uchar green")
      writer.println("property uchar blue")
      writer.println("element face $fCount")
      writer.println("property list uchar int vertex_indices")
      writer.println("end_header")

      val v = mesh.vertices
      val vn = mesh.normals
      val c = mesh.vertexColors

      // Vertices
      for (i in 0 until vCount) {
        val vi = i * 3
        val col = c[i]
        val r = Color.red(col)
        val g = Color.green(col)
        val b = Color.blue(col)
        writer.println(
          String.format(
            Locale.US,
            "%.4f %.4f %.4f %.4f %.4f %.4f %d %d %d",
            v[vi], v[vi + 1], v[vi + 2],
            vn[vi], vn[vi + 1], vn[vi + 2],
            r, g, b
          )
        )
      }

      // Faces
      val ind = mesh.indices
      for (i in ind.indices step 3) {
        writer.println("3 ${ind[i]} ${ind[i + 1]} ${ind[i + 2]}")
      }
    }

    return plyFile
  }
}
