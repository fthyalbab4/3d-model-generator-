#include "GaussianSplatEngine.h"
#include <algorithm>
#include <fstream>
#include <sstream>
#include <cmath>
#include <cstring>

struct DepthIndexPair {
    float depth;
    int index;
};

void GaussianSplatEngine::sortGaussiansByDepth(
    const float* positions,
    int count,
    const float* viewMatrix16,
    int* outSortedIndices
) {
    if (count <= 0 || !positions || !viewMatrix16 || !outSortedIndices) return;

    std::vector<DepthIndexPair> pairs(count);
    // Column-major OpenGL view matrix:
    // z_cam = m[2]*x + m[6]*y + m[10]*z + m[14]
    const float m2 = viewMatrix16[2];
    const float m6 = viewMatrix16[6];
    const float m10 = viewMatrix16[10];
    const float m14 = viewMatrix16[14];

    for (int i = 0; i < count; ++i) {
        const float x = positions[i * 3 + 0];
        const float y = positions[i * 3 + 1];
        const float z = positions[i * 3 + 2];
        pairs[i].depth = m2 * x + m6 * y + m10 * z + m14;
        pairs[i].index = i;
    }

    // Sort back-to-front (most negative Z first in OpenGL camera space)
    std::sort(pairs.begin(), pairs.end(), [](const DepthIndexPair& a, const DepthIndexPair& b) {
        return a.depth < b.depth;
    });

    for (int i = 0; i < count; ++i) {
        outSortedIndices[i] = pairs[i].index;
    }
}

bool GaussianSplatEngine::exportToPly(
    const std::string& filePath,
    const float* positions,
    const float* scales,
    const float* rotations,
    const float* colors,
    const float* opacities,
    int count
) {
    std::ofstream out(filePath, std::ios::binary);
    if (!out.is_open()) return false;

    // Standard 3D Gaussian Splatting PLY header (compatible with Luma, SuperSplat, Blender)
    std::ostringstream header;
    header << "ply\n";
    header << "format binary_little_endian 1.0\n";
    header << "element vertex " << count << "\n";
    header << "property float x\n";
    header << "property float y\n";
    header << "property float z\n";
    header << "property float nx\n";
    header << "property float ny\n";
    header << "property float nz\n";
    header << "property float f_dc_0\n";
    header << "property float f_dc_1\n";
    header << "property float f_dc_2\n";
    header << "property float opacity\n";
    header << "property float scale_0\n";
    header << "property float scale_1\n";
    header << "property float scale_2\n";
    header << "property float rot_0\n";
    header << "property float rot_1\n";
    header << "property float rot_2\n";
    header << "property float rot_3\n";
    header << "end_header\n";

    std::string headerStr = header.str();
    out.write(headerStr.data(), headerStr.size());

    const float C0 = 0.28209479177387814f; // SH_C0

    for (int i = 0; i < count; ++i) {
        float x = positions[i * 3 + 0];
        float y = positions[i * 3 + 1];
        float z = positions[i * 3 + 2];

        float nx = 0.0f, ny = 1.0f, nz = 0.0f;

        // Convert RGB [0,1] to 0th-order Spherical Harmonics f_dc
        float f_dc_0 = (colors[i * 3 + 0] - 0.5f) / C0;
        float f_dc_1 = (colors[i * 3 + 1] - 0.5f) / C0;
        float f_dc_2 = (colors[i * 3 + 2] - 0.5f) / C0;

        // Convert linear opacity [0,1] to logit
        float op = std::clamp(opacities[i], 0.001f, 0.999f);
        float logitOp = std::log(op / (1.0f - op));

        // Convert linear scale to log-scale
        float s0 = std::log(std::max(1e-5f, scales[i * 3 + 0]));
        float s1 = std::log(std::max(1e-5f, scales[i * 3 + 1]));
        float s2 = std::log(std::max(1e-5f, scales[i * 3 + 2]));

        float r0 = rotations[i * 4 + 0];
        float r1 = rotations[i * 4 + 1];
        float r2 = rotations[i * 4 + 2];
        float r3 = rotations[i * 4 + 3];

        float record[17] = {
            x, y, z,
            nx, ny, nz,
            f_dc_0, f_dc_1, f_dc_2,
            logitOp,
            s0, s1, s2,
            r0, r1, r2, r3
        };
        out.write(reinterpret_cast<const char*>(record), sizeof(record));
    }

    out.close();
    return true;
}

bool GaussianSplatEngine::importFromPly(
    const std::string& filePath,
    std::vector<GaussianSplat>& outSplats
) {
    outSplats.clear();
    std::ifstream in(filePath, std::ios::binary);
    if (!in.is_open()) return false;

    std::string line;
    int vertexCount = 0;
    bool isBinary = false;
    std::vector<std::string> properties;

    while (std::getline(in, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line == "end_header") break;

        std::istringstream iss(line);
        std::string token;
        iss >> token;
        if (token == "format") {
            std::string fmt;
            iss >> fmt;
            if (fmt == "binary_little_endian") isBinary = true;
        } else if (token == "element") {
            std::string elem;
            iss >> elem;
            if (elem == "vertex") iss >> vertexCount;
        } else if (token == "property") {
            std::string type, name;
            iss >> type >> name;
            properties.push_back(name);
        }
    }

    if (vertexCount <= 0 || vertexCount > 2000000) return false;

    int idxX = -1, idxY = -1, idxZ = -1;
    int idxR = -1, idxG = -1, idxB = -1;
    int idxFdc0 = -1, idxFdc1 = -1, idxFdc2 = -1;
    int idxOp = -1;
    int idxS0 = -1, idxS1 = -1, idxS2 = -1;
    int idxR0 = -1, idxR1 = -1, idxR2 = -1, idxR3 = -1;

    for (size_t i = 0; i < properties.size(); ++i) {
        const auto& p = properties[i];
        if (p == "x") idxX = i;
        else if (p == "y") idxY = i;
        else if (p == "z") idxZ = i;
        else if (p == "red") idxR = i;
        else if (p == "green") idxG = i;
        else if (p == "blue") idxB = i;
        else if (p == "f_dc_0") idxFdc0 = i;
        else if (p == "f_dc_1") idxFdc1 = i;
        else if (p == "f_dc_2") idxFdc2 = i;
        else if (p == "opacity") idxOp = i;
        else if (p == "scale_0") idxS0 = i;
        else if (p == "scale_1") idxS1 = i;
        else if (p == "scale_2") idxS2 = i;
        else if (p == "rot_0") idxR0 = i;
        else if (p == "rot_1") idxR1 = i;
        else if (p == "rot_2") idxR2 = i;
        else if (p == "rot_3") idxR3 = i;
    }

    if (idxX < 0 || idxY < 0 || idxZ < 0) return false;

    outSplats.resize(vertexCount);
    const float C0 = 0.28209479177387814f;

    if (isBinary) {
        std::vector<float> row(properties.size());
        for (int i = 0; i < vertexCount; ++i) {
            in.read(reinterpret_cast<char*>(row.data()), properties.size() * sizeof(float));
            if (!in) break;

            GaussianSplat& s = outSplats[i];
            s.x = row[idxX];
            s.y = row[idxY];
            s.z = row[idxZ];

            if (idxFdc0 >= 0 && idxFdc1 >= 0 && idxFdc2 >= 0) {
                s.r = std::clamp(0.5f + C0 * row[idxFdc0], 0.0f, 1.0f);
                s.g = std::clamp(0.5f + C0 * row[idxFdc1], 0.0f, 1.0f);
                s.b = std::clamp(0.5f + C0 * row[idxFdc2], 0.0f, 1.0f);
            } else if (idxR >= 0 && idxG >= 0 && idxB >= 0) {
                s.r = row[idxR] > 1.0f ? row[idxR] / 255.0f : row[idxR];
                s.g = row[idxG] > 1.0f ? row[idxG] / 255.0f : row[idxG];
                s.b = row[idxB] > 1.0f ? row[idxB] / 255.0f : row[idxB];
            } else {
                s.r = 0.8f; s.g = 0.85f; s.b = 0.9f;
            }

            if (idxOp >= 0) {
                s.opacity = 1.0f / (1.0f + std::exp(-row[idxOp]));
            } else {
                s.opacity = 0.95f;
            }

            if (idxS0 >= 0 && idxS1 >= 0 && idxS2 >= 0) {
                s.sx = std::clamp(std::exp(row[idxS0]), 0.002f, 0.15f);
                s.sy = std::clamp(std::exp(row[idxS1]), 0.002f, 0.15f);
                s.sz = std::clamp(std::exp(row[idxS2]), 0.002f, 0.15f);
            } else {
                s.sx = 0.025f; s.sy = 0.025f; s.sz = 0.025f;
            }

            if (idxR0 >= 0 && idxR1 >= 0 && idxR2 >= 0 && idxR3 >= 0) {
                s.qw = row[idxR0]; s.qx = row[idxR1]; s.qy = row[idxR2]; s.qz = row[idxR3];
            } else {
                s.qw = 1.0f; s.qx = 0.0f; s.qy = 0.0f; s.qz = 0.0f;
            }
        }
    } else {
        std::vector<float> row(properties.size());
        for (int i = 0; i < vertexCount; ++i) {
            if (!std::getline(in, line)) break;
            std::istringstream iss(line);
            for (size_t p = 0; p < properties.size(); ++p) {
                iss >> row[p];
            }
            GaussianSplat& s = outSplats[i];
            s.x = row[idxX];
            s.y = row[idxY];
            s.z = row[idxZ];
            if (idxFdc0 >= 0 && idxFdc1 >= 0 && idxFdc2 >= 0) {
                s.r = std::clamp(0.5f + C0 * row[idxFdc0], 0.0f, 1.0f);
                s.g = std::clamp(0.5f + C0 * row[idxFdc1], 0.0f, 1.0f);
                s.b = std::clamp(0.5f + C0 * row[idxFdc2], 0.0f, 1.0f);
            } else if (idxR >= 0 && idxG >= 0 && idxB >= 0) {
                s.r = row[idxR] > 1.0f ? row[idxR] / 255.0f : row[idxR];
                s.g = row[idxG] > 1.0f ? row[idxG] / 255.0f : row[idxG];
                s.b = row[idxB] > 1.0f ? row[idxB] / 255.0f : row[idxB];
            } else {
                s.r = 0.8f; s.g = 0.85f; s.b = 0.9f;
            }
            s.opacity = idxOp >= 0 ? (1.0f / (1.0f + std::exp(-row[idxOp]))) : 0.95f;
            s.sx = idxS0 >= 0 ? std::clamp(std::exp(row[idxS0]), 0.002f, 0.15f) : 0.025f;
            s.sy = idxS1 >= 0 ? std::clamp(std::exp(row[idxS1]), 0.002f, 0.15f) : 0.025f;
            s.sz = idxS2 >= 0 ? std::clamp(std::exp(row[idxS2]), 0.002f, 0.15f) : 0.025f;
            s.qw = 1.0f; s.qx = 0.0f; s.qy = 0.0f; s.qz = 0.0f;
        }
    }

    return !outSplats.empty();
}
