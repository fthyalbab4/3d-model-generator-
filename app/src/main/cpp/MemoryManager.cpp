#include "MemoryManager.h"
#include <fcntl.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <unistd.h>
#include <android/log.h>

#define TAG "MemoryManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

MemoryManager::MemoryManager() 
    : fd_(-1), mapped_data_(MAP_FAILED), file_size_(0), is_mapped_(false) {}

MemoryManager::~MemoryManager() {
    unmapModel();
}

bool MemoryManager::mapModel(const char* filePath) {
    if (is_mapped_) {
        unmapModel();
    }

    // Open file in read-only mode
    fd_ = open(filePath, O_RDONLY);
    if (fd_ == -1) {
        LOGE("Failed to open model file at %s", filePath);
        return false;
    }

    // Retrieve file size using fstat
    struct stat sb;
    if (fstat(fd_, &sb) == -1) {
        LOGE("Failed to get file stats for %s", filePath);
        close(fd_);
        fd_ = -1;
        return false;
    }
    file_size_ = static_cast<size_t>(sb.st_size);

    // Perform zero-copy mmap() mapping
    // MAP_SHARED or MAP_PRIVATE with PROT_READ ensures zero-copy kernel-space page caching
    mapped_data_ = mmap(nullptr, file_size_, PROT_READ, MAP_PRIVATE, fd_, 0);
    if (mapped_data_ == MAP_FAILED) {
        LOGE("mmap failed for file size %zu bytes", file_size_);
        close(fd_);
        fd_ = -1;
        mapped_data_ = nullptr;
        return false;
    }

    // Advise kernel on sequential memory access pattern for optimization
    madvise(mapped_data_, file_size_, MADV_SEQUENTIAL);

    is_mapped_ = true;
    LOGI("Successfully mapped model %s (%zu MB) via mmap()", filePath, file_size_ / (1024 * 1024));
    return true;
}

void MemoryManager::unmapModel() {
    if (!is_mapped_) {
        return;
    }

    if (mapped_data_ != MAP_FAILED && mapped_data_ != nullptr) {
        munmap(mapped_data_, file_size_);
        mapped_data_ = nullptr;
    }

    if (fd_ != -1) {
        close(fd_);
        fd_ = -1;
    }

    file_size_ = 0;
    is_mapped_ = false;
    LOGI("Unmapped model and released file descriptors.");
}

const void* MemoryManager::getData() const {
    return is_mapped_ ? mapped_data_ : nullptr;
}

size_t MemoryManager::getSize() const {
    return file_size_;
}

bool MemoryManager::isMapped() const {
    return is_mapped_;
}
