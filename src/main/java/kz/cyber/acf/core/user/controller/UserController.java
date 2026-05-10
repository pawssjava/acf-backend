package kz.cyber.acf.core.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.auth.dto.ChangePasswordRequest;
import kz.cyber.acf.auth.service.AuthService;
import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.core.user.dto.PlayerMatchDto;
import kz.cyber.acf.core.user.dto.PlayerTournamentHistoryDto;
import kz.cyber.acf.core.user.dto.UpdateUserRequest;
import kz.cyber.acf.core.user.dto.UserDto;
import kz.cyber.acf.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Users", description = "User profile management. New users are created via /api/auth/register.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.findByUsername(jwt.getClaimAsString("preferred_username"));
    }

    @Operation(summary = "List all users")
    @GetMapping
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    @Operation(summary = "Get user by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @GetMapping("/{id}")
    public UserDto findById(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(summary = "Update user profile",
            description = "Updates first name, last name, birth date, city and club.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @PutMapping("/{id}")
    public UserDto update(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestBody UpdateUserRequest req) {
        return userService.update(id, req);
    }

    @Operation(summary = "Upload profile photo",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Photo uploaded"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto uploadPhoto(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return userService.uploadPhoto(id, file);
    }

    @Operation(summary = "Upload verification document",
            description = "Accepts a PDF identity document. Sets isVerified=true on success.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Document uploaded, profile verified"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @PostMapping(value = "/{id}/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto uploadVerificationDocument(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return userService.uploadVerificationDocument(id, file);
    }

    @Operation(summary = "Get player's match history",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Match history returned"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @GetMapping("/{id}/matches")
    public PageResponse<PlayerMatchDto> getMatchHistory(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return userService.getMatchHistory(id, page, size);
    }

    @Operation(summary = "Get player's tournament history",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament history returned"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @GetMapping("/{id}/tournaments")
    public PageResponse<PlayerTournamentHistoryDto> getTournamentHistory(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return userService.getTournamentHistory(id, page, size);
    }

    @Operation(summary = "Change password",
            description = "Verifies the current password against Keycloak, then sets the new password.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password changed"),
                    @ApiResponse(responseCode = "401", description = "Current password is incorrect")
            })
    @PostMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "User ID") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChangePasswordRequest req) {
        authService.changePassword(
                jwt.getClaimAsString("preferred_username"),
                req.getCurrentPassword(),
                req.getNewPassword()
        );
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete user",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "User ID") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
