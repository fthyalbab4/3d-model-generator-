package com.example.engine.exporters

import android.content.Context
import android.util.Base64
import com.example.engine.Mesh3D
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * glTF 2.0 Exporter.
 * Produces self-contained standard glTF files with embedded binary geometry buffer.
 */
object GltfExporter {

  fun export(context: Context, mesh: Mesh3D, baseName: String = "model_3d"): File {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val gltfFile = File(exportDir, "$baseName.gltf")

    val vCount = mesh.vertexCount
    val indices = mesh.indices
    val iCount = indices.size

    // Calculate buffer sizes
    // Positions: vCount * 3 * 4 bytes (Float32)
    // Normals: vCount * 3 * 4 bytes (Float32)
    // UVs: vCount * 2 * 4 bytes (Float32)
    // Indices: iCount * 4 bytes (UInt32)
    val posBytes = vCount * 12
    val normBytes = vCount * 12
    val uvBytes = vCount * 8
    val indBytes = iCount * 4
    val totalBufferBytes = posBytes + normBytes + uvBytes + indBytes

    val buffer = ByteBuffer.allocate(totalBufferBytes).order(ByteOrder.LITTLE_ENDIAN)

    // Write positions
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE

    for (i in 0 until vCount) {
      val idx = i * 3
      val x = mesh.vertices[idx]
      val y = mesh.vertices[idx + 1]
      val z = mesh.vertices[idx + 2]
      if (x < minX) minX = x; if (x > maxX) maxX = x
      if (y < minY) minY = y; if (y > maxY) maxY = y
      if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
      buffer.putFloat(x)
      buffer.putFloat(y)
      buffer.putFloat(z)
    }

    // Write normals
    for (i in 0 until vCount) {
      val idx = i * 3
      buffer.putFloat(mesh.normals[idx])
      buffer.putFloat(mesh.normals[idx + 1])
      buffer.putFloat(mesh.normals[idx + 2])
    }

    // Write UVs
    for (i in 0 until vCount) {
      val idx = i * 2
      buffer.putFloat(mesh.uvs[idx])
      buffer.putFloat(mesh.uvs[idx + 1])
    }

    // Write Indices
    for (i in 0 until iCount) {
      buffer.putInt(indices[i])
    }

    val base64Data = Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    val uri = "data:application/octet-stream;base64,$base64Data"

    PrintWriter(gltfFile).use { w ->
      w.println("{")
      w.println("  \"asset\": { \"version\": \"2.0\", \"generator\": \"3D Studio Mobile\" },")
      w.println("  \"scenes\": [{ \"nodes\": [0] }],")
      w.println("  \"nodes\": [{ \"mesh\": 0, \"name\": \"$baseName\" }],")
      w.println("  \"meshes\": [{")
      w.println("    \"primitives\": [{")
      w.println("      \"attributes\": { \"POSITION\": 0, \"NORMAL\": 1, \"TEXCOORD_0\": 2 },")
      w.println("      \"indices\": 3,")
      w.println("      \"mode\": 4")
      w.println("    }]")
      w.println("  }],")
      w.println("  \"buffers\": [{ \"byteLength\": $totalBufferBytes, \"uri\": \"$uri\" }],")
      w.println("  \"bufferViews\": [")
      w.println("    { \"buffer\": 0, \"byteOffset\": 0, \"byteLength\": $posBytes, \"target\": 34962 },")
      w.println("    { \"buffer\": 0, \"byteOffset\": $posBytes, \"byteLength\": $normBytes, \"target\": 34962 },")
      w.println("    { \"buffer\": 0, \"byteOffset\": ${posBytes + normBytes}, \"byteLength\": $uvBytes, \"target\": 34962 },")
      w.println("    { \"buffer\": 0, \"byteOffset\": ${posBytes + normBytes + uvBytes}, \"byteLength\": $indBytes, \"target\": 34963 }")
      w.println("  ],")
      w.println("  \"accessors\": [")
      w.println("    { \"bufferView\": 0, \"byteOffset\": 0, \"componentType\": 5126, \"count\": $vCount, \"type\": \"VEC3\", \"min\": [$minX, $minY, $minZ], \"max\": [$maxX, $maxY, $maxZ] },")
      w.println("    { \"bufferView\": 1, \"byteOffset\": 0, \"componentType\": 5126, \"count\": $vCount, \"type\": \"VEC3\" },")
      w.println("    { \"bufferView\": 2, \"byteOffset\": 0, \"componentType\": 5126, \"count\": $vCount, \"type\": \"VEC2\" },")
      w.println("    { \"bufferView\": 3, \"byteOffset\": 0, \"componentType\": 5125, \"count\": $iCount, \"type\": \"SCALAR\" }")
      w.println("  ]")
      w.println("}")
    }

    return gltfFile
  }
}
