package kz.cyber.acf.core.news.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsDto {
    private Long id;
    private String title;
    private String description;
    private String image;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
    private boolean archived;
    private OffsetDateTime archivedDate;
}
