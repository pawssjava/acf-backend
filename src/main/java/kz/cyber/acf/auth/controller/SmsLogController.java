package kz.cyber.acf.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.auth.dto.SmsLogDto;
import kz.cyber.acf.auth.service.SmsLogService;
import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SMS Log", description = "Admin-only dashboard: paginated SMS code audit log with filters.")
@RestController
@RequestMapping("/api/admin/sms-log")
@RequiredArgsConstructor
public class SmsLogController {

    private final SmsLogService smsLogService;
    private final UserService   userService;

    @Operation(
            summary = "Get SMS log",
            description = """
                    Returns a paginated list of all SMS codes that were sent.
                    Filterable by phone number (partial match) and usage status.
                    Admin access only.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Log returned"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin")
            }
    )
    @GetMapping
    public PageResponse<SmsLogDto> getLogs(
            @Parameter(description = "Filter by phone number (partial match)") @RequestParam(required = false) String phone,
            @Parameter(description = "true = show only used codes, false = show only unused, omit = show all") @RequestParam(required = false) Boolean used,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        userService.requireAdmin(jwt.getClaimAsString("preferred_username"));
        return smsLogService.getLogs(phone, used, page, size);
    }
}
