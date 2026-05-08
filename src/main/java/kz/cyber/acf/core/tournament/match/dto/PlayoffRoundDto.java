package kz.cyber.acf.core.tournament.match.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlayoffRoundDto {
    private Integer roundNumber;
    private String roundName;
    private List<TournamentMatchDto> matches;
}
