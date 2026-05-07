package kz.cyber.acf.auth.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {
    private String phone;
    private String code;
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
}
