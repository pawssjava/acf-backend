package kz.cyber.acf.dictionary.dto;

import lombok.Data;

@Data
public class DictionaryRequest {
    private String name;
    private Boolean isActive;
}
