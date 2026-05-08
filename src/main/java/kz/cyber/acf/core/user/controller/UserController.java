package kz.cyber.acf.core.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.user.dto.PageResponse;
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

    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user derived from the JWT token.")
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return userService.findByUsername(jwt.getClaimAsString("preferred_username"));
    }

    @Operation(summary = "List all users", description = "Returns every registered user.")
    @GetMapping
    public List<UserDto> findAll() {
        return userService.findAll();
    }

    @Operation(
            summary = "Get user by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/{id}")
    public UserDto findById(@Parameter(description = "User ID") @PathVariable Long id) {
        return userService.findById(id);
    }

    @Operation(
            summary = "Update user profile",
            description = "Updates first name, last name, birth date and photo. Username and phone cannot be changed here.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Profile updated"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PutMapping("/{id}")
    public UserDto update(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestBody UpdateUserRequest req) {
        return userService.update(id, req);
    }

    @Operation(
            summary = "Upload profile photo",
            description = "Uploads a photo to MinIO and saves the reference. Returns the updated user with a presigned photo URL.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Photo uploaded"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto uploadPhoto(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return userService.uploadPhoto(id, file);
    }

    @Operation(
            summary = "Get player's tournament history",
            description = "Returns finished tournaments the player participated in, ordered by end date descending. Includes the player's final place and score in each tournament.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament history returned"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @GetMapping("/{id}/tournaments")
    public PageResponse<PlayerTournamentHistoryDto> getTournamentHistory(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return userService.getTournamentHistory(id, page, size);
    }

    @Operation(
            summary = "Delete user",
            description = "Permanently removes a user account.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "User ID") @PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
