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

    public UserDto(Long id, String username, Long phoneNumber, String firstName, String lastName, LocalDate birthDate, Boolean isAdmin, String photo, OffsetDateTime createdDate, OffsetDateTime updatedDate) {
        this.id = id;
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.isAdmin = isAdmin;
        this.photo = photo;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    private String username;
    private Long phoneNumber;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private Boolean isAdmin;
    private String photo;
    private Long cityId;
    private String cityNameRu;
    private String cityNameKk;
    private String cityNameEn;
    private Boolean isVerified;
    private Long clubId;
    private String clubNameRu;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
}
