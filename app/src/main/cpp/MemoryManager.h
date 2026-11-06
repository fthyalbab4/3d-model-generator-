#ifndef MEMORY_MANAGER_H
#ifndef MEMORY_MANAGER_H
#define MEMORY_MANAGER_H

#include <stddef.h>
#include <sys/types.h>

class MemoryManager {
public:
    MemoryManager();
    ~MemoryManager();

    // Maps a model file (.bin or .gguf) into memory using zero-copy mmap()
    bool mapModel(const char* filePath);
    
    // Unmaps the file and cleans up resources
    void unmapModel();

    // Returns a pointer to the mapped memory weight buffer
    const void* getData() const;

    // Returns the total size of the mapped model file in bytes
    size_t getSize() const;

    // Checks if the memory mapping is currently active
    bool isMapped() const;

private:
    int fd_;
    void* mapped_data_;
    size_t file_size_;
    bool is_mapped_;

    // Prevent copying
    MemoryManager(const MemoryManager&) = delete;
    MemoryManager& operator=(const MemoryManager&) = delete;
};

#endif // MEMORY_MANAGER_H
#endif
