package kz.cyber.acf.auth.dto;

import lombok.Data;

@Data
public class VerifySmsRequest {
    private String phone;
    private String code;
}
