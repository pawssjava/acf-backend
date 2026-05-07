package kz.cyber.acf.auth.service;

import kz.cyber.acf.auth.dto.RegisterRequest;
import kz.cyber.acf.core.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static group.bi.postsales.database.Tables.USER;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DefaultDSLContext dsl;
    private final SmsVerificationService smsService;

    public void sendSms(String phone) {
        smsService.sendCode(phone);
    }

    public UserDto register(RegisterRequest req) {
        if (!smsService.verifyCode(req.getPhone(), req.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SMS code");
        }

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
}
