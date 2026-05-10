package kz.cyber.acf.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerMatchDto {
    private Long matchId;
    private Long tournamentId;
    private String tournamentName;
    private Long opponentId;
    private String opponentUsername;
    private String opponentPhoto;
    private Integer myScore;
    private Integer opponentScore;
    private String result;
    private String status;
    private OffsetDateTime updatedDate;
}
