package kz.cyber.acf.dictionary.club.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.dictionary.club.dto.ClubDto;
import kz.cyber.acf.dictionary.club.dto.ClubRequest;
import kz.cyber.acf.dictionary.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dictionary — Clubs", description = "Lookup table for esports clubs.")
@RestController
@RequestMapping("/api/dictionary/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @Operation(summary = "List all clubs")
    @GetMapping
    public List<ClubDto> findAll() {
        return clubService.findAll();
    }

    @Operation(summary = "Get club by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Club found"),
                    @ApiResponse(responseCode = "404", description = "Club not found")
            })
    @GetMapping("/{id}")
    public ClubDto findById(@Parameter(description = "Club ID") @PathVariable Long id) {
        return clubService.findById(id);
    }

    @Operation(summary = "Create club (admin only)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Club created"),
                    @ApiResponse(responseCode = "403", description = "Admin access required")
            })
    @PostMapping
    public ResponseEntity<ClubDto> create(@RequestBody ClubRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clubService.create(req));
    }

    @Operation(summary = "Update club (admin only)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Club updated"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Club not found")
            })
    @PutMapping("/{id}")
    public ClubDto update(
            @Parameter(description = "Club ID") @PathVariable Long id,
            @RequestBody ClubRequest req) {
        return clubService.update(id, req);
    }

    @Operation(summary = "Delete club (admin only)",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Club deleted"),
                    @ApiResponse(responseCode = "403", description = "Admin access required"),
                    @ApiResponse(responseCode = "404", description = "Club not found")
            })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Club ID") @PathVariable Long id) {
        clubService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
