package kz.cyber.acf.core.result.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.result.dto.ResultDto;
import kz.cyber.acf.core.result.dto.ResultRequest;
import kz.cyber.acf.core.result.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tournament Results", description = "Record and manage final standings for a tournament. Results are ordered by place ascending.")
@RestController
@RequestMapping("/api/tournaments/{tournamentId}/results")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @Operation(
            summary = "Get tournament results",
            description = "Returns all result entries for the tournament sorted by place (1st, 2nd, …)."
    )
    @GetMapping
    public List<ResultDto> getResults(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        return resultService.getResults(tournamentId);
    }

    @Operation(
            summary = "Add result entry",
            description = "Records a place and score for a user in a given tournament. Each user can have at most one result per tournament.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Result recorded")
            }
    )
    @PostMapping
    public ResponseEntity<ResultDto> create(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @RequestBody ResultRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resultService.create(tournamentId, req));
    }

    @Operation(
            summary = "Update result entry",
            description = "Updates the place and/or score for an existing result.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Result updated"),
                    @ApiResponse(responseCode = "404", description = "Result not found")
            }
    )
    @PutMapping("/{userId}")
    public ResultDto update(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "User ID whose result to update") @PathVariable Long userId,
            @RequestBody ResultRequest req) {
        return resultService.update(tournamentId, userId, req);
    }

    @Operation(
            summary = "Delete result entry",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Result deleted"),
                    @ApiResponse(responseCode = "404", description = "Result not found")
            }
    )
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "User ID whose result to delete") @PathVariable Long userId) {
        resultService.delete(tournamentId, userId);
        return ResponseEntity.noContent().build();
    }
}
