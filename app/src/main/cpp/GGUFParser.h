#ifndef GGUF_PARSER_H
#define GGUF_PARSER_H

#include <string>
#include <vector>
#include <unordered_map>
#include <cstdint>

// GGUF Q4_K_M quantization block structure (llama.cpp compatible)
// Each block contains 256 weights quantized to 4-bit with scales and mins
struct BlockQ4_K {
    uint8_t scales[12];     // Quantization scales and mins packed
    uint8_t qs[128];        // 4-bit quantized weights (256 values)
};

struct GGUFTensorInfo {
    std::string name;
    std::vector<int64_t> dimensions;
    uint32_t type;          // e.g., GGML_TYPE_Q4_K = 12, GGML_TYPE_F32 = 0
    size_t offset;          // Byte offset relative to tensor data start in file
    size_t size_in_bytes;
    const void* data_ptr;   // Mapped pointer via mmap
};

class GGUFParser {
public:
    GGUFParser();
    ~GGUFParser();

    // Parses GGUF header, metadata, and tensor info dictionary from mmap buffer
    bool parse(const void* mapped_data, size_t file_size);

    // Retrieves tensor info by name
    const GGUFTensorInfo* getTensor(const std::string& name) const;

    // Dequantizes Q4_K block into float32 array
    static void dequantizeQ4K(const BlockQ4_K* block, float* output_f32);

    // Retrieves model hyper-parameters
    int getDimension(const std::string& key, int default_val) const;

private:
    bool parseHeader(const uint8_t*& ptr, const uint8_t* end);
    bool parseMetadata(const uint8_t*& ptr, const uint8_t* end);
    bool parseTensors(const uint8_t*& ptr, const uint8_t* end, const void* base_mapped);

    uint32_t version_;
    uint64_t tensor_count_;
    uint64_t metadata_kv_count_;
    
    std::unordered_map<std::string, GGUFTensorInfo> tensors_;
    std::unordered_map<std::string, std::string> metadata_str_;
    std::unordered_map<std::string, int32_t> metadata_int_;
};

#endif // GGUF_PARSER_H
