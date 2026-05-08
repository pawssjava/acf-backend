package kz.cyber.acf.core.tournament.match.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupDto {
    private String groupName;
    private List<TournamentMatchDto> matches;
    private List<GroupStandingDto> standings;
}
