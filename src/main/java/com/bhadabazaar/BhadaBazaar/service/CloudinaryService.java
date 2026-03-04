package com.bhadabazaar.BhadaBazaar.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private static final Set<String> ALLOWED_EXT =
        Set.of("jpg", "jpeg", "png", "webp", "avif");

    public String uploadFile(MultipartFile file) {
        validateFileIsImage(file);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }
    
    public Map<String, String> uploadFileWithPublicId(MultipartFile file) {
        validateFileIsImage(file);
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return Map.of(
                "url", (String) uploadResult.get("secure_url"),
                "public_id", (String) uploadResult.get("public_id")
            );
        } catch (IOException e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    private void validateFileIsImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        String contentType = file.getContentType();

        if (file.isEmpty()) {
        throw new IllegalArgumentException("File cannot be empty");
    }

    if (file.getSize() > 5 * 1024 * 1024) {
        throw new IllegalArgumentException("File too large");
    }

    String name = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
    String ext = name.substring(name.lastIndexOf('.') + 1);

    if (!ALLOWED_EXT.contains(ext)) {
        throw new IllegalArgumentException("Unsupported format");
    }
    
    if (contentType == null || !contentType.startsWith("image/")) {
        throw new IllegalArgumentException("file should only be image");
    }
    }
    
    public boolean deleteFile(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            Object status = result.get("result");
            return status != null && "ok".equals(status.toString());
        } catch (Exception e) {
            return false;
        }
    }
}
