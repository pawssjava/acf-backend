package kz.cyber.acf.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase();
    }
}
