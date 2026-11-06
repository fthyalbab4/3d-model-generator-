package com.example.engine.exporters

import android.content.Context
import android.graphics.Bitmap
import com.example.engine.Mesh3D
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.Locale

object ObjExporter {

  data class ObjExportResult(
    val objFile: File,
    val mtlFile: File,
    val textureFile: File?
  )

  fun export(context: Context, mesh: Mesh3D, baseName: String = "model_3d"): ObjExportResult {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val objFile = File(exportDir, "$baseName.obj")
    val mtlFile = File(exportDir, "$baseName.mtl")
    val texFile = File(exportDir, "${baseName}_texture.png")

    // Save texture PNG if available
    mesh.textureBitmap?.let { bmp ->
      FileOutputStream(texFile).use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
      }
    }

    // Write MTL file
    PrintWriter(mtlFile).use { writer ->
      writer.println("# 3D Studio MTL Material File")
      writer.println("newmtl Material_Model")
      writer.println("Ka 0.2 0.2 0.2")
      writer.println("Kd 0.9 0.9 0.9")
      writer.println("Ks 0.1 0.1 0.1")
      writer.println("Ns 20.0")
      writer.println("illum 2")
      if (texFile.exists()) {
        writer.println("map_Kd ${texFile.name}")
      }
    }

    // Write OBJ file
    PrintWriter(objFile).use { writer ->
      writer.println("# 3D Studio Reconstructed 3D Mesh")
      writer.println("mtllib ${mtlFile.name}")
      writer.println("o $baseName")

      val vCount = mesh.vertexCount
      val v = mesh.vertices
      val vt = mesh.uvs
      val vn = mesh.normals

      // Write vertices
      for (i in 0 until vCount) {
        val idx = i * 3
        writer.println(String.format(Locale.US, "v %.4f %.4f %.4f", v[idx], v[idx + 1], v[idx + 2]))
      }

      // Write texture coords (v flipped for OBJ standard)
      for (i in 0 until vCount) {
        val idx = i * 2
        writer.println(String.format(Locale.US, "vt %.4f %.4f", vt[idx], 1.0f - vt[idx + 1]))
      }

      // Write normals
      for (i in 0 until vCount) {
        val idx = i * 3
        writer.println(String.format(Locale.US, "vn %.4f %.4f %.4f", vn[idx], vn[idx + 1], vn[idx + 2]))
      }

      // Write faces (1-indexed)
      writer.println("usemtl Material_Model")
      writer.println("s 1")

      val ind = mesh.indices
      for (i in ind.indices step 3) {
        val i1 = ind[i] + 1
        val i2 = ind[i + 1] + 1
        val i3 = ind[i + 2] + 1
        writer.println("f $i1/$i1/$i1 $i2/$i2/$i2 $i3/$i3/$i3")
      }
    }

    return ObjExportResult(objFile, mtlFile, if (texFile.exists()) texFile else null)
  }
}
