package kz.cyber.acf.core.education.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.education.dto.EducationMaterialDto;
import kz.cyber.acf.core.education.dto.EducationMaterialRequest;
import kz.cyber.acf.core.education.service.EducationMaterialService;
import kz.cyber.acf.core.user.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Education", description = "Video lessons and presentations for the education platform.")
@RestController
@RequestMapping("/api/education")
@RequiredArgsConstructor
public class EducationMaterialController {

    private final EducationMaterialService service;

    @Operation(summary = "List education materials", description = "Paginated list, optionally filtered by category.")
    @GetMapping
    public PageResponse<EducationMaterialDto> findAll(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.findAll(category, page, size);
    }

    @Operation(summary = "Get education material by ID")
    @GetMapping("/{id}")
    public EducationMaterialDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @Operation(summary = "Create education material (admin only)")
    @PostMapping
    public ResponseEntity<EducationMaterialDto> create(
            @RequestBody EducationMaterialRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(jwt.getClaimAsString("preferred_username"), req));
    }

    @Operation(summary = "Update education material metadata (admin only)")
    @PutMapping("/{id}")
    public EducationMaterialDto update(
            @PathVariable Long id,
            @RequestBody EducationMaterialRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return service.update(jwt.getClaimAsString("preferred_username"), id, req);
    }

    @Operation(summary = "Upload video file (admin only)")
    @PostMapping(value = "/{id}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EducationMaterialDto uploadVideo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return service.uploadVideo(jwt.getClaimAsString("preferred_username"), id, file);
    }

    @Operation(summary = "Upload presentation PPTX/PDF (admin only)")
    @PostMapping(value = "/{id}/presentation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EducationMaterialDto uploadPresentation(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return service.uploadPresentation(jwt.getClaimAsString("preferred_username"), id, file);
    }

    @Operation(summary = "Upload thumbnail image (admin only)")
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EducationMaterialDto uploadThumbnail(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return service.uploadThumbnail(jwt.getClaimAsString("preferred_username"), id, file);
    }

    @Operation(summary = "Delete education material (admin only)", responses = @ApiResponse(responseCode = "204"))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        service.delete(jwt.getClaimAsString("preferred_username"), id);
        return ResponseEntity.noContent().build();
    }
}
