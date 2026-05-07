package kz.cyber.acf.core.tournament.service;

import kz.cyber.acf.core.tournament.dto.TournamentDto;
import kz.cyber.acf.core.tournament.dto.TournamentRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.*;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final DefaultDSLContext dsl;

    public List<TournamentDto> findAll() {
        return dsl.select(
                        TOURNAMENT.ID,
                        TOURNAMENT.NAME,
                        TOURNAMENT.LOGO,
                        TOURNAMENT.START_DATE,
                        TOURNAMENT.CAPACITY,
                        TOURNAMENT.PRIZE_MONEY,
                        TOURNAMENT.TOURNAMENT_STATUS,
                        D_TOURNAMENT_STATUS.NAME.as("status_name"),
                        TOURNAMENT.TOURNAMENT_TYPE,
                        D_TOURNAMENT_TYPE.NAME.as("type_name"),
                        TOURNAMENT.CREATED_DATE,
                        TOURNAMENT.UPDATED_DATE
                )
                .from(TOURNAMENT)
                .leftJoin(D_TOURNAMENT_STATUS).on(TOURNAMENT.TOURNAMENT_STATUS.eq(D_TOURNAMENT_STATUS.ID))
                .leftJoin(D_TOURNAMENT_TYPE).on(TOURNAMENT.TOURNAMENT_TYPE.eq(D_TOURNAMENT_TYPE.ID))
                .orderBy(TOURNAMENT.CREATED_DATE.desc())
                .fetch(r -> new TournamentDto(
                        r.get(TOURNAMENT.ID),
                        r.get(TOURNAMENT.NAME),
                        r.get(TOURNAMENT.LOGO),
                        r.get(TOURNAMENT.START_DATE),
                        r.get(TOURNAMENT.CAPACITY),
                        r.get(TOURNAMENT.PRIZE_MONEY),
                        r.get(TOURNAMENT.TOURNAMENT_STATUS),
                        r.get("status_name", String.class),
                        r.get(TOURNAMENT.TOURNAMENT_TYPE),
                        r.get("type_name", String.class),
                        r.get(TOURNAMENT.CREATED_DATE),
                        r.get(TOURNAMENT.UPDATED_DATE)
                ));
    }

    public TournamentDto findById(Long id) {
        return dsl.select(
                        TOURNAMENT.ID,
                        TOURNAMENT.NAME,
                        TOURNAMENT.LOGO,
                        TOURNAMENT.START_DATE,
                        TOURNAMENT.CAPACITY,
                        TOURNAMENT.PRIZE_MONEY,
                        TOURNAMENT.TOURNAMENT_STATUS,
                        D_TOURNAMENT_STATUS.NAME.as("status_name"),
                        TOURNAMENT.TOURNAMENT_TYPE,
                        D_TOURNAMENT_TYPE.NAME.as("type_name"),
                        TOURNAMENT.CREATED_DATE,
                        TOURNAMENT.UPDATED_DATE
                )
                .from(TOURNAMENT)
                .leftJoin(D_TOURNAMENT_STATUS).on(TOURNAMENT.TOURNAMENT_STATUS.eq(D_TOURNAMENT_STATUS.ID))
                .leftJoin(D_TOURNAMENT_TYPE).on(TOURNAMENT.TOURNAMENT_TYPE.eq(D_TOURNAMENT_TYPE.ID))
                .where(TOURNAMENT.ID.eq(id))
                .fetchOptional(r -> new TournamentDto(
                        r.get(TOURNAMENT.ID),
                        r.get(TOURNAMENT.NAME),
                        r.get(TOURNAMENT.LOGO),
                        r.get(TOURNAMENT.START_DATE),
                        r.get(TOURNAMENT.CAPACITY),
                        r.get(TOURNAMENT.PRIZE_MONEY),
                        r.get(TOURNAMENT.TOURNAMENT_STATUS),
                        r.get("status_name", String.class),
                        r.get(TOURNAMENT.TOURNAMENT_TYPE),
                        r.get("type_name", String.class),
                        r.get(TOURNAMENT.CREATED_DATE),
                        r.get(TOURNAMENT.UPDATED_DATE)
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found"));
    }

    public TournamentDto create(TournamentRequest req) {
        Long id = dsl.insertInto(TOURNAMENT)
                .set(TOURNAMENT.NAME, req.getName())
                .set(TOURNAMENT.LOGO, req.getLogo())
                .set(TOURNAMENT.START_DATE, req.getStartDate())
                .set(TOURNAMENT.CAPACITY, req.getCapacity())
                .set(TOURNAMENT.PRIZE_MONEY, req.getPrizeMoney())
                .set(TOURNAMENT.TOURNAMENT_STATUS, req.getTournamentStatusId())
                .set(TOURNAMENT.TOURNAMENT_TYPE, req.getTournamentTypeId())
                .set(TOURNAMENT.CREATED_DATE, OffsetDateTime.now())
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now())
                .returning(TOURNAMENT.ID)
                .fetchOne(TOURNAMENT.ID);
        return findById(id);
    }

    public TournamentDto update(Long id, TournamentRequest req) {
        int updated = dsl.update(TOURNAMENT)
                .set(TOURNAMENT.NAME, req.getName())
                .set(TOURNAMENT.LOGO, req.getLogo())
                .set(TOURNAMENT.START_DATE, req.getStartDate())
                .set(TOURNAMENT.CAPACITY, req.getCapacity())
                .set(TOURNAMENT.PRIZE_MONEY, req.getPrizeMoney())
                .set(TOURNAMENT.TOURNAMENT_STATUS, req.getTournamentStatusId())
                .set(TOURNAMENT.TOURNAMENT_TYPE, req.getTournamentTypeId())
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now())
                .where(TOURNAMENT.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found");
        }
        return findById(id);
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(TOURNAMENT).where(TOURNAMENT.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament not found");
        }
    }
}
