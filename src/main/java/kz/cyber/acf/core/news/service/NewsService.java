package kz.cyber.acf.core.news.service;

import kz.cyber.acf.core.news.dto.NewsDto;
import kz.cyber.acf.core.news.dto.NewsRequest;
import kz.cyber.acf.core.user.service.UserService;
import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.NEWS;

@Service
@RequiredArgsConstructor
public class NewsService {

    private static final String BUCKET = "news";

    private final DefaultDSLContext dsl;
    private final MinioService minioService;
    private final UserService userService;

    public List<NewsDto> findAll(boolean archived) {
        return dsl.selectFrom(NEWS)
                .where(NEWS.IS_ARCHIVED.eq(archived))
                .orderBy(NEWS.CREATED_DATE.desc())
                .limit(25)
                .fetch(this::toDto);
    }

    public NewsDto findById(Long id, Jwt jwt) {
        NewsDto dto = findByIdRaw(id);
        if (dto.isArchived() && !userService.isAdmin(jwt)) {
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Жаңалық табылмады", "Новость не найдена", "News not found");
        }
        return dto;
    }

    private NewsDto findByIdRaw(Long id) {
        return dsl.selectFrom(NEWS)
                .where(NEWS.ID.eq(id))
                .fetchOptional()
                .map(this::toDto)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Жаңалық табылмады", "Новость не найдена", "News not found"));
    }

    public NewsDto create(NewsRequest req) {
        var record = dsl.insertInto(NEWS)
                .set(NEWS.TITLE, req.getTitle())
                .set(NEWS.DESCRIPTION, req.getDescription())
                .set(NEWS.CREATED_DATE, OffsetDateTime.now())
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return toDto(record);
    }

    public NewsDto update(Long id, NewsRequest req, Jwt jwt) {
        userService.requireAdmin(jwt);
        var record = dsl.update(NEWS)
                .set(NEWS.TITLE, req.getTitle())
                .set(NEWS.DESCRIPTION, req.getDescription())
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .where(NEWS.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                    "Жаңалық табылмады",
                    "Новость не найдена",
                    "News not found");
        return toDto(record);
    }

    public NewsDto uploadImage(Long id, MultipartFile file) {
        String objectName = minioService.upload(BUCKET, file);
        var record = dsl.update(NEWS)
                .set(NEWS.IMAGE, objectName)
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .where(NEWS.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                    "Жаңалық табылмады",
                    "Новость не найдена",
                    "News not found");
        return toDto(record);
    }

    public NewsDto archive(Long id, Jwt jwt) {
        userService.requireAdmin(jwt);
        var record = dsl.update(NEWS)
                .set(NEWS.IS_ARCHIVED, true)
                .set(NEWS.ARCHIVED_DATE, OffsetDateTime.now())
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .where(NEWS.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                    "Жаңалық табылмады",
                    "Новость не найдена",
                    "News not found");
        return toDto(record);
    }

    public NewsDto restore(Long id, Jwt jwt) {
        userService.requireAdmin(jwt);
        var record = dsl.update(NEWS)
                .set(NEWS.IS_ARCHIVED, false)
                .setNull(NEWS.ARCHIVED_DATE)
                .set(NEWS.UPDATED_DATE, OffsetDateTime.now())
                .where(NEWS.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                    "Жаңалық табылмады",
                    "Новость не найдена",
                    "News not found");
        return toDto(record);
    }

    public void delete(Long id, Jwt jwt) {
        userService.requireAdmin(jwt);
        int deleted = dsl.deleteFrom(NEWS).where(NEWS.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                    "Жаңалық табылмады",
                    "Новость не найдена",
                    "News not found");
        }
    }

    private NewsDto toDto(group.bi.postsales.database.tables.records.NewsRecord r) {
        return new NewsDto(
                r.getId(), r.getTitle(), r.getDescription(), resolveUrl(r.getImage()),
                r.getCreatedDate(), r.getUpdatedDate(),
                Boolean.TRUE.equals(r.getIsArchived()), r.getArchivedDate()
        );
    }

    private String resolveUrl(String objectName) {
        if (objectName == null || objectName.startsWith("http")) return objectName;
        return minioService.presignedUrl(objectName, 24);
    }
}
