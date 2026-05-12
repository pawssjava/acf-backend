package kz.cyber.acf.core.education.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EducationMaterialDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String thumbnailUrl;
    private String presentationUrl;
    private String videoUrl;
    private Integer ordering;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
