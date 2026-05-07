package kz.cyber.acf.core.tournament.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TournamentRequest {
    private String name;
    private LocalDate startDate;
    private Integer capacity;
    private BigDecimal prizeMoney;
    private Long tournamentStatusId;
    private Long tournamentTypeId;
}
