package kz.cyber.acf.dictionary.club.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClubDto {
    private Long id;
    private String nameRu;
    private String nameKk;
    private String nameEn;
    private Boolean isActive;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
