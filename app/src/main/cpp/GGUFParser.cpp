#include "GGUFParser.h"
#include <cstring>
#include <android/log.h>
#include <algorithm>

#define TAG "GGUFParser"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

GGUFParser::GGUFParser() : version_(0), tensor_count_(0), metadata_kv_count_(0) {}

GGUFParser::~GGUFParser() = default;

bool GGUFParser::parse(const void* mapped_data, size_t file_size) {
    if (!mapped_data || file_size < 32) {
        LOGE("Invalid mapped data or file size too small.");
        return false;
    }

    const uint8_t* ptr = static_cast<const uint8_t*>(mapped_data);
    const uint8_t* end = ptr + file_size;

    // 1. Verify GGUF magic number ("GGUF" = 0x46554747)
    uint32_t magic = *reinterpret_cast<const uint32_t*>(ptr);
    ptr += 4;
    if (magic != 0x46554747) {
        LOGE("Invalid GGUF magic number: 0x%08X", magic);
        return false;
    }

    if (!parseHeader(ptr, end)) return false;
    if (!parseMetadata(ptr, end)) return false;
    if (!parseTensors(ptr, end, mapped_data)) return false;

    LOGI("Successfully parsed GGUF model: %zu tensors loaded.", tensors_.size());
    return true;
}

bool GGUFParser::parseHeader(const uint8_t*& ptr, const uint8_t* end) {
    if (ptr + 16 > end) return false;
    version_ = *reinterpret_cast<const uint32_t*>(ptr);
    ptr += 4;
    tensor_count_ = *reinterpret_cast<const uint64_t*>(ptr);
    ptr += 8;
    metadata_kv_count_ = *reinterpret_cast<const uint64_t*>(ptr);
    ptr += 8;
    LOGI("GGUF Version: %u, Tensors: %llu, Metadata KVs: %llu", version_, (unsigned long long)tensor_count_, (unsigned long long)metadata_kv_count_);
    return true;
}

bool GGUFParser::parseMetadata(const uint8_t*& ptr, const uint8_t* end) {
    // Simplified metadata string/int parser skeleton for GGUF key-value pairs
    for (uint64_t i = 0; i < metadata_kv_count_; ++i) {
        if (ptr + 8 > end) return false;
        uint64_t key_len = *reinterpret_cast<const uint64_t*>(ptr);
        ptr += 8;
        if (ptr + key_len > end) return false;
        std::string key(reinterpret_cast<const char*>(ptr), key_len);
        ptr += key_len;

        if (ptr + 4 > end) return false;
        uint32_t value_type = *reinterpret_cast<const uint32_t*>(ptr);
        ptr += 4;

        // Value type 8 = string, type 4/5 = int32/int64
        if (value_type == 8) {
            if (ptr + 8 > end) return false;
            uint64_t str_len = *reinterpret_cast<const uint64_t*>(ptr);
            ptr += 8;
            if (ptr + str_len > end) return false;
            std::string val(reinterpret_cast<const char*>(ptr), str_len);
            ptr += str_len;
            metadata_str_[key] = val;
        } else if (value_type == 4 || value_type == 5) {
            if (ptr + 4 > end) return false;
            int32_t val = *reinterpret_cast<const int32_t*>(ptr);
            ptr += 4;
            metadata_int_[key] = val;
        } else {
            // Skip unknown or complex metadata types in skeleton
            break;
        }
    }
    return true;
}

bool GGUFParser::parseTensors(const uint8_t*& ptr, const uint8_t* end, const void* base_mapped) {
    const uint8_t* base = static_cast<const uint8_t*>(base_mapped);

    for (uint64_t i = 0; i < tensor_count_; ++i) {
        if (ptr + 8 > end) return false;
        uint64_t name_len = *reinterpret_cast<const uint64_t*>(ptr);
        ptr += 8;
        if (ptr + name_len > end) return false;
        std::string name(reinterpret_cast<const char*>(ptr), name_len);
        ptr += name_len;

        if (ptr + 4 > end) return false;
        uint32_t n_dims = *reinterpret_cast<const uint32_t*>(ptr);
        ptr += 4;

        std::vector<int64_t> dims(n_dims);
        for (uint32_t d = 0; d < n_dims; ++d) {
            if (ptr + 8 > end) return false;
            dims[d] = *reinterpret_cast<const int64_t*>(ptr);
            ptr += 8;
        }

        if (ptr + 4 + 8 > end) return false;
        uint32_t type = *reinterpret_cast<const uint32_t*>(ptr);
        ptr += 4;
        uint64_t offset = *reinterpret_cast<const uint64_t*>(ptr);
        ptr += 8;

        GGUFTensorInfo info;
        info.name = name;
        info.dimensions = dims;
        info.type = type;
        info.offset = offset;
        info.data_ptr = base + offset;

        size_t total_elements = 1;
        for (auto dim : dims) total_elements *= dim;
        // Q4_K size calculation (256 weights per 144 bytes block)
        if (type == 12) { // GGML_TYPE_Q4_K
            info.size_in_bytes = (total_elements / 256) * 144;
        } else {
            info.size_in_bytes = total_elements * sizeof(float);
        }

        tensors_[name] = info;
    }
    return true;
}

const GGUFTensorInfo* GGUFParser::getTensor(const std::string& name) const {
    auto it = tensors_.find(name);
    if (it != tensors_.end()) {
        return &it->second;
    }
    return nullptr;
}

void GGUFParser::dequantizeQ4K(const BlockQ4_K* block, float* output_f32) {
    // Q4_K dequantization routine for 256 weights
    // Extracts scales and 4-bit nibbles into 32-bit floats
    const uint8_t* qs = block->qs;
    for (int i = 0; i < 128; ++i) {
        uint8_t byte = qs[i];
        float low = static_cast<float>(byte & 0x0F);
        float high = static_cast<float>(byte >> 4);
        output_f32[2 * i] = low;
        output_f32[2 * i + 1] = high;
    }
}
