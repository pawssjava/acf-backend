package kz.cyber.acf.core.user.service;

import kz.cyber.acf.core.user.dto.UpdateUserRequest;
import kz.cyber.acf.core.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.USER;

@Service
@RequiredArgsConstructor
public class UserService {

    private final DefaultDSLContext dsl;

    public List<UserDto> findAll() {
        return dsl.selectFrom(USER).fetch(r -> new UserDto(
                r.getId(), r.getUsername(), r.getPhoneNumber(),
                r.getFirstName(), r.getLastName(), r.getBirthDate(),
                r.getIsAdmin(), r.getPhoto(), r.getCreatedDate(), r.getUpdatedDate()
        ));
    }

    public UserDto findById(Long id) {
        return dsl.selectFrom(USER)
                .where(USER.ID.eq(id))
                .fetchOptional()
                .map(r -> new UserDto(
                        r.getId(), r.getUsername(), r.getPhoneNumber(),
                        r.getFirstName(), r.getLastName(), r.getBirthDate(),
                        r.getIsAdmin(), r.getPhoto(), r.getCreatedDate(), r.getUpdatedDate()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDto update(Long id, UpdateUserRequest req) {
        int updated = dsl.update(USER)
                .set(USER.FIRST_NAME, req.getFirstName())
                .set(USER.LAST_NAME, req.getLastName())
                .set(USER.BIRTH_DATE, req.getBirthDate())
                .set(USER.PHOTO, req.getPhoto())
                .set(USER.UPDATED_DATE, OffsetDateTime.now())
                .where(USER.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return findById(id);
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(USER).where(USER.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }
}
