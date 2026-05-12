package kz.cyber.acf.core.user.service;

import kz.cyber.acf.core.user.dto.*;
import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String BUCKET_USERS = "users";
    private static final String BUCKET_VERIFICATIONS = "verifications";
    private static final long STATUS_COMPLETED = 3L;

    private final DefaultDSLContext dsl;
    private final MinioService minioService;
    private final KzIdDocumentValidator kzIdDocumentValidator;

    public List<UserDto> findAll() {
        return dsl.select(
                        USER.asterisk(),
                        D_CITY.NAME_RU.as("city_name_ru"),
                        D_CITY.NAME_KK.as("city_name_kk"),
                        D_CITY.NAME_EN.as("city_name_en"),
                        D_CLUB.NAME_RU.as("club_name_ru")
                )
                .from(USER)
                .leftJoin(D_CITY).on(USER.CITY_ID.eq(D_CITY.ID))
                .leftJoin(D_CLUB).on(USER.CLUB_ID.eq(D_CLUB.ID))
                .fetch(this::mapUser);
    }

    public UserDto findByUsername(String username) {
        return dsl.select(
                        USER.asterisk(),
                        D_CITY.NAME_RU.as("city_name_ru"),
                        D_CITY.NAME_KK.as("city_name_kk"),
                        D_CITY.NAME_EN.as("city_name_en"),
                        D_CLUB.NAME_RU.as("club_name_ru")
                )
                .from(USER)
                .leftJoin(D_CITY).on(USER.CITY_ID.eq(D_CITY.ID))
                .leftJoin(D_CLUB).on(USER.CLUB_ID.eq(D_CLUB.ID))
                .where(USER.USERNAME.eq(username))
                .fetchOptional()
                .map(this::mapUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Пайдаланушы табылмады", "Пользователь не найден", "User not found"));
    }

    public UserDto findById(Long id) {
        return dsl.select(
                        USER.asterisk(),
                        D_CITY.NAME_RU.as("city_name_ru"),
                        D_CITY.NAME_KK.as("city_name_kk"),
                        D_CITY.NAME_EN.as("city_name_en"),
                        D_CLUB.NAME_RU.as("club_name_ru")
                )
                .from(USER)
                .leftJoin(D_CITY).on(USER.CITY_ID.eq(D_CITY.ID))
                .leftJoin(D_CLUB).on(USER.CLUB_ID.eq(D_CLUB.ID))
                .where(USER.ID.eq(id))
                .fetchOptional()
                .map(this::mapUser)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Пайдаланушы табылмады", "Пользователь не найден", "User not found"));
    }

    public UserDto update(Long id, UpdateUserRequest req) {
        int updated = dsl.update(USER)
                .set(USER.FIRST_NAME, req.getFirstName())
                .set(USER.LAST_NAME, req.getLastName())
                .set(USER.BIRTH_DATE, req.getBirthDate())
                .set(USER.CITY_ID, req.getCityId())
                .set(USER.CLUB_ID, req.getClubId())
                .set(USER.UPDATED_DATE, OffsetDateTime.now())
                .where(USER.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Пайдаланушы табылмады",
                    "Пользователь не найден",
                    "User not found");
        }
        return findById(id);
    }

    public UserDto uploadPhoto(Long id, MultipartFile file) {
        String objectName = minioService.upload(BUCKET_USERS, file);
        dsl.update(USER)
                .set(USER.PHOTO, objectName)
                .set(USER.UPDATED_DATE, OffsetDateTime.now())
                .where(USER.ID.eq(id))
                .execute();
        return findById(id);
    }

    public UserDto uploadVerificationDocument(Long id, MultipartFile file) {
        kzIdDocumentValidator.validate(file);
        String objectName = minioService.upload(BUCKET_VERIFICATIONS, file);
        dsl.update(USER)
                .set(USER.VERIFICATION_DOCUMENT, objectName)
                .set(USER.IS_VERIFIED, true)
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
            throw new AppException(HttpStatus.FORBIDDEN,
                    "Администратор рұқсаты қажет",
                    "Требуется доступ администратора",
                    "Admin access required");
        }
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(USER).where(USER.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Пайдаланушы табылмады",
                    "Пользователь не найден",
                    "User not found");
        }
    }

    public PageResponse<PlayerTournamentHistoryDto> getTournamentHistory(Long userId, int page, int size) {
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
                        D_TOURNAMENT_STATUS.NAME_RU.as("status_name_ru"),
                        D_TOURNAMENT_STATUS.NAME_KK.as("status_name_kk"),
                        D_TOURNAMENT_STATUS.NAME_EN.as("status_name_en"),
                        TOURNAMENT.TOURNAMENT_TYPE,
                        D_TOURNAMENT_TYPE.NAME_RU.as("type_name_ru"),
                        D_TOURNAMENT_TYPE.NAME_KK.as("type_name_kk"),
                        D_TOURNAMENT_TYPE.NAME_EN.as("type_name_en"),
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
                        r.get("status_name_ru", String.class),
                        r.get("status_name_kk", String.class),
                        r.get("status_name_en", String.class),
                        r.get(TOURNAMENT.TOURNAMENT_TYPE),
                        r.get("type_name_ru", String.class),
                        r.get("type_name_kk", String.class),
                        r.get("type_name_en", String.class),
                        r.get(TOURNAMENT.FORMAT),
                        r.get(TOURNAMENT_RESULT.PLACE),
                        r.get(TOURNAMENT_RESULT.SCORE)
                ));

        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    public PageResponse<PlayerMatchDto> getMatchHistory(Long userId, int page, int size) {
        int offset = page * size;

        var condition = TOURNAMENT_MATCH.PARTICIPANT1_ID.eq(userId)
                .or(TOURNAMENT_MATCH.PARTICIPANT2_ID.eq(userId))
                .and(TOURNAMENT_MATCH.STATUS.eq("COMPLETED"));

        long total = dsl.selectCount()
                .from(TOURNAMENT_MATCH)
                .where(condition)
                .fetchOne(0, Long.class);

        var P1 = USER.as("p1");
        var P2 = USER.as("p2");

        List<PlayerMatchDto> content = dsl.select(
                        TOURNAMENT_MATCH.ID,
                        TOURNAMENT_MATCH.TOURNAMENT_ID,
                        TOURNAMENT.NAME.as("tournament_name"),
                        TOURNAMENT_MATCH.PARTICIPANT1_ID,
                        TOURNAMENT_MATCH.PARTICIPANT2_ID,
                        TOURNAMENT_MATCH.SCORE1,
                        TOURNAMENT_MATCH.SCORE2,
                        TOURNAMENT_MATCH.WINNER_ID,
                        TOURNAMENT_MATCH.STATUS,
                        TOURNAMENT_MATCH.UPDATED_DATE,
                        P1.USERNAME.as("p1_username"),
                        P1.PHOTO.as("p1_photo"),
                        P2.USERNAME.as("p2_username"),
                        P2.PHOTO.as("p2_photo")
                )
                .from(TOURNAMENT_MATCH)
                .join(TOURNAMENT).on(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(TOURNAMENT.ID))
                .leftJoin(P1).on(TOURNAMENT_MATCH.PARTICIPANT1_ID.eq(P1.ID))
                .leftJoin(P2).on(TOURNAMENT_MATCH.PARTICIPANT2_ID.eq(P2.ID))
                .where(condition)
                .orderBy(TOURNAMENT_MATCH.UPDATED_DATE.desc())
                .limit(size)
                .offset(offset)
                .fetch(r -> {
                    boolean isP1 = userId.equals(r.get(TOURNAMENT_MATCH.PARTICIPANT1_ID));
                    Long opponentId = isP1
                            ? r.get(TOURNAMENT_MATCH.PARTICIPANT2_ID)
                            : r.get(TOURNAMENT_MATCH.PARTICIPANT1_ID);
                    String opponentUsername = isP1
                            ? r.get("p2_username", String.class)
                            : r.get("p1_username", String.class);
                    String opponentPhoto = isP1
                            ? resolveUrl(r.get("p2_photo", String.class))
                            : resolveUrl(r.get("p1_photo", String.class));
                    Integer myScore = isP1 ? r.get(TOURNAMENT_MATCH.SCORE1) : r.get(TOURNAMENT_MATCH.SCORE2);
                    Integer opponentScore = isP1 ? r.get(TOURNAMENT_MATCH.SCORE2) : r.get(TOURNAMENT_MATCH.SCORE1);

                    Long winnerId = r.get(TOURNAMENT_MATCH.WINNER_ID);
                    String result = winnerId == null ? "DRAW"
                            : winnerId.equals(userId) ? "WIN" : "LOSS";

                    return new PlayerMatchDto(
                            r.get(TOURNAMENT_MATCH.ID),
                            r.get(TOURNAMENT_MATCH.TOURNAMENT_ID),
                            r.get("tournament_name", String.class),
                            opponentId,
                            opponentUsername,
                            opponentPhoto,
                            myScore != null ? myScore : 0,
                            opponentScore != null ? opponentScore : 0,
                            result,
                            r.get(TOURNAMENT_MATCH.STATUS),
                            r.get(TOURNAMENT_MATCH.UPDATED_DATE)
                    );
                });

        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private UserDto mapUser(org.jooq.Record r) {
        return new UserDto(
                r.get(USER.ID), r.get(USER.USERNAME), r.get(USER.PHONE_NUMBER),
                r.get(USER.FIRST_NAME), r.get(USER.LAST_NAME), r.get(USER.BIRTH_DATE),
                r.get(USER.IS_ADMIN), resolveUrl(r.get(USER.PHOTO)),
                r.get(USER.CITY_ID), r.get("city_name_ru", String.class),
                r.get("city_name_kk", String.class), r.get("city_name_en", String.class),
                r.get(USER.IS_VERIFIED),
                r.get(USER.CLUB_ID), r.get("club_name_ru", String.class),
                r.get(USER.CREATED_DATE), r.get(USER.UPDATED_DATE)
        );
    }

    private String resolveUrl(String objectName) {
        if (objectName == null || objectName.startsWith("http")) return objectName;
        return minioService.presignedUrl(objectName, 24);
    }
}
