package kz.cyber.acf.dictionary.type.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.dictionary.dto.DictionaryDto;
import kz.cyber.acf.dictionary.dto.DictionaryRequest;
import kz.cyber.acf.dictionary.type.service.TournamentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Dictionary — Tournament Types", description = "Lookup table for tournament types (e.g. Региональные турниры, Социальная лига). Used when creating or filtering tournaments.")
@RestController
@RequestMapping("/api/dictionary/tournament-types")
@RequiredArgsConstructor
public class TournamentTypeController {

    private final TournamentTypeService tournamentTypeService;

    @Operation(summary = "List all tournament types")
    @GetMapping
    public PageResponse<DictionaryDto> findAll(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return tournamentTypeService.findAll(page, size);
    }

    @Operation(
            summary = "Get tournament type by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Type found"),
                    @ApiResponse(responseCode = "404", description = "Type not found")
            }
    )
    @GetMapping("/{id}")
    public DictionaryDto findById(@Parameter(description = "Tournament type ID") @PathVariable Long id) {
        return tournamentTypeService.findById(id);
    }

    @Operation(
            summary = "Create tournament type (admin only)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Type created"),
                    @ApiResponse(responseCode = "403", description = "Admin access required")
            }
    )
    @PostMapping
    public ResponseEntity<DictionaryDto> create(@RequestBody DictionaryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentTypeService.create(req));
    }

    @Operation(
            summary = "Update tournament type (admin only)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Type updated"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Type not found")
            }
    )
    @PutMapping("/{id}")
    public DictionaryDto update(
            @Parameter(description = "Tournament type ID") @PathVariable Long id,
            @RequestBody DictionaryRequest req) {
        return tournamentTypeService.update(id, req);
    }

    @Operation(
            summary = "Delete tournament type (admin only)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Type deleted"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Type not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Tournament type ID") @PathVariable Long id) {
        tournamentTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
