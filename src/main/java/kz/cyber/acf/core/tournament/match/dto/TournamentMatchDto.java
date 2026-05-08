package kz.cyber.acf.core.tournament.match.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TournamentMatchDto {
    private Long id;
    private Long tournamentId;
    private String phase;
    private Integer roundNumber;
    private Integer matchNumber;
    private String groupName;
    private Long participant1Id;
    private String participant1Username;
    private String participant1FirstName;
    private String participant1LastName;
    private Long participant2Id;
    private String participant2Username;
    private String participant2FirstName;
    private String participant2LastName;
    private Integer score1;
    private Integer score2;
    private Long winnerId;
    private String status;
    private Long nextMatchId;
}
