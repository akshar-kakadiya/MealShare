package com.mealshare.evidence;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/evidence")
@Tag(name = "Evidence", description = "Endpoints for uploading and retrieving real pickup/delivery photo evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload evidence photo (multipart)")
    public ResponseEntity<Map<String, String>> uploadMultipart(@RequestParam("file") MultipartFile file) {
        String url = evidenceService.storeFile(file);
        return ResponseEntity.ok(Map.of("photoUrl", url));
    }

    @PostMapping("/upload-base64")
    @Operation(summary = "Upload evidence photo (Base64)")
    public ResponseEntity<Map<String, String>> uploadBase64(@RequestBody Map<String, String> payload) {
        String base64Data = payload.get("photoBase64");
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException("Field 'photoBase64' is required.");
        }
        String url = evidenceService.storeBase64Image(base64Data);
        return ResponseEntity.ok(Map.of("photoUrl", url));
    }

    @GetMapping("/{filename:.+}")
    @Operation(summary = "Retrieve evidence photo")
    public ResponseEntity<Resource> getEvidencePhoto(@PathVariable String filename) {
        try {
            Path file = evidenceService.loadFileAsPath(filename);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = "image/jpeg";
            if (filename.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (filename.toLowerCase().endsWith(".webp")) {
                contentType = "image/webp";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
