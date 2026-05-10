package kz.cyber.acf.dictionary.city.dto;

import lombok.Data;

@Data
public class CityRequest {
    private String nameRu;
    private String nameKk;
    private String nameEn;
    private Boolean isActive;
}
