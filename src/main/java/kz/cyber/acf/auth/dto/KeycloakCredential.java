package kz.cyber.acf.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeycloakCredential {
    private String type;
    private String value;
    private boolean temporary;
}
