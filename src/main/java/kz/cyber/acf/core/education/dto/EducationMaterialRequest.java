package kz.cyber.acf.core.education.dto;

import lombok.Data;

@Data
public class EducationMaterialRequest {
    private String title;
    private String description;
    private String category;
    private Integer ordering;
}
