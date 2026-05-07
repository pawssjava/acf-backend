package kz.cyber.acf.auth.dto;

import lombok.Data;

@Data
public class KeycloakUserResponse {
    private String id;
    private String username;
}
