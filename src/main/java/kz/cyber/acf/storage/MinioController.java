package kz.cyber.acf.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Storage", description = "File upload/download via MinIO")
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class MinioController {

    private final MinioService minioService;

    @Operation(summary = "Upload a file to a bucket")
    @PostMapping(value = "/{bucket}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @PathVariable String bucket,
            @RequestParam("file") MultipartFile file) {
        String objectName = minioService.upload(bucket, file);
        return Map.of("objectName", objectName);
    }

    @Operation(summary = "Download a file from a bucket")
    @GetMapping("/{bucket}/{objectName}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String bucket,
            @PathVariable String objectName) {
        var stream = minioService.download(bucket, objectName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stream));
    }

    @Operation(summary = "Delete a file from a bucket")
    @DeleteMapping("/{bucket}/{objectName}")
    public ResponseEntity<Void> delete(
            @PathVariable String bucket,
            @PathVariable String objectName) {
        minioService.delete(bucket, objectName);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a presigned download URL (default 1h expiry)")
    @GetMapping("/{bucket}/{objectName}/url")
    public Map<String, String> presignedUrl(
            @PathVariable String bucket,
            @PathVariable String objectName,
            @RequestParam(defaultValue = "1") int expiryHours) {
        String url = minioService.presignedUrl(bucket, objectName, expiryHours);
        return Map.of("url", url);
    }
}
