package kz.cyber.acf.core.result.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultDto {
    private Long id;
    private Long tournamentId;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private Integer place;
    private BigDecimal score;
    private OffsetDateTime createdDate;
}
