package kz.cyber.acf.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.auth.dto.RegisterRequest;
import kz.cyber.acf.auth.dto.SendSmsRequest;
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
}
