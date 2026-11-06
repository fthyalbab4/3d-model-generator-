#include <jni.h>
#include <string>
#include <fstream>
#include <android/log.h>
#include "MemoryManager.h"
#include "HardwareAccelerator.h"
#include "GGUFParser.h"
#include "TripoSRForward.h"
#include "GaussianSplatEngine.h"

#define TAG "NativeInferenceEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global engine state instances
static MemoryManager g_memory_manager;
static HardwareAccelerator g_hardware_accelerator;
static GGUFParser g_gguf_parser;
static TripoSRForward g_triposr_forward;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_engine_AiInferenceEngine_nativeLoadModel(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath) {
    
    const char* path_chars = env->GetStringUTFChars(modelPath, nullptr);
    if (path_chars == nullptr) {
        LOGE("Failed to get model path string from Java.");
        return JNI_FALSE;
    }

    LOGI("Native loadModel invoked with path: %s", path_chars);

    // Initialize Vulkan acceleration
    g_hardware_accelerator.initVulkanCompute();

    // Zero-copy mmap() model weights loading
    bool success = g_memory_manager.mapModel(path_chars);
    if (success) {
        // Parse GGUF Q4_K_M metadata and tensor dictionary from mmap buffer
        bool parsed = g_gguf_parser.parse(g_memory_manager.getData(), g_memory_manager.getSize());
        if (parsed) {
            g_triposr_forward.initialize(g_gguf_parser);
            LOGI("TripoSR GGUF Q4_K_M model successfully parsed and initialized.");
        } else {
            LOGE("Failed to parse GGUF structure from mapped memory.");
        }
    }

    env->ReleaseStringUTFChars(modelPath, path_chars);

    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_example_engine_AiInferenceEngine_nativeGenerate3D(
    JNIEnv* env,
    jobject thiz,
    jfloatArray inputData,
    jstring outputObjPath) {

    if (!g_memory_manager.isMapped()) {
        LOGE("Cannot generate 3D object: Model weights not mapped.");
        return JNI_FALSE;
    }

    jsize input_len = env->GetArrayLength(inputData);
    jfloat* input_body = env->GetFloatArrayElements(inputData, nullptr);

    const char* out_path_chars = env->GetStringUTFChars(outputObjPath, nullptr);

    LOGI("Executing TripoSR forward pass for 3D object generation (input len=%d)...", input_len);

    std::vector<float> input_tensor(input_body, input_body + input_len);
    std::vector<float> output_vertices;
    std::vector<float> output_colors;

    // Execute core TripoSR forward pass graph (ViT -> Triplane -> NeRF query decoder)
    bool success = g_triposr_forward.forward_pass(input_tensor.data(), output_vertices, output_colors);

    if (success && !output_vertices.empty()) {
        // Serialize generated 3D mesh to Wavefront OBJ format at out_path_chars
        std::ofstream obj_file(out_path_chars);
        if (obj_file.is_open()) {
            obj_file << "# TripoSR Local 3D Generation Engine (Q4_K_M GGUF)\n";
            for (size_t i = 0; i < output_vertices.size(); i += 3) {
                obj_file << "v " << output_vertices[i] << " " << output_vertices[i+1] << " " << output_vertices[i+2] << "\n";
            }
            obj_file.close();
            LOGI("Successfully exported 3D OBJ file to %s (%zu vertices)", out_path_chars, output_vertices.size() / 3);
        } else {
            LOGE("Failed to open output OBJ file for writing at %s", out_path_chars);
            success = false;
        }
    } else {
        LOGE("TripoSR forward pass generated empty vertices or failed.");
        success = false;
    }

    // Clean up JNI arrays
    env->ReleaseFloatArrayElements(inputData, input_body, JNI_ABORT);
    env->ReleaseStringUTFChars(outputObjPath, out_path_chars);

    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_example_engine_GaussianSplatNative_nativeSortGaussians(
    JNIEnv* env,
    jobject thiz,
    jfloatArray positions,
    jint count,
    jfloatArray viewMatrix,
    jintArray outSortedIndices) {

    jfloat* posPtr = env->GetFloatArrayElements(positions, nullptr);
    jfloat* matPtr = env->GetFloatArrayElements(viewMatrix, nullptr);
    jint* idxPtr = env->GetIntArrayElements(outSortedIndices, nullptr);

    if (posPtr && matPtr && idxPtr) {
        GaussianSplatEngine::sortGaussiansByDepth(posPtr, count, matPtr, idxPtr);
    }

    if (posPtr) env->ReleaseFloatArrayElements(positions, posPtr, JNI_ABORT);
    if (matPtr) env->ReleaseFloatArrayElements(viewMatrix, matPtr, JNI_ABORT);
    if (idxPtr) env->ReleaseIntArrayElements(outSortedIndices, idxPtr, 0);
}

JNIEXPORT jboolean JNICALL
Java_com_example_engine_GaussianSplatNative_nativeExportPly(
    JNIEnv* env,
    jobject thiz,
    jstring filePath,
    jfloatArray positions,
    jfloatArray scales,
    jfloatArray rotations,
    jfloatArray colors,
    jfloatArray opacities,
    jint count) {

    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return JNI_FALSE;

    jfloat* posPtr = env->GetFloatArrayElements(positions, nullptr);
    jfloat* scalePtr = env->GetFloatArrayElements(scales, nullptr);
    jfloat* rotPtr = env->GetFloatArrayElements(rotations, nullptr);
    jfloat* colPtr = env->GetFloatArrayElements(colors, nullptr);
    jfloat* opPtr = env->GetFloatArrayElements(opacities, nullptr);

    bool ok = false;
    if (posPtr && scalePtr && rotPtr && colPtr && opPtr) {
        ok = GaussianSplatEngine::exportToPly(
            pathChars, posPtr, scalePtr, rotPtr, colPtr, opPtr, count
        );
    }

    if (posPtr) env->ReleaseFloatArrayElements(positions, posPtr, JNI_ABORT);
    if (scalePtr) env->ReleaseFloatArrayElements(scales, scalePtr, JNI_ABORT);
    if (rotPtr) env->ReleaseFloatArrayElements(rotations, rotPtr, JNI_ABORT);
    if (colPtr) env->ReleaseFloatArrayElements(colors, colPtr, JNI_ABORT);
    if (opPtr) env->ReleaseFloatArrayElements(opacities, opPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(filePath, pathChars);

    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloatArray JNICALL
Java_com_example_engine_GaussianSplatNative_nativeImportPly(
    JNIEnv* env,
    jobject thiz,
    jstring filePath) {

    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return nullptr;

    std::vector<GaussianSplat> splats;
    bool ok = GaussianSplatEngine::importFromPly(pathChars, splats);
    env->ReleaseStringUTFChars(filePath, pathChars);

    if (!ok || splats.empty()) return nullptr;

    // Pack 14 floats per splat: x,y,z, sx,sy,sz, qw,qx,qy,qz, r,g,b, opacity
    const int stride = 14;
    jfloatArray result = env->NewFloatArray(static_cast<jsize>(splats.size() * stride));
    if (!result) return nullptr;

    std::vector<float> packed(splats.size() * stride);
    for (size_t i = 0; i < splats.size(); ++i) {
        const auto& s = splats[i];
        size_t base = i * stride;
        packed[base + 0] = s.x;
        packed[base + 1] = s.y;
        packed[base + 2] = s.z;
        packed[base + 3] = s.sx;
        packed[base + 4] = s.sy;
        packed[base + 5] = s.sz;
        packed[base + 6] = s.qw;
        packed[base + 7] = s.qx;
        packed[base + 8] = s.qy;
        packed[base + 9] = s.qz;
        packed[base + 10] = s.r;
        packed[base + 11] = s.g;
        packed[base + 12] = s.b;
        packed[base + 13] = s.opacity;
    }

    env->SetFloatArrayRegion(result, 0, static_cast<jsize>(packed.size()), packed.data());
    return result;
}

} // extern "C"
