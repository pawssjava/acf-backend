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

    @Operation(summary = "Upload a file to a folder inside the acf bucket")
    @PostMapping(value = "/{folder}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @PathVariable String folder,
            @RequestParam("file") MultipartFile file) {
        String objectName = minioService.upload(folder, file);
        return Map.of("objectName", objectName);
    }

    @Operation(summary = "Download a file by its full object name")
    @GetMapping("/{objectName}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String objectName) {
        var stream = minioService.download(objectName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stream));
    }

    @Operation(summary = "Delete a file by its full object name")
    @DeleteMapping("/{objectName}")
    public ResponseEntity<Void> delete(@PathVariable String objectName) {
        minioService.delete(objectName);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a presigned download URL (default 1h expiry)")
    @GetMapping("/{objectName}/url")
    public Map<String, String> presignedUrl(
            @PathVariable String objectName,
            @RequestParam(defaultValue = "1") int expiryHours) {
        String url = minioService.presignedUrl(objectName, expiryHours);
        return Map.of("url", url);
    }
}
