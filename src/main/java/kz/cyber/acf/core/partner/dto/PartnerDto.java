package kz.cyber.acf.core.partner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartnerDto {
    private Long id;
    private String name;
    private String logo;
    private String description;
    private String hyperlink;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
