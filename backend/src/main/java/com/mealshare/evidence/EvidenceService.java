package com.mealshare.evidence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

@Service
public class EvidenceService {

    private final Path storageDirectory;

    public EvidenceService(@Value("${mealshare.evidence.storage-dir:uploads/evidence}") String storageDir) {
        this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + this.storageDirectory, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Evidence photo file must not be empty.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + extension;
        Path targetLocation = this.storageDirectory.resolve(filename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store evidence photo: " + filename, e);
        }

        return "/api/v1/evidence/" + filename;
    }

    public String storeBase64Image(String base64Data) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException("Base64 photo data must not be empty.");
        }

        String rawData = base64Data;
        String extension = ".jpg";
        if (rawData.contains(",")) {
            String header = rawData.substring(0, rawData.indexOf(","));
            if (header.contains("png")) extension = ".png";
            else if (header.contains("webp")) extension = ".webp";
            rawData = rawData.substring(rawData.indexOf(",") + 1);
        }

        byte[] decodedBytes;
        try {
            decodedBytes = Base64.getDecoder().decode(rawData.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 image encoding.");
        }

        String filename = UUID.randomUUID() + extension;
        Path targetLocation = this.storageDirectory.resolve(filename);

        try {
            Files.write(targetLocation, decodedBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store decoded evidence photo: " + filename, e);
        }

        return "/api/v1/evidence/" + filename;
    }

    public Path loadFileAsPath(String filename) {
        Path filePath = this.storageDirectory.resolve(filename).normalize();
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new IllegalArgumentException("Evidence file not found: " + filename);
        }
        return filePath;
    }
}
