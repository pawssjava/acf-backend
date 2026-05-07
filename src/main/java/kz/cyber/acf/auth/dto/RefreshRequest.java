package kz.cyber.acf.auth.dto;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
