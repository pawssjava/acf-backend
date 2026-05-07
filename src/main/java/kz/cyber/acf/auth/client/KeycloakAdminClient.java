package kz.cyber.acf.auth.client;

import kz.cyber.acf.auth.dto.KeycloakPasswordResetRequest;
import kz.cyber.acf.auth.dto.KeycloakUserRequest;
import kz.cyber.acf.auth.dto.KeycloakUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "keycloak-admin", url = "${application.keycloak.admin-auth-url}")
public interface KeycloakAdminClient {

    @PostMapping(
            value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void createUser(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @RequestBody KeycloakUserRequest user
    );

    @GetMapping("/admin/realms/{realm}/users")
    List<KeycloakUserResponse> findUsers(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @RequestParam("username") String username,
            @RequestParam("exact") boolean exact
    );

    @PutMapping(
            value = "/admin/realms/{realm}/users/{userId}/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void resetPassword(
            @RequestHeader("Authorization") String authorization,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestBody KeycloakPasswordResetRequest request
    );
}
