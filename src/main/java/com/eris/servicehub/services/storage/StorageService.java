package com.eris.servicehub.services.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadFile(MultipartFile file, String folderName);
    void deleteFile(String fileUrl);
}
