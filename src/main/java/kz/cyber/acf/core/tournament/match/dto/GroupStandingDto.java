package kz.cyber.acf.core.tournament.match.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GroupStandingDto {
    private Long userId;
    private String groupName;
    private String username;
    private String firstName;
    private String lastName;
    private int played;
    private int wins;
    private int draws;
    private int losses;
    private int points;
    private int scoreDiff;
    private Double swissPoints;
    private Double buchholz;
}
