package kz.cyber.acf.dictionary.city.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CityDto {
    private Long id;
    private String nameRu;
    private String nameKk;
    private String nameEn;
    private Boolean isActive;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
