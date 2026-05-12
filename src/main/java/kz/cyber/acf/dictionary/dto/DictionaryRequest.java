package kz.cyber.acf.dictionary.dto;

import lombok.Data;

@Data
public class DictionaryRequest {
    private String nameRu;
    private String nameKk;
    private String nameEn;
    private Boolean isActive;
}
