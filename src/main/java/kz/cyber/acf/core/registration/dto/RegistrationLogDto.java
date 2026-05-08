package kz.cyber.acf.core.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationLogDto {
    private Long id;
    private Long tournamentId;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String action;
    private String psn;
    private OffsetDateTime createdDate;
}
