package kz.cyber.acf.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsLogDto {
    private Long id;
    private String phoneNumber;
    private String code;
    private String action;
    private OffsetDateTime sentAt;
    private OffsetDateTime expiresAt;
    private OffsetDateTime verifiedAt;
    private Boolean used;
}
