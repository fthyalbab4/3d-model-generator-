#ifndef GAUSSIAN_SPLAT_ENGINE_H
#define GAUSSIAN_SPLAT_ENGINE_H

#include <vector>
#include <string>
#include <cstdint>

struct GaussianSplat {
    float x, y, z;
    float sx, sy, sz;
    float qw, qx, qy, qz;
    float r, g, b;
    float opacity;
};

class GaussianSplatEngine {
public:
    // Fast C++ back-to-front depth sorting of 3D Gaussians for alpha-blended rendering
    static void sortGaussiansByDepth(
        const float* positions,
        int count,
        const float* viewMatrix16,
        int* outSortedIndices
    );

    // Exports 3D Gaussians to standard binary_little_endian .ply format (3DGS compatible)
    static bool exportToPly(
        const std::string& filePath,
        const float* positions,
        const float* scales,
        const float* rotations,
        const float* colors,
        const float* opacities,
        int count
    );

    // Imports 3D Gaussians from .ply file (supports both binary_little_endian and ascii)
    static bool importFromPly(
        const std::string& filePath,
        std::vector<GaussianSplat>& outSplats
    );
};

#endif // GAUSSIAN_SPLAT_ENGINE_H
