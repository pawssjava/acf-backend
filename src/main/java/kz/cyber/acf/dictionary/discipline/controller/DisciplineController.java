package kz.cyber.acf.dictionary.discipline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.dictionary.dto.DictionaryDto;
import kz.cyber.acf.dictionary.dto.DictionaryRequest;
import kz.cyber.acf.dictionary.discipline.service.DisciplineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Dictionary — Disciplines", description = "Lookup table for tournament disciplines (e.g. FC 24, eFootball, UFL). New disciplines can be added here without any changes to tournament filtering logic.")
@RestController
@RequestMapping("/api/dictionary/disciplines")
@RequiredArgsConstructor
public class DisciplineController {

    private final DisciplineService disciplineService;

    @Operation(summary = "List all disciplines")
    @GetMapping
    public PageResponse<DictionaryDto> findAll(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        return disciplineService.findAll(page, size);
    }

    @Operation(
            summary = "Get discipline by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Discipline found"),
                    @ApiResponse(responseCode = "404", description = "Discipline not found")
            }
    )
    @GetMapping("/{id}")
    public DictionaryDto findById(@Parameter(description = "Discipline ID") @PathVariable Long id) {
        return disciplineService.findById(id);
    }

    @Operation(
            summary = "Create discipline (admin only)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Discipline created"),
                    @ApiResponse(responseCode = "403", description = "Admin access required")
            }
    )
    @PostMapping
    public ResponseEntity<DictionaryDto> create(@RequestBody DictionaryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(disciplineService.create(req));
    }

    @Operation(
            summary = "Update discipline (admin only)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Discipline updated"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Discipline not found")
            }
    )
    @PutMapping("/{id}")
    public DictionaryDto update(
            @Parameter(description = "Discipline ID") @PathVariable Long id,
            @RequestBody DictionaryRequest req) {
        return disciplineService.update(id, req);
    }

    @Operation(
            summary = "Delete discipline (admin only)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Discipline deleted"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Discipline not found")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Discipline ID") @PathVariable Long id) {
        disciplineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
