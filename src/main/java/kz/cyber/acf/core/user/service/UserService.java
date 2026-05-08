package kz.cyber.acf.core.user.service;

import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.core.user.dto.PlayerTournamentHistoryDto;
import kz.cyber.acf.core.user.dto.UpdateUserRequest;
import kz.cyber.acf.core.user.dto.UserDto;
import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String BUCKET = "users";

    private final DefaultDSLContext dsl;
    private final MinioService minioService;

    public List<UserDto> findAll() {
        return dsl.selectFrom(USER).fetch(r -> new UserDto(
                r.getId(), r.getUsername(), r.getPhoneNumber(),
                r.getFirstName(), r.getLastName(), r.getBirthDate(),
                r.getIsAdmin(), resolveUrl(r.getPhoto()), r.getCreatedDate(), r.getUpdatedDate()
        ));
    }

    public UserDto findByUsername(String username) {
        return dsl.selectFrom(USER)
                .where(USER.USERNAME.eq(username))
                .fetchOptional()
                .map(r -> new UserDto(
                        r.getId(), r.getUsername(), r.getPhoneNumber(),
                        r.getFirstName(), r.getLastName(), r.getBirthDate(),
                        r.getIsAdmin(), resolveUrl(r.getPhoto()), r.getCreatedDate(), r.getUpdatedDate()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDto findById(Long id) {
        return dsl.selectFrom(USER)
                .where(USER.ID.eq(id))
                .fetchOptional()
                .map(r -> new UserDto(
                        r.getId(), r.getUsername(), r.getPhoneNumber(),
                        r.getFirstName(), r.getLastName(), r.getBirthDate(),
                        r.getIsAdmin(), resolveUrl(r.getPhoto()), r.getCreatedDate(), r.getUpdatedDate()
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public UserDto update(Long id, UpdateUserRequest req) {
        int updated = dsl.update(USER)
                .set(USER.FIRST_NAME, req.getFirstName())
                .set(USER.LAST_NAME, req.getLastName())
                .set(USER.BIRTH_DATE, req.getBirthDate())
                .set(USER.UPDATED_DATE, OffsetDateTime.now())
                .where(USER.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return findById(id);
    }

    public UserDto uploadPhoto(Long id, MultipartFile file) {
        findById(id);
        String objectName = minioService.upload(BUCKET, file);
        dsl.update(USER)
                .set(USER.PHOTO, objectName)
                .set(USER.UPDATED_DATE, OffsetDateTime.now())
                .where(USER.ID.eq(id))
                .execute();
        return findById(id);
    }

    public void requireAdmin(String username) {
        Boolean isAdmin = dsl.select(USER.IS_ADMIN)
                .from(USER)
                .where(USER.USERNAME.eq(username))
                .fetchOneInto(Boolean.class);
        if (!Boolean.TRUE.equals(isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(USER).where(USER.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private static final long STATUS_COMPLETED = 3L;

    public PageResponse<PlayerTournamentHistoryDto> getTournamentHistory(Long userId, int page, int size) {
        findById(userId);

        int offset = page * size;

        long total = dsl.selectCount()
                .from(TOURNAMENT_RESULT)
                .join(TOURNAMENT).on(TOURNAMENT_RESULT.TOURNAMENT_ID.eq(TOURNAMENT.ID))
                .where(TOURNAMENT_RESULT.USER_ID.eq(userId)
                        .and(TOURNAMENT.TOURNAMENT_STATUS.eq(STATUS_COMPLETED)))
                .fetchOne(0, Long.class);

        List<PlayerTournamentHistoryDto> content = dsl.select(
                        TOURNAMENT.ID,
                        TOURNAMENT.NAME,
                        TOURNAMENT.LOGO,
                        TOURNAMENT.START_DATE,
                        TOURNAMENT.END_DATE,
                        TOURNAMENT.TOURNAMENT_STATUS,
                        D_TOURNAMENT_STATUS.NAME.as("status_name"),
                        TOURNAMENT.TOURNAMENT_TYPE,
                        D_TOURNAMENT_TYPE.NAME.as("type_name"),
                        TOURNAMENT.FORMAT,
                        TOURNAMENT_RESULT.PLACE,
                        TOURNAMENT_RESULT.SCORE
                )
                .from(TOURNAMENT_RESULT)
                .join(TOURNAMENT).on(TOURNAMENT_RESULT.TOURNAMENT_ID.eq(TOURNAMENT.ID))
                .leftJoin(D_TOURNAMENT_STATUS).on(TOURNAMENT.TOURNAMENT_STATUS.eq(D_TOURNAMENT_STATUS.ID))
                .leftJoin(D_TOURNAMENT_TYPE).on(TOURNAMENT.TOURNAMENT_TYPE.eq(D_TOURNAMENT_TYPE.ID))
                .where(TOURNAMENT_RESULT.USER_ID.eq(userId)
                        .and(TOURNAMENT.TOURNAMENT_STATUS.eq(STATUS_COMPLETED)))
                .orderBy(TOURNAMENT.END_DATE.desc())
                .limit(size)
                .offset(offset)
                .fetch(r -> new PlayerTournamentHistoryDto(
                        r.get(TOURNAMENT.ID),
                        r.get(TOURNAMENT.NAME),
                        resolveUrl(r.get(TOURNAMENT.LOGO)),
                        r.get(TOURNAMENT.START_DATE),
                        r.get(TOURNAMENT.END_DATE),
                        r.get(TOURNAMENT.TOURNAMENT_STATUS),
                        r.get("status_name", String.class),
                        r.get(TOURNAMENT.TOURNAMENT_TYPE),
                        r.get("type_name", String.class),
                        r.get(TOURNAMENT.FORMAT),
                        r.get(TOURNAMENT_RESULT.PLACE),
                        r.get(TOURNAMENT_RESULT.SCORE)
                ));

        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private String resolveUrl(String objectName) {
        if (objectName == null || objectName.startsWith("http")) return objectName;
        return minioService.presignedUrl(objectName, 24);
    }
}
