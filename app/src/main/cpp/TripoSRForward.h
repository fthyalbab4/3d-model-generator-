#ifndef TRIPOSR_FORWARD_H
#define TRIPOSR_FORWARD_H

#include "GGUFParser.h"
#include <vector>

class TripoSRForward {
public:
    TripoSRForward();
    ~TripoSRForward();

    // Initializes model layers from parsed GGUF parser
    bool initialize(const GGUFParser& parser);

    // Executes the complete TripoSR forward pass:
    // Input: Image features [B, 3, 256, 256] -> ViT Encoder -> Triplane Projection -> NeRF Query Decoder -> Output 3D Mesh/Radiance Field
    bool forward_pass(const float* input_image_tensor, std::vector<float>& output_vertices, std::vector<float>& output_colors);

private:
    // Transformer ViT Encoder layer execution
    void vitEncoderLayer(const std::vector<float>& tokens, std::vector<float>& output_tokens);

    // Triplane feature map projection from tokens
    void projectTriplanes(const std::vector<float>& tokens, std::vector<float>& triplane_xy, std::vector<float>& triplane_xz, std::vector<float>& triplane_yz);

    // Triplane NeRF query decoder step
    void triplaneNeRFDecoder(const std::vector<float>& t_xy, const std::vector<float>& t_xz, const std::vector<float>& t_yz, float x, float y, float z, float& density, float& r, float& g, float& b);

    const GGUFParser* parser_;
    bool initialized_;
};

#endif // TRIPOSR_FORWARD_H
