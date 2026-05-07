package kz.cyber.acf.core.result.service;

import kz.cyber.acf.core.result.dto.ResultDto;
import kz.cyber.acf.core.result.dto.ResultRequest;
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
public class ResultService {

    private final DefaultDSLContext dsl;

    public List<ResultDto> getResults(Long tournamentId) {
        return dsl.select(
                        TOURNAMENT_RESULT.ID,
                        TOURNAMENT_RESULT.TOURNAMENT_ID,
                        TOURNAMENT_RESULT.USER_ID,
                        USER.USERNAME,
                        USER.FIRST_NAME,
                        USER.LAST_NAME,
                        TOURNAMENT_RESULT.PLACE,
                        TOURNAMENT_RESULT.SCORE,
                        TOURNAMENT_RESULT.CREATED_DATE
                )
                .from(TOURNAMENT_RESULT)
                .join(USER).on(TOURNAMENT_RESULT.USER_ID.eq(USER.ID))
                .where(TOURNAMENT_RESULT.TOURNAMENT_ID.eq(tournamentId))
                .orderBy(TOURNAMENT_RESULT.PLACE.asc())
                .fetch(r -> new ResultDto(
                        r.get(TOURNAMENT_RESULT.ID),
                        r.get(TOURNAMENT_RESULT.TOURNAMENT_ID),
                        r.get(TOURNAMENT_RESULT.USER_ID),
                        r.get(USER.USERNAME),
                        r.get(USER.FIRST_NAME),
                        r.get(USER.LAST_NAME),
                        r.get(TOURNAMENT_RESULT.PLACE),
                        r.get(TOURNAMENT_RESULT.SCORE),
                        r.get(TOURNAMENT_RESULT.CREATED_DATE)
                ));
    }

    public ResultDto create(Long tournamentId, ResultRequest req) {
        dsl.insertInto(TOURNAMENT_RESULT)
                .set(TOURNAMENT_RESULT.TOURNAMENT_ID, tournamentId)
                .set(TOURNAMENT_RESULT.USER_ID, req.getUserId())
                .set(TOURNAMENT_RESULT.PLACE, req.getPlace())
                .set(TOURNAMENT_RESULT.SCORE, req.getScore())
                .set(TOURNAMENT_RESULT.CREATED_DATE, OffsetDateTime.now())
                .execute();
        return getResults(tournamentId).stream()
                .filter(r -> r.getUserId().equals(req.getUserId()))
                .findFirst()
                .orElseThrow();
    }

    public ResultDto update(Long tournamentId, Long userId, ResultRequest req) {
        int updated = dsl.update(TOURNAMENT_RESULT)
                .set(TOURNAMENT_RESULT.PLACE, req.getPlace())
                .set(TOURNAMENT_RESULT.SCORE, req.getScore())
                .where(TOURNAMENT_RESULT.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_RESULT.USER_ID.eq(userId)))
                .execute();
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
        }
        return getResults(tournamentId).stream()
                .filter(r -> r.getUserId().equals(userId))
                .findFirst()
                .orElseThrow();
    }

    public void delete(Long tournamentId, Long userId) {
        int deleted = dsl.deleteFrom(TOURNAMENT_RESULT)
                .where(TOURNAMENT_RESULT.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_RESULT.USER_ID.eq(userId)))
                .execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
        }
    }
}
