package kz.cyber.acf.core.news.service;

import kz.cyber.acf.core.news.dto.NewsDto;
import kz.cyber.acf.core.news.dto.NewsRequest;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.NEWS;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final DefaultDSLContext dsl;

    public List<NewsDto> findAll() {
        return dsl.selectFrom(NEWS)
                .orderBy(NEWS.CREATED_DATE.desc())
                .fetch(r -> new NewsDto(r.getId(), r.getTitle(), r.getDescription(), r.getImage(), r.getCreatedDate(), r.getUpdatedDate()));
    }

    public NewsDto findById(Long id) {
        return dsl.selectFrom(NEWS)
                .where(NEWS.ID.eq(id))
                .fetchOptional()
                .map(r -> new NewsDto(r.getId(), r.getTitle(), r.getDescription(), r.getImage(), r.getCreatedDate(), r.getUpdatedDate()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "News not found"));
    }

    public NewsDto create(NewsRequest req) {
        var record = dsl.insertInto(NEWS)
                .set(NEWS.TITLE, req.getTitle())
                .set(NEWS.DESCRIPTION, req.getDescription())
                .set(NEWS.CREATED_DATE, OffsetDateTime.now())
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return new NewsDto(record.getId(), record.getTitle(), record.getDescription(), record.getImage(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public NewsDto update(Long id, NewsRequest req) {
        int updated = dsl.update(NEWS)
                .set(NEWS.TITLE, req.getTitle())
                .set(NEWS.DESCRIPTION, req.getDescription())
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .where(NEWS.ID.eq(id))
                .execute();
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "News not found");
        }
        return findById(id);
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(NEWS).where(NEWS.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "News not found");
        }
    }
}
