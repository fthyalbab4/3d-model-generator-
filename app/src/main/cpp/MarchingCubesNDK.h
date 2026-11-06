#ifndef MARCHING_CUBES_NDK_H
#define MARCHING_CUBES_NDK_H

#include <vector>

class MarchingCubesNDK {
public:
    // Extracts a watertight 3D polygon mesh isosurface from a 3D scalar density grid (NeRF output)
    static void extractIsosurface(
        const std::vector<float>& density_grid,
        int sizeX, int sizeY, int sizeZ,
        float isovalue,
        std::vector<float>& out_vertices,
        std::vector<float>& out_normals,
        std::vector<int>& out_indices
    );

private:
    static const int edgeTable[256];
};

#endif // MARCHING_CUBES_NDK_H
