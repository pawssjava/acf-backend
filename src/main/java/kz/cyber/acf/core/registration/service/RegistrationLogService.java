package kz.cyber.acf.core.registration.service;

import kz.cyber.acf.core.registration.dto.RegistrationLogDto;
import kz.cyber.acf.core.user.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.*;

@Service
@RequiredArgsConstructor
public class RegistrationLogService {

    public static final String ACTION_REGISTER   = "REGISTER";
    public static final String ACTION_UNREGISTER = "UNREGISTER";

    private final DefaultDSLContext dsl;

    public void log(Long tournamentId, Long userId, String action, String psn) {
        dsl.insertInto(TOURNAMENT_REGISTRATION_LOG)
                .set(TOURNAMENT_REGISTRATION_LOG.TOURNAMENT_ID, tournamentId)
                .set(TOURNAMENT_REGISTRATION_LOG.USER_ID, userId)
                .set(TOURNAMENT_REGISTRATION_LOG.ACTION, action)
                .set(TOURNAMENT_REGISTRATION_LOG.PSN, psn)
                .set(TOURNAMENT_REGISTRATION_LOG.CREATED_DATE, OffsetDateTime.now())
                .execute();
    }

    public PageResponse<RegistrationLogDto> getLogs(Long tournamentId, String search, int page, int size) {
        int offset = page * size;

        Condition filter = buildFilter(tournamentId, search);

        long total = dsl.selectCount()
                .from(TOURNAMENT_REGISTRATION_LOG)
                .join(USER).on(TOURNAMENT_REGISTRATION_LOG.USER_ID.eq(USER.ID))
                .where(filter)
                .fetchOne(0, Long.class);

        List<RegistrationLogDto> content = dsl.select(
                        TOURNAMENT_REGISTRATION_LOG.ID,
                        TOURNAMENT_REGISTRATION_LOG.TOURNAMENT_ID,
                        TOURNAMENT_REGISTRATION_LOG.USER_ID,
                        USER.USERNAME,
                        USER.FIRST_NAME,
                        USER.LAST_NAME,
                        TOURNAMENT_REGISTRATION_LOG.ACTION,
                        TOURNAMENT_REGISTRATION_LOG.PSN,
                        TOURNAMENT_REGISTRATION_LOG.CREATED_DATE
                )
                .from(TOURNAMENT_REGISTRATION_LOG)
                .join(USER).on(TOURNAMENT_REGISTRATION_LOG.USER_ID.eq(USER.ID))
                .where(filter)
                .orderBy(TOURNAMENT_REGISTRATION_LOG.CREATED_DATE.desc())
                .limit(size)
                .offset(offset)
                .fetch(r -> new RegistrationLogDto(
                        r.get(TOURNAMENT_REGISTRATION_LOG.ID),
                        r.get(TOURNAMENT_REGISTRATION_LOG.TOURNAMENT_ID),
                        r.get(TOURNAMENT_REGISTRATION_LOG.USER_ID),
                        r.get(USER.USERNAME),
                        r.get(USER.FIRST_NAME),
                        r.get(USER.LAST_NAME),
                        r.get(TOURNAMENT_REGISTRATION_LOG.ACTION),
                        r.get(TOURNAMENT_REGISTRATION_LOG.PSN),
                        r.get(TOURNAMENT_REGISTRATION_LOG.CREATED_DATE)
                ));

        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private Condition buildFilter(Long tournamentId, String search) {
        Condition base = TOURNAMENT_REGISTRATION_LOG.TOURNAMENT_ID.eq(tournamentId);
        if (search == null || search.isBlank()) return base;

        String pattern = "%" + search.trim().toLowerCase() + "%";
        return base.and(
                DSL.lower(TOURNAMENT_REGISTRATION_LOG.PSN).like(pattern)
                        .or(DSL.lower(USER.USERNAME).like(pattern))
                        .or(DSL.lower(DSL.concat(USER.FIRST_NAME, DSL.val(" "), USER.LAST_NAME)).like(pattern))
        );
    }
}
