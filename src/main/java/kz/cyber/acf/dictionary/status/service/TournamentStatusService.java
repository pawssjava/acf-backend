package kz.cyber.acf.dictionary.status.service;

import kz.cyber.acf.dictionary.dto.DictionaryDto;
import kz.cyber.acf.dictionary.dto.DictionaryRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.D_TOURNAMENT_STATUS;

@Service
@RequiredArgsConstructor
public class TournamentStatusService {

    private final DefaultDSLContext dsl;

    public List<DictionaryDto> findAll() {
        return dsl.selectFrom(D_TOURNAMENT_STATUS)
                .fetch(r -> new DictionaryDto(r.getId(), r.getName(), r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()));
    }

    public DictionaryDto findById(Long id) {
        return dsl.selectFrom(D_TOURNAMENT_STATUS)
                .where(D_TOURNAMENT_STATUS.ID.eq(id))
                .fetchOptional()
                .map(r -> new DictionaryDto(r.getId(), r.getName(), r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament status not found"));
    }

    public DictionaryDto create(DictionaryRequest req) {
        var record = dsl.insertInto(D_TOURNAMENT_STATUS)
                .set(D_TOURNAMENT_STATUS.NAME, req.getName())
                .set(D_TOURNAMENT_STATUS.IS_ACTIVE, req.getIsActive() != null ? req.getIsActive() : true)
                .set(D_TOURNAMENT_STATUS.CREATED_DATE, OffsetDateTime.now())
                .set(D_TOURNAMENT_STATUS.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return new DictionaryDto(record.getId(), record.getName(), record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public DictionaryDto update(Long id, DictionaryRequest req) {
        int updated = dsl.update(D_TOURNAMENT_STATUS)
                .set(D_TOURNAMENT_STATUS.NAME, req.getName())
                .set(D_TOURNAMENT_STATUS.IS_ACTIVE, req.getIsActive())
                .set(D_TOURNAMENT_STATUS.UPDATED_DATE, OffsetDateTime.now())
                .where(D_TOURNAMENT_STATUS.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament status not found");
        }
        return findById(id);
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(D_TOURNAMENT_STATUS).where(D_TOURNAMENT_STATUS.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournament status not found");
        }
    }
}
