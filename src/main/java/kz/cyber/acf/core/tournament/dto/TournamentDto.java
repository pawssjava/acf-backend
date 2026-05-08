package kz.cyber.acf.core.tournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TournamentDto {
    private Long id;
    private String name;
    private String logo;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer capacity;
    private BigDecimal prizeMoney;
    private Long tournamentStatusId;
    private String tournamentStatusName;
    private Long tournamentTypeId;
    private String tournamentTypeName;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
    private String format;
    private String phase;
    private Integer totalRounds;
}
