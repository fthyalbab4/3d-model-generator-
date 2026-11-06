#ifndef HARDWARE_ACCELERATOR_H
#define HARDWARE_ACCELERATOR_H

#include <vector>
#include <cstdint>

class HardwareAccelerator {
public:
    HardwareAccelerator();
    ~HardwareAccelerator();

    // ARM NEON optimized matrix-vector multiplication for CPU fallback / lightweight layers
    // Computes output = Matrix(M x N) * Vector(N) using ARM NEON intrinsics
    void matrixMultiplyNeon(const float* matrix, const float* vector, float* output, int M, int N);

    // Initialize Vulkan Compute context for GPU offloading (MediaTek Dimensity Mali GPU)
    bool initVulkanCompute();
    void cleanupVulkanCompute();

    // Execute heavy 3D tensor matrix multiplication on GPU via Vulkan Compute Shader
    bool dispatchVulkanMatrixMultiply(const float* A, const float* B, float* C, int M, int K, int N);

private:
    bool vulkan_initialized_;
    // Opaque Vulkan context handles
    void* vk_instance_;
    void* vk_device_;
    void* vk_pipeline_;
};

#endif // HARDWARE_ACCELERATOR_H
