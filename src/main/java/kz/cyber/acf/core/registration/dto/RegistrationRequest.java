package kz.cyber.acf.core.registration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationRequest {
    private Long userId;

    @NotBlank(message = "PSN is mandatory")
    private String psn;
}
