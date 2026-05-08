package kz.cyber.acf.core.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.registration.dto.RegistrationLogDto;
import kz.cyber.acf.core.registration.service.RegistrationLogService;
import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tournament Registration Log", description = "Admin-only audit log of registration and unregistration actions for a tournament.")
@RestController
@RequestMapping("/api/tournaments/{tournamentId}/registration-log")
@RequiredArgsConstructor
public class RegistrationLogController {

    private final RegistrationLogService registrationLogService;
    private final UserService userService;

    @Operation(
            summary = "Get registration activity log",
            description = "Returns a paginated audit trail of REGISTER and UNREGISTER actions for the tournament. Admin access only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Log returned"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin")
            }
    )
    @GetMapping
    public PageResponse<RegistrationLogDto> getLogs(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "Search by PSN, username, or full name") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        userService.requireAdmin(jwt.getClaimAsString("preferred_username"));
        return registrationLogService.getLogs(tournamentId, search, page, size);
    }
}
