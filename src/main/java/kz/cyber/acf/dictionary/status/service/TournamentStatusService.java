package kz.cyber.acf.dictionary.status.service;

import group.bi.postsales.database.tables.records.DTournamentStatusRecord;
import kz.cyber.acf.dictionary.dto.DictionaryDto;
import kz.cyber.acf.dictionary.dto.DictionaryRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.D_TOURNAMENT_STATUS;

@Service
@RequiredArgsConstructor
public class TournamentStatusService {

    private final DefaultDSLContext dsl;

    public List<DictionaryDto> findAll() {
        return dsl.selectFrom(D_TOURNAMENT_STATUS)
                .where(D_TOURNAMENT_STATUS.IS_ACTIVE.isTrue())
                .fetch(this::toDto);
    }

    public DictionaryDto findById(Long id) {
        return dsl.selectFrom(D_TOURNAMENT_STATUS)
                .where(D_TOURNAMENT_STATUS.ID.eq(id))
                .fetchOptional()
                .map(this::toDto)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Турнир статусы табылмады", "Статус турнира не найден", "Tournament status not found"));
    }

    public DictionaryDto create(DictionaryRequest req) {
        var record = dsl.insertInto(D_TOURNAMENT_STATUS)
                .set(D_TOURNAMENT_STATUS.NAME_RU, req.getNameRu())
                .set(D_TOURNAMENT_STATUS.NAME_KK, req.getNameKk())
                .set(D_TOURNAMENT_STATUS.NAME_EN, req.getNameEn())
                .set(D_TOURNAMENT_STATUS.IS_ACTIVE, req.getIsActive() != null ? req.getIsActive() : true)
                .set(D_TOURNAMENT_STATUS.CREATED_DATE, OffsetDateTime.now())
                .set(D_TOURNAMENT_STATUS.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return toDto(record);
    }

    public DictionaryDto update(Long id, DictionaryRequest req) {
        var record = dsl.update(D_TOURNAMENT_STATUS)
                .set(D_TOURNAMENT_STATUS.NAME_RU, req.getNameRu())
                .set(D_TOURNAMENT_STATUS.NAME_KK, req.getNameKk())
                .set(D_TOURNAMENT_STATUS.NAME_EN, req.getNameEn())
                .set(D_TOURNAMENT_STATUS.IS_ACTIVE, req.getIsActive())
                .set(D_TOURNAMENT_STATUS.UPDATED_DATE, OffsetDateTime.now())
                .where(D_TOURNAMENT_STATUS.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир статусы табылмады",
                "Статус турнира не найден",
                "Tournament status not found");
        return toDto(record);
    }

    public void delete(Long id) {
        int updated = dsl.update(D_TOURNAMENT_STATUS)
                .set(D_TOURNAMENT_STATUS.IS_ACTIVE, false)
                .set(D_TOURNAMENT_STATUS.UPDATED_DATE, OffsetDateTime.now())
                .where(D_TOURNAMENT_STATUS.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир статусы табылмады",
                "Статус турнира не найден",
                "Tournament status not found");
        }
    }

    private DictionaryDto toDto(DTournamentStatusRecord r) {
        return new DictionaryDto(r.getId(), r.getNameRu(), r.getNameKk(), r.getNameEn(),
                r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate());
    }
}
