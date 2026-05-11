package kz.cyber.acf.core.registration.service;

import kz.cyber.acf.core.registration.dto.ParticipantDto;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static group.bi.postsales.database.Tables.*;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final DefaultDSLContext dsl;
    private final RegistrationLogService registrationLogService;

    public ParticipantDto register(Long tournamentId, Long userId, String psn) {
        if (psn == null || psn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PSN is mandatory");
        }

        var tournamentRow = dsl.select(TOURNAMENT.CAPACITY, D_TOURNAMENT_STATUS.NAME.as("status_name"))
                .from(TOURNAMENT)
                .join(D_TOURNAMENT_STATUS).on(D_TOURNAMENT_STATUS.ID.eq(TOURNAMENT.TOURNAMENT_STATUS))
                .where(TOURNAMENT.ID.eq(tournamentId))
                .fetchOne();
        if (tournamentRow == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found");
        }
        if (!"Будущие".equals(tournamentRow.get("status_name", String.class))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is only allowed for upcoming tournaments");
        }

        var userRow = dsl.select(
                        USER.IS_VERIFIED,
                        DSL.field(DSL.exists(
                                DSL.select(DSL.one())
                                        .from(TOURNAMENT_REGISTRATION)
                                        .where(TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId)
                                                .and(TOURNAMENT_REGISTRATION.USER_ID.eq(userId)))
                        )).as("already_registered"))
                .from(USER)
                .where(USER.ID.eq(userId))
                .fetchOne();
        if (userRow == null || !Boolean.TRUE.equals(userRow.get(USER.IS_VERIFIED))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Document verification required to register for tournaments");
        }
        if (Boolean.TRUE.equals(userRow.get("already_registered", Boolean.class))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already registered for this tournament");
        }

        Integer capacity = tournamentRow.get(TOURNAMENT.CAPACITY);
        if (capacity != null) {
            int currentCount = dsl.fetchCount(TOURNAMENT_REGISTRATION, TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId));
            if (currentCount >= capacity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tournament is full");
            }
        }

        dsl.insertInto(TOURNAMENT_REGISTRATION)
                .set(TOURNAMENT_REGISTRATION.TOURNAMENT_ID, tournamentId)
                .set(TOURNAMENT_REGISTRATION.USER_ID, userId)
                .set(TOURNAMENT_REGISTRATION.PSN, psn.trim())
                .execute();

        registrationLogService.log(tournamentId, userId, RegistrationLogService.ACTION_REGISTER, psn.trim());

        return dsl.select(
                        TOURNAMENT_REGISTRATION.ID,
                        TOURNAMENT_REGISTRATION.TOURNAMENT_ID,
                        TOURNAMENT_REGISTRATION.USER_ID,
                        USER.USERNAME,
                        USER.FIRST_NAME,
                        USER.LAST_NAME,
                        TOURNAMENT_REGISTRATION.REGISTERED_DATE,
                        TOURNAMENT_REGISTRATION.PSN
                )
                .from(TOURNAMENT_REGISTRATION)
                .join(USER).on(TOURNAMENT_REGISTRATION.USER_ID.eq(USER.ID))
                .where(TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_REGISTRATION.USER_ID.eq(userId)))
                .fetchOptional(r -> new ParticipantDto(
                        r.get(TOURNAMENT_REGISTRATION.ID),
                        r.get(TOURNAMENT_REGISTRATION.TOURNAMENT_ID),
                        r.get(TOURNAMENT_REGISTRATION.USER_ID),
                        r.get(USER.USERNAME),
                        r.get(USER.FIRST_NAME),
                        r.get(USER.LAST_NAME),
                        r.get(TOURNAMENT_REGISTRATION.REGISTERED_DATE),
                        r.get(TOURNAMENT_REGISTRATION.PSN)
                ))
                .orElseThrow();
    }

    public void unregister(Long tournamentId, Long userId) {
        String statusName = dsl.select(D_TOURNAMENT_STATUS.NAME)
                .from(TOURNAMENT)
                .join(D_TOURNAMENT_STATUS).on(D_TOURNAMENT_STATUS.ID.eq(TOURNAMENT.TOURNAMENT_STATUS))
                .where(TOURNAMENT.ID.eq(tournamentId))
                .fetchOneInto(String.class);
        if (statusName == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found");
        }
        if (!"Будущие".equals(statusName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unregistration is only allowed for upcoming tournaments");
        }

        String psn = dsl.select(TOURNAMENT_REGISTRATION.PSN)
                .from(TOURNAMENT_REGISTRATION)
                .where(TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_REGISTRATION.USER_ID.eq(userId)))
                .fetchOneInto(String.class);

        int deleted = dsl.deleteFrom(TOURNAMENT_REGISTRATION)
                .where(TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_REGISTRATION.USER_ID.eq(userId)))
                .execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
        }

        registrationLogService.log(tournamentId, userId, RegistrationLogService.ACTION_UNREGISTER, psn);
    }

    public List<ParticipantDto> getParticipants(Long tournamentId) {
        return dsl.select(
                        TOURNAMENT_REGISTRATION.ID,
                        TOURNAMENT_REGISTRATION.TOURNAMENT_ID,
                        TOURNAMENT_REGISTRATION.USER_ID,
                        USER.USERNAME,
                        USER.FIRST_NAME,
                        USER.LAST_NAME,
                        TOURNAMENT_REGISTRATION.REGISTERED_DATE,
                        TOURNAMENT_REGISTRATION.PSN
                )
                .from(TOURNAMENT_REGISTRATION)
                .join(USER).on(TOURNAMENT_REGISTRATION.USER_ID.eq(USER.ID))
                .where(TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId))
                .orderBy(TOURNAMENT_REGISTRATION.REGISTERED_DATE.asc())
                .fetch(r -> new ParticipantDto(
                        r.get(TOURNAMENT_REGISTRATION.ID),
                        r.get(TOURNAMENT_REGISTRATION.TOURNAMENT_ID),
                        r.get(TOURNAMENT_REGISTRATION.USER_ID),
                        r.get(USER.USERNAME),
                        r.get(USER.FIRST_NAME),
                        r.get(USER.LAST_NAME),
                        r.get(TOURNAMENT_REGISTRATION.REGISTERED_DATE),
                        r.get(TOURNAMENT_REGISTRATION.PSN)
                ));
    }
}
