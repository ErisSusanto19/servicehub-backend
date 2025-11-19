package com.eris.servicehub.services.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CloudinaryStorageService implements StorageService{

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadFile(MultipartFile file, String folderName){
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
