package com.eris.servicehub.services.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.eris.servicehub.exceptions.FileValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CloudinaryStorageService implements StorageService{

    @Autowired
    private Cloudinary cloudinary;

    private final long maxSizeInBytes;
    private final List<String> allowedMimeTypes;

    public CloudinaryStorageService(
            @Value("${file.max-size-mb}") long maxSizeInMb,
            @Value("${file.allowed-types}") String allowedTypes
    ) {
        this.maxSizeInBytes = maxSizeInMb * 1024 * 1024;
        this.allowedMimeTypes = List.of(allowedTypes.split(","));
    }

    @Override
    public String uploadFile(MultipartFile file, String folderName){
        validateFile(file);

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "auto"
            ));

            return (String) uploadResult.get("secure_url");

        } catch (IOException e){
            throw new RuntimeException("Could not upload file to Cloudinary", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileValidationException("File cannot be empty.");
        }

        if (file.getSize() > this.maxSizeInBytes) {
            throw new FileValidationException("File size exceeds the limit of " + (this.maxSizeInBytes / (1024 * 1024)) + " MB.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !this.allowedMimeTypes.contains(mimeType)) {
            throw new FileValidationException("Invalid file type. Only " + this.allowedMimeTypes + " are allowed.");
        }
    }

    @Override
    public void deleteFile(String fileUrl){
        if(fileUrl == null || fileUrl.isBlank()){
            return;
        }

        try {
            String publicId = extractPublicIdFromUrl(fileUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e){
            throw  new RuntimeException("Failed to delete file from Cloudinary: " + fileUrl);
        }
    }

    private String extractPublicIdFromUrl(String url){
        // Regex to extract the public ID from a Cloudinary URL
        // Example: https://res.cloudinary.com/cloud_name/image/upload/v12345/folder/public_id.jpg
        // The public ID is "folder/public_id"

        Pattern pattern = Pattern.compile("/v\\d+/(.+)");
        Matcher matcher = pattern.matcher(url);

        if(matcher.find()){
            String publicIdWithExtension = matcher.group(1);
            int lastDotIndex = publicIdWithExtension.lastIndexOf('.');
            if(lastDotIndex != -1){
                return publicIdWithExtension.substring(0, lastDotIndex);
            }

            return publicIdWithExtension;
        }

        throw new IllegalArgumentException("Invalid Cloudinary URL format");
    }
}
