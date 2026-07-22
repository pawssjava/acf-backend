package kz.cyber.acf.auth.service;

import kz.cyber.acf.auth.SmsAction;
import kz.cyber.acf.auth.client.KeycloakAdminClient;
import kz.cyber.acf.auth.client.KeycloakTokenClient;
import kz.cyber.acf.auth.dto.ForgotPasswordRequest;
import kz.cyber.acf.auth.dto.KeycloakCredential;
import kz.cyber.acf.auth.dto.KeycloakPasswordResetRequest;
import kz.cyber.acf.auth.dto.KeycloakUserRequest;
import kz.cyber.acf.auth.dto.KeycloakUserResponse;
import kz.cyber.acf.auth.dto.RegisterRequest;
import kz.cyber.acf.auth.dto.TokenResponse;
import kz.cyber.acf.core.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Value;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.USER;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DefaultDSLContext dsl;
    private final SmsVerificationService smsService;
    private final KeycloakTokenClient keycloakTokenClient;
    private final KeycloakAdminClient keycloakAdminClient;

    @Value("${application.keycloak.admin-auth-client-id}")
    private String clientId;

    @Value("${application.keycloak.admin-auth-client-secret}")
    private String clientSecret;

    @Value("${application.keycloak.realm}")
    private String realm;

    public void sendSms(String phone) {
        smsService.sendCode(phone, SmsAction.REGISTRATION);
    }

    public void verifySms(String phone, String code) {
        if (!smsService.verifyCode(phone, code)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "SMS коды қате",
                    "Неверный SMS-код",
                    "Invalid SMS code");
        }
    }

    public void checkPhoneAvailable(String phone) {
        boolean exists = dsl.fetchExists(USER, USER.PHONE_NUMBER.eq(Long.parseLong(phone)));
        if (exists) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Телефон нөмірі тіркелген",
                    "Номер телефона уже зарегистрирован",
                    "Phone number already registered");
        }
    }

    public void checkUsernameAvailable(String username) {
        boolean exists = dsl.fetchExists(USER, USER.USERNAME.eq(normalize(username)));
        if (exists) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Пайдаланушы аты бос емес",
                    "Имя пользователя уже занято",
                    "Username already taken");
        }
    }

    public TokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "password");
        form.add("username", normalize(username));
        form.add("password", password);
        return keycloakTokenClient.token(realm, form);
    }

    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return keycloakTokenClient.token(realm, form);
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        keycloakTokenClient.logout(realm, form);
    }

    public UserDto register(RegisterRequest req) {
        if (!smsService.isVerified(req.getPhone())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Телефон нөмірі расталмаған",
                    "Номер телефона не подтверждён",
                    "Phone number not verified");
        }

        createKeycloakUser(req);

        var record = dsl.insertInto(USER)
                .set(USER.USERNAME, req.getUsername())
                .set(USER.PHONE_NUMBER, Long.parseLong(req.getPhone()))
                .set(USER.FIRST_NAME, req.getFirstName())
                .set(USER.LAST_NAME, req.getLastName())
                .set(USER.BIRTH_DATE, req.getBirthDate())
                .set(USER.IS_ADMIN, false)
                .set(USER.CREATED_DATE, OffsetDateTime.now())
                .set(USER.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();

        smsService.invalidate(req.getPhone());

        return new UserDto(
                record.getId(), record.getUsername(), record.getPhoneNumber(),
                record.getFirstName(), record.getLastName(), record.getBirthDate(),
                record.getIsAdmin(), record.getPhoto(),
                record.getCreatedDate(), record.getUpdatedDate()
        );
    }

    public void sendForgotPasswordSms(String phone) {
        dsl.selectFrom(USER)
                .where(USER.PHONE_NUMBER.eq(Long.parseLong(phone)))
                .fetchOptional()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Пайдаланушы табылмады", "Пользователь не найден", "User not found"));

        smsService.sendCode(phone, SmsAction.FORGOT_PASSWORD);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        username = normalize(username);
        try {
            login(username, currentPassword);
        } catch (Exception e) {
            throw new AppException(HttpStatus.UNAUTHORIZED,
                    "Ағымдағы құпия сөз қате",
                    "Текущий пароль неверен",
                    "Current password is incorrect");
        }

        String adminToken = "Bearer " + adminToken().getAccessToken();
        List<KeycloakUserResponse> users = keycloakAdminClient.findUsers(adminToken, realm, username, true);
        if (users.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Пайдаланушы табылмады",
                    "Пользователь не найден",
                    "User not found");
        }

        keycloakAdminClient.resetPassword(
                adminToken, realm, users.get(0).getId(),
                new KeycloakPasswordResetRequest("password", newPassword, false)
        );
    }

    public void resetPassword(ForgotPasswordRequest req) {
        if (!smsService.isVerified(req.getPhone())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Телефон нөмірі расталмаған",
                    "Номер телефона не подтверждён",
                    "Phone number not verified");
        }

        String username = dsl.selectFrom(USER)
                .where(USER.PHONE_NUMBER.eq(Long.parseLong(req.getPhone())))
                .fetchOptional()
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Пайдаланушы табылмады", "Пользователь не найден", "User not found"))
                .getUsername();

        String adminToken = "Bearer " + adminToken().getAccessToken();

        List<KeycloakUserResponse> users = keycloakAdminClient.findUsers(adminToken, realm, username, true);
        if (users.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Пайдаланушы табылмады",
                    "Пользователь не найден",
                    "User not found");
        }

        keycloakAdminClient.resetPassword(
                adminToken, realm, users.get(0).getId(),
                new KeycloakPasswordResetRequest("password", req.getNewPassword(), false)
        );

        smsService.invalidate(req.getPhone());
    }

    private void createKeycloakUser(RegisterRequest req) {
        String adminToken = "Bearer " + adminToken().getAccessToken();

        KeycloakUserRequest keycloakUser = KeycloakUserRequest.builder()
                .username(req.getUsername())
                .enabled(true)
                .emailVerified(true)
                .requiredActions(List.of())
                .credentials(List.of(new KeycloakCredential("password", req.getPassword(), false)))
                .build();

        keycloakAdminClient.createUser(adminToken, realm, keycloakUser);
    }

    private static String normalize(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    private TokenResponse adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("grant_type", "client_credentials");
        return keycloakTokenClient.token(realm, form);
    }
}
