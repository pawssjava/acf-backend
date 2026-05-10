package kz.cyber.acf.core.tournament.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.tournament.dto.TournamentDto;
import kz.cyber.acf.core.tournament.dto.TournamentRequest;
import kz.cyber.acf.core.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Tournaments", description = "Tournament management. Each tournament response includes the resolved status and type names joined from the dictionary tables.")
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;

    @Operation(summary = "List all tournaments", description = "Returns all tournaments ordered by creation date descending, with status and type names resolved. Optionally filter by tournament_type_id.")
    @GetMapping
    public List<TournamentDto> findAll(
            @Parameter(description = "Filter by tournament type ID") @RequestParam(required = false) Long tournamentTypeId) {
        return tournamentService.findAll(tournamentTypeId);
    }

    @Operation(
            summary = "Get tournament by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament found"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @GetMapping("/{id}")
    public TournamentDto findById(@Parameter(description = "Tournament ID") @PathVariable Long id) {
        return tournamentService.findById(id);
    }

    @Operation(
            summary = "Create tournament",
            description = "Creates a new tournament. `tournamentStatusId` and `tournamentTypeId` must reference existing dictionary entries.",
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
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tournament updated"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @PutMapping("/{id}")
    public TournamentDto update(
            @Parameter(description = "Tournament ID") @PathVariable Long id,
            @RequestBody TournamentRequest req) {
        return tournamentService.update(id, req);
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
            summary = "Delete tournament",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Tournament deleted"),
                    @ApiResponse(responseCode = "404", description = "Tournament not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Tournament ID") @PathVariable Long id) {
        tournamentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
