package kz.cyber.acf.core.registration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantDto {
    private Long id;
    private Long tournamentId;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private OffsetDateTime registeredDate;
}