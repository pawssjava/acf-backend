package kz.cyber.acf.core.result.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultRequest {
    private Long userId;
    private Integer place;
    private BigDecimal score;
}
