package kz.cyber.acf.dictionary.city.service;

import kz.cyber.acf.dictionary.city.dto.CityDto;
import kz.cyber.acf.dictionary.city.dto.CityRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.D_CITY;

@Service
@RequiredArgsConstructor
public class CityService {

    private final DefaultDSLContext dsl;

    public List<CityDto> findAll() {
        return dsl.selectFrom(D_CITY)
                .where(D_CITY.IS_ACTIVE.isTrue())
                .fetch(r -> new CityDto(r.getId(), r.getNameRu(), r.getNameKk(), r.getNameEn(),
                        r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()));
    }

    public CityDto findById(Long id) {
        return dsl.selectFrom(D_CITY)
                .where(D_CITY.ID.eq(id))
                .fetchOptional()
                .map(r -> new CityDto(r.getId(), r.getNameRu(), r.getNameKk(), r.getNameEn(),
                        r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found"));
    }

    public CityDto create(CityRequest req) {
        var record = dsl.insertInto(D_CITY)
                .set(D_CITY.NAME_RU, req.getNameRu())
                .set(D_CITY.NAME_KK, req.getNameKk())
                .set(D_CITY.NAME_EN, req.getNameEn())
                .set(D_CITY.IS_ACTIVE, req.getIsActive() != null ? req.getIsActive() : true)
                .set(D_CITY.CREATED_DATE, OffsetDateTime.now())
                .set(D_CITY.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return new CityDto(record.getId(), record.getNameRu(), record.getNameKk(), record.getNameEn(),
                record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public CityDto update(Long id, CityRequest req) {
        var record = dsl.update(D_CITY)
                .set(D_CITY.NAME_RU, req.getNameRu())
                .set(D_CITY.NAME_KK, req.getNameKk())
                .set(D_CITY.NAME_EN, req.getNameEn())
                .set(D_CITY.IS_ACTIVE, req.getIsActive())
                .set(D_CITY.UPDATED_DATE, OffsetDateTime.now())
                .where(D_CITY.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found");
        return new CityDto(record.getId(), record.getNameRu(), record.getNameKk(), record.getNameEn(),
                record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(D_CITY).where(D_CITY.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found");
        }
    }
}
