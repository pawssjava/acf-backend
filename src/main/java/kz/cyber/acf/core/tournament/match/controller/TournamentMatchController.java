package kz.cyber.acf.core.tournament.match.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.cyber.acf.core.tournament.match.dto.*;
import kz.cyber.acf.core.tournament.match.service.TournamentMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tournament Matches", description = "Start tournaments, manage match scores, and view brackets.")
@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
public class TournamentMatchController {

    private final TournamentMatchService service;

    @Operation(summary = "Start tournament",
            description = "Generates the initial bracket/matches based on the tournament's format (SINGLE_ELIMINATION, GROUP_STAGE, SWISS, EKPL). " +
                    "For GROUP_STAGE, optionally pass numberOfGroups in the body (default: 2).")
    @PostMapping("/start")
    public ResponseEntity<Void> start(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @RequestBody(required = false) TournamentStartRequest req) {
        service.start(tournamentId, req);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all matches")
    @GetMapping("/matches")
    public List<TournamentMatchDto> getMatches(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        return service.getMatches(tournamentId);
    }

    @Operation(summary = "Get bracket",
            description = "Returns the full bracket organized by phase. Supports GROUP_STAGE, SINGLE_ELIMINATION, SWISS, and EKPL formats.")
    @GetMapping("/bracket")
    public BracketDto getBracket(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        return service.getBracket(tournamentId);
    }

    @Operation(summary = "Get standings",
            description = "Returns standings. For GROUP_STAGE: per-group standings. For SWISS: Swiss points + Buchholz. For EKPL: regular season standings.")
    @GetMapping("/standings")
    public List<GroupStandingDto> getStandings(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        return service.getStandings(tournamentId);
    }

    @Operation(summary = "Update match score", description = "Sets score1 and score2 for a match and marks it IN_PROGRESS.")
    @PatchMapping("/matches/{matchId}/score")
    public TournamentMatchDto updateScore(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "Match ID") @PathVariable Long matchId,
            @RequestBody ScoreUpdateRequest req) {
        return service.updateScore(tournamentId, matchId, req);
    }

    @Operation(summary = "Complete match",
            description = "Declares winner based on current scores. For SWISS format, a tie is recorded as a draw. For other formats, ties require /winner.")
    @PostMapping("/matches/{matchId}/complete")
    public TournamentMatchDto completeMatch(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "Match ID") @PathVariable Long matchId) {
        return service.completeMatch(tournamentId, matchId);
    }

    @Operation(summary = "Set match winner manually", description = "Allows admin to explicitly declare the winner (useful for draws or overrides).")
    @PutMapping("/matches/{matchId}/winner")
    public TournamentMatchDto setWinner(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @Parameter(description = "Match ID") @PathVariable Long matchId,
            @RequestBody WinnerRequest req) {
        return service.setWinner(tournamentId, matchId, req.getWinnerId());
    }

    @Operation(summary = "Next Swiss round",
            description = "Generates the next Swiss round using greedy pairing (sorted by points, no rematches). " +
                    "If total_rounds is reached, finalizes the tournament and saves results.")
    @PostMapping("/next-round")
    public ResponseEntity<Void> nextSwissRound(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId) {
        service.nextSwissRound(tournamentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Advance to playoff (eKPL)",
            description = "Takes the top N participants from the regular season and generates the single-elimination playoff. " +
                    "Only valid after all regular season matches are completed. Default advancers: 8.")
    @PostMapping("/advance-regular-season")
    public ResponseEntity<Void> advanceRegularSeason(
            @Parameter(description = "Tournament ID") @PathVariable Long tournamentId,
            @RequestBody(required = false) AdvancePlayoffRequest req) {
        int advancers = req != null && req.getAdvancersPerGroup() != null ? req.getAdvancersPerGroup() : 8;
        service.advanceRegularSeason(tournamentId, advancers);
        return ResponseEntity.noContent().build();
    }
}
