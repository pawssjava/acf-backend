package kz.cyber.acf.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KeycloakUserRequest {
    private String username;
    private boolean enabled;
    private boolean emailVerified;
    private List<KeycloakCredential> credentials;
    private List<String> requiredActions;
}
