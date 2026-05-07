package kz.cyber.acf.auth.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String phone;
    private String code;
    private String newPassword;
}
