package kz.cyber.acf.dictionary.club.service;

import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.dictionary.club.dto.ClubDto;
import kz.cyber.acf.dictionary.club.dto.ClubRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.D_CLUB;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final DefaultDSLContext dsl;

    public PageResponse<ClubDto> findAll(int page, int size) {
        long total = dsl.selectCount()
                .from(D_CLUB)
                .where(D_CLUB.IS_ACTIVE.isTrue())
                .fetchOne(0, Long.class);
        List<ClubDto> content = dsl.selectFrom(D_CLUB)
                .where(D_CLUB.IS_ACTIVE.isTrue())
                .orderBy(D_CLUB.ID.asc())
                .limit(size)
                .offset((long) page * size)
                .fetch(r -> new ClubDto(r.getId(), r.getNameRu(), r.getNameKk(), r.getNameEn(),
                        r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()));
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    public ClubDto findById(Long id) {
        return dsl.selectFrom(D_CLUB)
                .where(D_CLUB.ID.eq(id))
                .fetchOptional()
                .map(r -> new ClubDto(r.getId(), r.getNameRu(), r.getNameKk(), r.getNameEn(),
                        r.getIsActive(), r.getCreatedDate(), r.getUpdatedDate()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Клуб табылмады", "Клуб не найден", "Club not found"));
    }

    public ClubDto create(ClubRequest req) {
        var record = dsl.insertInto(D_CLUB)
                .set(D_CLUB.NAME_RU, req.getNameRu())
                .set(D_CLUB.NAME_KK, req.getNameKk())
                .set(D_CLUB.NAME_EN, req.getNameEn())
                .set(D_CLUB.IS_ACTIVE, req.getIsActive() != null ? req.getIsActive() : true)
                .set(D_CLUB.CREATED_DATE, OffsetDateTime.now())
                .set(D_CLUB.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return new ClubDto(record.getId(), record.getNameRu(), record.getNameKk(), record.getNameEn(),
                record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public ClubDto update(Long id, ClubRequest req) {
        var record = dsl.update(D_CLUB)
                .set(D_CLUB.NAME_RU, req.getNameRu())
                .set(D_CLUB.NAME_KK, req.getNameKk())
                .set(D_CLUB.NAME_EN, req.getNameEn())
                .set(D_CLUB.IS_ACTIVE, req.getIsActive())
                .set(D_CLUB.UPDATED_DATE, OffsetDateTime.now())
                .where(D_CLUB.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Клуб табылмады",
                "Клуб не найден",
                "Club not found");
        return new ClubDto(record.getId(), record.getNameRu(), record.getNameKk(), record.getNameEn(),
                record.getIsActive(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public void delete(Long id) {
        int updated = dsl.update(D_CLUB)
                .set(D_CLUB.IS_ACTIVE, false)
                .set(D_CLUB.UPDATED_DATE, OffsetDateTime.now())
                .where(D_CLUB.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                "Клуб табылмады",
                "Клуб не найден",
                "Club not found");
        }
    }
}
