# Mobile 3D Gaussian Splatting Studio (Idol-SDK Architecture for Android)

A mobile-native 3D Gaussian Splatting (`.ply`) generation and real-time rendering studio inspired by `speridlabs/idol-sdk`, engineered specifically for mobile processors (ARM64 / MediaTek Dimensity) using lightweight on-device models, C++ NDK acceleration, and a real-time OpenGL ES 3.0 Gaussian Splatting renderer.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> `speridlabs/idol-sdk` converts a single 2D image into a **3D Gaussian Splat (`.ply`)** by removing the background, framing the subject, and predicting 3D Gaussians (position, scale, rotation, opacity, and color). Because the desktop version requires 7.2 GB VRAM (DINOv3 + FLUX.2-dev), this mobile implementation adapts the exact **Single-Image-to-3D-Gaussian-Splat (`.ply`)** pipeline to run natively on Android:
> 1. **Lightweight On-Device Inference**: Foreground segmentation + lightweight monocular depth & normal estimation (ONNX Runtime / NDK) to synthesize multi-layer 3D Gaussians (front, mid-volume, and inferred back-side splats) rather than a flat plane or sphere.
> 2. **Real-Time 3D Gaussian Splatting Renderer**: Custom OpenGL ES 3.0 instanced/point-sprite Gaussian Splatting shader pipeline with alpha-blended elliptical splats, depth sorting, and `.ply` file import/export.

- **Confirmed Decision 1 (Execution Target)**: Mobile-compatible lightweight on-device pipeline (no 7.2 GB desktop GPU required).
- **Confirmed Decision 2 (3D Format)**: Real-time 3D Gaussian Splatting (`.ply`) renderer and exporter.

---

## 1. Overview & Core Concept

- **What It Does**: Takes any single 2D photo or character/object image, automatically isolates the foreground subject, estimates per-pixel depth and surface normals, and synthesizes a dense **3D Gaussian Splat cloud** (`.ply` format) with position `(x, y, z)`, 3D scale `(sx, sy, sz)`, quaternion rotation, opacity, and color. Users can orbit, zoom, pan, inspect splat density, and export/import standard `.ply` files.
- **Target Audience**: Mobile 3D creators, game developers, and AR/3D enthusiasts who want fast, offline 3D Gaussian Splatting on their phone.
- **Key Value**: Delivers the `idol-sdk` workflow—single image to 3D Gaussian Splat (`.ply`)—running locally on mobile hardware in seconds.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Image Input & Background Removal**:
   - Pick an image from Gallery, capture via Camera, or select a built-in studio sample.
   - Automatic foreground segmentation & framing (isolating the subject from the background, just like `idol-sdk`).
2. **3D Gaussian Splat Synthesis**:
   - Lightweight on-device depth + normal inference generates thousands of 3D Gaussians (front surface, volumetric thickness, and back-side completion).
   - Progress HUD shows splat count (e.g., `65,536 Gaussians`), inference time (ms), and memory usage.
3. **Real-Time 3D Gaussian Splatting Viewport**:
   - Smooth 60 FPS OpenGL ES 3.0 `GLSurfaceView` rendering elliptical, alpha-blended 3D Gaussians with depth-sorted back-to-front compositing.
   - Touch gestures: 1-finger orbit, pinch-to-zoom, 2-finger pan, plus splat size/opacity controls.
4. **`.PLY` Export & Import**:
   - Export generated 3D Gaussian Splats to standard `.ply` format (compatible with Luma, Polycam, SuperSplat, and Blender).
   - Import external `.ply` Gaussian Splat files directly into the mobile viewer.

### Visual Identity & Theme
- **Aesthetic Direction**: Dark technical 3D studio (`#0F1117` obsidian canvas, `#161922` elevated panels, `#00E5FF` electric cyan accents, `#10B981` emerald status).
- **Typography**: Monospace telemetry readouts for Gaussian count, FPS, and `.ply` file size; clean sans-serif controls.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: OpenGL ES 3.0 Instanced Gaussian Splatting Renderer**
  - *Chosen Approach*: Use `GLSurfaceView` with OpenGL ES 3.0 instanced quad/point-sprite shaders that project 3D covariance/scale into 2D screen-space ellipses with Gaussian falloff (`exp(-0.5 * dot(d, d))`).
  - *Why*: CPU Canvas drawing cannot render tens of thousands of alpha-blended 3D Gaussians at interactive frame rates; OpenGL ES 3.0 runs natively on all Android GPUs (Mali, Adreno).
- **Decision 2: C++ NDK Depth-Sorted Splat Buffer & `.PLY` Parser/Exporter**
  - *Chosen Approach*: Perform fast radix/quick depth sorting of 3D Gaussians and binary/ASCII `.ply` serialization in C++ NDK.
  - *Why*: Sorting 50,000+ Gaussians per frame in Kotlin/JVM causes GC pauses, whereas C++ NDK with `-O3` and NEON SIMD sorts in a few milliseconds.
- **Decision 3: Lightweight Mobile Splat Synthesis**
  - *Chosen Approach*: Combine foreground alpha matting, multi-scale depth/normal estimation, and bilateral back-face Gaussian synthesis to produce a full 360° 3D Gaussian Splat cloud.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   StudioViewModel                      │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
        [2D Image Input]           [.PLY File Import]
               │                          │
               ▼                          ▼
┌───────────────────────────────┐  ┌─────────────────────┐
│ Mobile Gaussian Splat Engine  │  │  C++ NDK .PLY I/O   │
│ - Foreground Matting & Crop   │  │ - Binary/ASCII .ply │
│ - Lightweight Depth & Normals │  │   Parser & Writer   │
│ - 360° Gaussian Synthesizer   │  └──────────┬──────────┘
└──────────────┬────────────────┘             │
               │                              │
               └──────────────┬───────────────┘
                              ▼
               ┌───────────────────────────────┐
               │   GaussianSplatCloud (NDK)    │
               │ - Positions, Scales, Colors,  │
               │   Opacities, Quaternions      │
               └──────────────┬────────────────┘
                              ▼
               ┌───────────────────────────────┐
               │  OpenGL ES 3.0 Splat Viewer   │
               │ - C++ NDK Depth Sorter        │
               │ - Elliptical Gaussian Shader  │
               │ - 360° Orbit / Zoom / Pan     │
               └───────────────────────────────┘
```

### Key Components to Build:
1. **C++ NDK Gaussian Splat Core (`GaussianSplatEngine.h/.cpp`)**:
   - Data structures for 3D Gaussians (`x, y, z, sx, sy, sz, qw, qx, qy, qz, r, g, b, opacity`).
   - Fast C++ depth-sorting of splats based on camera view matrix.
   - Standard `.ply` binary/ASCII reader and writer.
2. **Mobile Splat Synthesizer (`MobileGaussianGenerator.kt`)**:
   - Foreground segmentation, depth/normal estimation, and 360° 3D Gaussian Splat generation.
3. **OpenGL ES 3.0 Gaussian Splatting Renderer (`GaussianSplatRenderer.kt` & `GaussianViewport.kt`)**:
   - Custom vertex & fragment shaders rendering smooth, alpha-blended 3D Gaussians in real time.
4. **Studio UI & `.PLY` Export/Import (`StudioScreen.kt`, `StudioViewModel.kt`)**:
   - Seamless integration with camera, gallery, sample presets, splat size/opacity sliders, and `.ply` export/import.
