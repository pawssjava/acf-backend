package kz.cyber.acf.auth.client;

import kz.cyber.acf.auth.dto.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "keycloak-token", url = "${application.keycloak.admin-auth-url}")
public interface KeycloakTokenClient {

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    TokenResponse token(
            @PathVariable("realm") String realm,
            @RequestBody MultiValueMap<String, String> form
    );

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/logout",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    void logout(
            @PathVariable("realm") String realm,
            @RequestBody MultiValueMap<String, String> form
    );
}
