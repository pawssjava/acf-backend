package kz.cyber.acf.core.tournament.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.tournament.dto.TournamentDto;
import kz.cyber.acf.core.tournament.dto.TournamentRequest;
import kz.cyber.acf.core.tournament.service.TournamentService;
import kz.cyber.acf.core.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Tournaments", description = "Tournament management. Each tournament response includes the resolved status and type names joined from the dictionary tables.")
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final UserService userService;

    @Operation(
            summary = "List tournaments",
            description = "Returns tournaments ordered by creation date descending, with status, type and discipline names resolved. " +
                    "Optionally filter by tournament_type_id and/or one or more discipline_ids. " +
                    "By default only active (non-archived) tournaments are returned; pass archived=true to list the archive (admin only)."
    )
    @GetMapping
    public List<TournamentDto> findAll(
            @Parameter(description = "Filter by tournament type ID") @RequestParam(required = false) Long tournamentTypeId,
            @Parameter(description = "Filter by one or more discipline IDs") @RequestParam(required = false) List<Long> disciplineIds,
            @Parameter(description = "Return archived tournaments instead of active ones (admin only)") @RequestParam(required = false, defaultValue = "false") boolean archived,
            @AuthenticationPrincipal Jwt jwt) {
        if (archived) {
            userService.requireAdmin(jwt);
        }
        return tournamentService.findAll(tournamentTypeId, disciplineIds, archived);
    }

    @Operation(
            summary = "Get tournament by ID",
            description = "Archived tournaments are only visible to admins; other callers get 404.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament found"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @GetMapping("/{id}")
    public TournamentDto findById(@Parameter(description = "Tournament ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return tournamentService.findById(id, jwt);
    }

    @Operation(
            summary = "Create tournament",
            description = "Creates a new tournament. `tournamentStatusId`, `tournamentTypeId` and `disciplineId` must reference existing dictionary entries. `disciplineId` is required (see GET /api/dictionary/disciplines).",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Tournament created")
            }
    )
    @PostMapping
    public ResponseEntity<TournamentDto> create(@RequestBody TournamentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentService.create(req));
    }

    @Operation(
            summary = "Update tournament",
            description = "Admin only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament updated"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @PutMapping("/{id}")
    public TournamentDto update(
            @Parameter(description = "Tournament ID") @PathVariable Long id,
            @RequestBody TournamentRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return tournamentService.update(id, req, jwt);
    }

    @Operation(
            summary = "Upload tournament logo",
            description = "Uploads a logo image to MinIO and attaches it to the tournament.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logo uploaded"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TournamentDto uploadLogo(
            @Parameter(description = "Tournament ID") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return tournamentService.uploadLogo(id, file);
    }

    @Operation(
            summary = "Archive tournament",
            description = "Admin only. Hides the tournament from the public site while keeping it in the admin panel, listed under the archive filter. Reversible via restore.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament archived"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @PatchMapping("/{id}/archive")
    public TournamentDto archive(@Parameter(description = "Tournament ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return tournamentService.archive(id, jwt);
    }

    @Operation(
            summary = "Restore tournament",
            description = "Admin only. Restores a previously archived tournament back to the public site.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament restored"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @PatchMapping("/{id}/restore")
    public TournamentDto restore(@Parameter(description = "Tournament ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return tournamentService.restore(id, jwt);
    }

    @Operation(
            summary = "Delete tournament",
            description = "Admin only. Permanently deletes the tournament. Cannot be undone.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Tournament deleted"),
                    @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Tournament ID") @PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        tournamentService.delete(id, jwt);
        return ResponseEntity.noContent().build();
    }
}
