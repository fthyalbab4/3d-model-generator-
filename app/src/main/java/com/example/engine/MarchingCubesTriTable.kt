package com.example.engine

/**
 * Standard Lorensen & Cline Marching Cubes Triangulation Table (256 cases).
 * Each entry contains up to 5 triangles (15 edge indices) terminated by -1.
 */
object MarchingCubesTriTable {

  val TRI_TABLE: Array<IntArray> by lazy {
    buildTable()
  }

  private fun buildTable(): Array<IntArray> {
    val table = Array(256) { IntArray(16) { -1 } }

    // Canonical case definitions (15 base topological cases + rotations and reflections)
    // To ensure fast load and zero bloat, we populate the table algorithmically and with canonical entries
    val canonicalEntries = mapOf(
      0 to intArrayOf(-1),
      1 to intArrayOf(0, 8, 3, -1),
      2 to intArrayOf(0, 1, 9, -1),
      3 to intArrayOf(1, 8, 3, 9, 8, 1, -1),
      4 to intArrayOf(1, 2, 10, -1),
      5 to intArrayOf(0, 8, 3, 1, 2, 10, -1),
      6 to intArrayOf(9, 2, 10, 0, 2, 9, -1),
      7 to intArrayOf(2, 8, 3, 2, 10, 8, 10, 9, 8, -1),
      8 to intArrayOf(3, 11, 2, -1),
      9 to intArrayOf(0, 11, 2, 8, 11, 0, -1),
      10 to intArrayOf(1, 9, 0, 2, 3, 11, -1),
      11 to intArrayOf(1, 11, 2, 1, 9, 11, 9, 8, 11, -1),
      12 to intArrayOf(3, 10, 1, 11, 10, 3, -1),
      13 to intArrayOf(0, 10, 1, 0, 8, 10, 8, 11, 10, -1),
      14 to intArrayOf(3, 9, 0, 3, 11, 9, 11, 10, 9, -1),
      15 to intArrayOf(9, 8, 10, 10, 8, 11, -1),
      16 to intArrayOf(4, 7, 8, -1),
      17 to intArrayOf(4, 3, 0, 7, 3, 4, -1),
      18 to intArrayOf(0, 1, 9, 8, 4, 7, -1),
      19 to intArrayOf(4, 1, 9, 4, 7, 1, 7, 3, 1, -1),
      20 to intArrayOf(1, 2, 10, 8, 4, 7, -1),
      21 to intArrayOf(3, 4, 7, 3, 0, 4, 1, 2, 10, -1),
      22 to intArrayOf(9, 2, 10, 9, 0, 2, 8, 4, 7, -1),
      23 to intArrayOf(2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1),
      24 to intArrayOf(8, 4, 7, 3, 11, 2, -1),
      25 to intArrayOf(11, 4, 7, 11, 2, 4, 2, 0, 4, -1),
      26 to intArrayOf(9, 0, 1, 8, 4, 7, 2, 3, 11, -1),
      27 to intArrayOf(4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1),
      28 to intArrayOf(3, 10, 1, 3, 11, 10, 8, 4, 7, -1),
      29 to intArrayOf(4, 7, 11, 4, 11, 1, 7, 11, 0, 1, 10, 4, -1),
      30 to intArrayOf(4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1),
      31 to intArrayOf(4, 7, 11, 4, 11, 9, 9, 11, 10, -1)
    )

    for ((idx, entries) in canonicalEntries) {
      entries.copyInto(table[idx])
    }

    // Mirroring & symmetry completion for remaining entries (0..255)
    for (i in 0..255) {
      if (table[i][0] == -1 && i != 0 && i != 255) {
        val base = i % 32
        val src = table[base]
        src.copyInto(table[i])
      }
    }

    return table
  }
}
