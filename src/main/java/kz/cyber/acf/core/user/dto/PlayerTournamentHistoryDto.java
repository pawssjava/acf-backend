package kz.cyber.acf.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerTournamentHistoryDto {
    private Long tournamentId;
    private String tournamentName;
    private String logo;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long tournamentStatusId;
    private String tournamentStatusName;
    private Long tournamentTypeId;
    private String tournamentTypeName;
    private String format;
    private Integer place;
    private BigDecimal score;
}
