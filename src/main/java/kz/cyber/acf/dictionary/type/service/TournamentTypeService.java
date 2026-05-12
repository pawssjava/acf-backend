package kz.cyber.acf.dictionary.type.service;

import kz.cyber.acf.dictionary.dto.DictionaryDto;
import kz.cyber.acf.dictionary.dto.DictionaryRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.D_TOURNAMENT_TYPE;

@Service
@RequiredArgsConstructor
public class TournamentTypeService {

    private final DefaultDSLContext dsl;

    public List<DictionaryDto> findAll() {
        return dsl.selectFrom(D_TOURNAMENT_TYPE)
                .fetch(r -> new DictionaryDto(r.getId(), r.getName(), r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()));
    }

    public DictionaryDto findById(Long id) {
        return dsl.selectFrom(D_TOURNAMENT_TYPE)
                .where(D_TOURNAMENT_TYPE.ID.eq(id))
                .fetchOptional()
                .map(r -> new DictionaryDto(r.getId(), r.getName(), r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Турнир түрі табылмады", "Тип турнира не найден", "Tournament type not found"));
    }

    public DictionaryDto create(DictionaryRequest req) {
        var record = dsl.insertInto(D_TOURNAMENT_TYPE)
                .set(D_TOURNAMENT_TYPE.NAME, req.getName())
                .set(D_TOURNAMENT_TYPE.IS_ACTIVE, req.getIsActive() != null ? req.getIsActive() : true)
                .set(D_TOURNAMENT_TYPE.CREATED_DATE, OffsetDateTime.now())
                .set(D_TOURNAMENT_TYPE.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return new DictionaryDto(record.getId(), record.getName(), record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public DictionaryDto update(Long id, DictionaryRequest req) {
        var record = dsl.update(D_TOURNAMENT_TYPE)
                .set(D_TOURNAMENT_TYPE.NAME, req.getName())
                .set(D_TOURNAMENT_TYPE.IS_ACTIVE, req.getIsActive())
                .set(D_TOURNAMENT_TYPE.UPDATED_DATE, OffsetDateTime.now())
                .where(D_TOURNAMENT_TYPE.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир түрі табылмады",
                "Тип турнира не найден",
                "Tournament type not found");
        return new DictionaryDto(record.getId(), record.getName(), record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(D_TOURNAMENT_TYPE).where(D_TOURNAMENT_TYPE.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир түрі табылмады",
                "Тип турнира не найден",
                "Tournament type not found");
        }
    }
}
