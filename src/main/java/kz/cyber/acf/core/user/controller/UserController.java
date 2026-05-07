package kz.cyber.acf.core.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.user.dto.UpdateUserRequest;
import kz.cyber.acf.core.user.dto.UserDto;
import kz.cyber.acf.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User profile management. New users are created via /api/auth/register.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
