package kz.cyber.acf.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.auth.dto.ForgotPasswordRequest;
import kz.cyber.acf.auth.dto.LoginRequest;
import kz.cyber.acf.auth.dto.RefreshRequest;
import kz.cyber.acf.auth.dto.RegisterRequest;
import kz.cyber.acf.auth.dto.SendSmsRequest;
import kz.cyber.acf.auth.dto.TokenResponse;
import kz.cyber.acf.auth.service.AuthService;
import kz.cyber.acf.core.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "SMS-based user registration. Send a code to the phone, then confirm it together with profile details to create an account.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Send SMS verification code",
            description = "Sends a one-time code to the given phone number. For testing purposes the code is always `1111`.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Code sent successfully")
            }
    )
    @PostMapping("/send-sms")
    public ResponseEntity<Void> sendSms(@RequestBody SendSmsRequest req) {
        authService.sendSms(req.getPhone());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Register a new user",
            description = "Verifies the SMS code and creates a user account. Returns the created user profile. Fails with `400` if the code is wrong.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid SMS code")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a user via Keycloak and returns access + refresh tokens.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed"),
                    @ApiResponse(responseCode = "401", description = "Refresh token expired or invalid")
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    @Operation(
            summary = "Logout",
            description = "Invalidates the user's refresh token in Keycloak.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logged out successfully")
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Send forgot-password SMS",
            description = "Sends a verification code to the phone number associated with an existing account. Code is always `1111` in test mode.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Code sent"),
                    @ApiResponse(responseCode = "404", description = "No account with that phone number")
            }
    )
    @PostMapping("/forgot-password/send-sms")
    public ResponseEntity<Void> sendForgotPasswordSms(@RequestBody SendSmsRequest req) {
        authService.sendForgotPasswordSms(req.getPhone());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Reset password",
            description = "Verifies the SMS code and sets a new password in Keycloak.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password reset successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid SMS code"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody ForgotPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok().build();
    }
}
