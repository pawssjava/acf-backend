package kz.cyber.acf.dictionary.club.dto;

import lombok.Data;

@Data
public class ClubRequest {
    private String nameRu;
    private String nameKk;
    private String nameEn;
    private Boolean isActive;
}
