package kz.cyber.acf.core.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.registration.dto.ParticipantDto;
import kz.cyber.acf.core.registration.dto.RegistrationRequest;
import kz.cyber.acf.core.registration.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tournament Participants", description = "Register and remove users from a tournament. Registering is rejected when the tournament has reached its capacity.")
@RestController
@RequestMapping("/api/tournaments/{tournamentId}/participants")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(
            summary = "List participants",
            description = "Returns all users registered for the given tournament, ordered by registration date."
    )
    @GetMapping
    public List<ParticipantDto> getParticipants(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        return registrationService.getParticipants(tournamentId);
    }

    @Operation(
            summary = "Register user to tournament",
            description = "Adds a user to the tournament participant list. Returns `400` if the tournament is already full.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User registered"),
                    @ApiResponse(responseCode = "400", description = "Tournament is full"),
                    @ApiResponse(responseCode = "404", description = "Tournament or user not found")
            }
    )
    @PostMapping
    public ResponseEntity<ParticipantDto> register(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @RequestBody RegistrationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(registrationService.register(tournamentId, req.getUserId()));
    }

    @Operation(
            summary = "Unregister user from tournament",
            description = "Removes a user from the tournament participant list.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User unregistered"),
                    @ApiResponse(responseCode = "404", description = "Registration not found")
            }
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unregister(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "User ID to remove") @PathVariable Long userId) {
        registrationService.unregister(tournamentId, userId);
        return ResponseEntity.noContent().build();
    }
}
