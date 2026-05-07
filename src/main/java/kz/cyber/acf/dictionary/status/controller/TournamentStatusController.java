package kz.cyber.acf.dictionary.status.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.dictionary.dto.DictionaryDto;
import kz.cyber.acf.dictionary.dto.DictionaryRequest;
import kz.cyber.acf.dictionary.status.service.TournamentStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dictionary — Tournament Statuses", description = "Lookup table for tournament statuses (Активные, Будущие, Завершенные). Used when creating or filtering tournaments.")
@RestController
@RequestMapping("/api/dictionary/tournament-statuses")
@RequiredArgsConstructor
public class TournamentStatusController {

    private final TournamentStatusService tournamentStatusService;

    @Operation(summary = "List all tournament statuses")
    @GetMapping
    public List<DictionaryDto> findAll() {
        return tournamentStatusService.findAll();
    }

    @Operation(
            summary = "Get tournament status by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status found"),
                    @ApiResponse(responseCode = "404", description = "Status not found")
            }
    )
    @GetMapping("/{id}")
    public DictionaryDto findById(@Parameter(description = "Tournament status ID") @PathVariable Long id) {
        return tournamentStatusService.findById(id);
    }

    @Operation(
            summary = "Create tournament status",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Status created")
            }
    )
    @PostMapping
    public ResponseEntity<DictionaryDto> create(@RequestBody DictionaryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentStatusService.create(req));
    }

    @Operation(
            summary = "Update tournament status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status updated"),
                    @ApiResponse(responseCode = "404", description = "Status not found")
            }
    )
    @PutMapping("/{id}")
    public DictionaryDto update(
            @Parameter(description = "Tournament status ID") @PathVariable Long id,
            @RequestBody DictionaryRequest req) {
        return tournamentStatusService.update(id, req);
    }

    @Operation(
            summary = "Delete tournament status",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Status deleted"),
                    @ApiResponse(responseCode = "404", description = "Status not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Tournament status ID") @PathVariable Long id) {
        tournamentStatusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
