package kz.cyber.acf.core.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private Long phoneNumber;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Boolean isAdmin;
    private String photo;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
