package kz.cyber.acf.core.tournament.match.dto;

import lombok.Data;

import java.util.List;

@Data
public class BracketDto {
    private String format;
    private String phase;
    private List<GroupDto> groups;
    private List<PlayoffRoundDto> playoffRounds;
}
