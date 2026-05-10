package kz.cyber.acf.core.partner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.partner.dto.PartnerDto;
import kz.cyber.acf.core.partner.dto.PartnerRequest;
import kz.cyber.acf.core.partner.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Partners", description = "Sponsors and partners of the platform. Viewing is public; write operations require admin.")
@RestController
@RequestMapping("/api/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @Operation(summary = "List all partners", description = "Returns all partners sorted by creation date descending.")
    @GetMapping
    public List<PartnerDto> findAll() {
        return partnerService.findAll();
    }

    @Operation(
            summary = "Get partner by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Partner found"),
                    @ApiResponse(responseCode = "404", description = "Partner not found")
            }
    )
    @GetMapping("/{id}")
    public PartnerDto findById(@Parameter(description = "Partner ID") @PathVariable Long id) {
        return partnerService.findById(id);
    }

    @Operation(
            summary = "Create partner (admin only)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Partner created"),
                    @ApiResponse(responseCode = "403", description = "Admin access required")
            }
    )
    @PostMapping
    public ResponseEntity<PartnerDto> create(@RequestBody PartnerRequest req, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnerService.create(jwt.getClaimAsString("preferred_username"), req));
    }

    @Operation(
            summary = "Update partner (admin only)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Partner updated"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Partner not found")
            }
    )
    @PutMapping("/{id}")
    public PartnerDto update(
            @Parameter(description = "Partner ID") @PathVariable Long id,
            @RequestBody PartnerRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return partnerService.update(jwt.getClaimAsString("preferred_username"), id, req);
    }

    @Operation(
            summary = "Upload partner logo (admin only)",
            description = "Uploads a logo image to MinIO and attaches it to the partner.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logo uploaded"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Partner not found")
            }
    )
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PartnerDto uploadLogo(
            @Parameter(description = "Partner ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        return partnerService.uploadLogo(jwt.getClaimAsString("preferred_username"), id, file);
    }

    @Operation(
            summary = "Delete partner (admin only)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Partner deleted"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Partner not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Partner ID") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        partnerService.delete(jwt.getClaimAsString("preferred_username"), id);
        return ResponseEntity.noContent().build();
    }
}
