package kz.cyber.acf.core.education.service;

import group.bi.postsales.database.tables.records.EducationMaterialRecord;
import kz.cyber.acf.core.education.dto.EducationMaterialDto;
import kz.cyber.acf.core.education.dto.EducationMaterialRequest;
import kz.cyber.acf.core.user.dto.PageResponse;
import kz.cyber.acf.core.user.service.UserService;
import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DSL;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.EDUCATION_MATERIAL;

@Service
@RequiredArgsConstructor
public class EducationMaterialService {

    private static final String FOLDER = "education";

    private final DefaultDSLContext dsl;
    private final MinioService minioService;
    private final UserService userService;

    public PageResponse<EducationMaterialDto> findAll(String category, int page, int size) {
        Condition condition = category != null ? EDUCATION_MATERIAL.CATEGORY.eq(category) : DSL.trueCondition();

        int offset = page * size;
        List<EducationMaterialDto> content = dsl.selectFrom(EDUCATION_MATERIAL)
                .where(condition)
                .orderBy(EDUCATION_MATERIAL.ORDERING.asc(), EDUCATION_MATERIAL.CREATED_DATE.desc())
                .limit(size)
                .offset(offset)
                .fetch(this::toDto);

        long total = dsl.fetchCount(dsl.selectFrom(EDUCATION_MATERIAL).where(condition));
        int totalPages = (int) Math.ceil((double) total / size);

        return new PageResponse<>(content, page, size, total, totalPages);
    }

    public EducationMaterialDto findById(Long id) {
        return dsl.selectFrom(EDUCATION_MATERIAL)
                .where(EDUCATION_MATERIAL.ID.eq(id))
                .fetchOptional()
                .map(this::toDto)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Оқу материалы табылмады", "Учебный материал не найден", "Education material not found"));
    }

    public EducationMaterialDto create(String username, EducationMaterialRequest req) {
        userService.requireAdmin(username);
        var record = dsl.insertInto(EDUCATION_MATERIAL)
                .set(EDUCATION_MATERIAL.TITLE, req.getTitle())
                .set(EDUCATION_MATERIAL.DESCRIPTION, req.getDescription())
                .set(EDUCATION_MATERIAL.CATEGORY, req.getCategory())
                .set(EDUCATION_MATERIAL.ORDERING, req.getOrdering() != null ? req.getOrdering() : 0)
                .set(EDUCATION_MATERIAL.CREATED_DATE, OffsetDateTime.now())
                .set(EDUCATION_MATERIAL.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return toDto(record);
    }

    public EducationMaterialDto update(String username, Long id, EducationMaterialRequest req) {
        userService.requireAdmin(username);
        var record = dsl.update(EDUCATION_MATERIAL)
                .set(EDUCATION_MATERIAL.TITLE, req.getTitle())
                .set(EDUCATION_MATERIAL.DESCRIPTION, req.getDescription())
                .set(EDUCATION_MATERIAL.CATEGORY, req.getCategory())
                .set(EDUCATION_MATERIAL.ORDERING, req.getOrdering())
                .set(EDUCATION_MATERIAL.UPDATED_DATE, OffsetDateTime.now())
                .where(EDUCATION_MATERIAL.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Оқу материалы табылмады",
                "Учебный материал не найден",
                "Education material not found");
        return toDto(record);
    }

    public EducationMaterialDto uploadVideo(String username, Long id, MultipartFile file) {
        userService.requireAdmin(username);
        String objectName = minioService.upload(FOLDER, file);
        var record = dsl.update(EDUCATION_MATERIAL)
                .set(EDUCATION_MATERIAL.VIDEO_PATH, objectName)
                .set(EDUCATION_MATERIAL.UPDATED_DATE, OffsetDateTime.now())
                .where(EDUCATION_MATERIAL.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Оқу материалы табылмады",
                "Учебный материал не найден",
                "Education material not found");
        return toDto(record);
    }

    public EducationMaterialDto uploadPresentation(String username, Long id, MultipartFile file) {
        userService.requireAdmin(username);
        String objectName = minioService.upload(FOLDER, file);
        var record = dsl.update(EDUCATION_MATERIAL)
                .set(EDUCATION_MATERIAL.PRESENTATION_PATH, objectName)
                .set(EDUCATION_MATERIAL.UPDATED_DATE, OffsetDateTime.now())
                .where(EDUCATION_MATERIAL.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Оқу материалы табылмады",
                "Учебный материал не найден",
                "Education material not found");
        return toDto(record);
    }

    public EducationMaterialDto uploadThumbnail(String username, Long id, MultipartFile file) {
        userService.requireAdmin(username);
        String objectName = minioService.upload(FOLDER, file);
        var record = dsl.update(EDUCATION_MATERIAL)
                .set(EDUCATION_MATERIAL.THUMBNAIL_PATH, objectName)
                .set(EDUCATION_MATERIAL.UPDATED_DATE, OffsetDateTime.now())
                .where(EDUCATION_MATERIAL.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Оқу материалы табылмады",
                "Учебный материал не найден",
                "Education material not found");
        return toDto(record);
    }

    public void delete(String username, Long id) {
        userService.requireAdmin(username);
        int deleted = dsl.deleteFrom(EDUCATION_MATERIAL).where(EDUCATION_MATERIAL.ID.eq(id)).execute();
        if (deleted == 0) throw new AppException(HttpStatus.NOT_FOUND,
                "Оқу материалы табылмады",
                "Учебный материал не найден",
                "Education material not found");
    }

    private EducationMaterialDto toDto(EducationMaterialRecord record) {
        return new EducationMaterialDto(
                record.getId(),
                record.getTitle(),
                record.getDescription(),
                record.getCategory(),
                resolvePresignedUrl(record.getThumbnailPath()),
                resolvePresignedUrl(record.getPresentationPath()),
                resolvePresignedUrl(record.getVideoPath()),
                record.getOrdering(),
                record.getCreatedDate(),
                record.getUpdatedDate()
        );
    }

    private String resolvePresignedUrl(String objectName) {
        if (objectName == null) return null;
        return minioService.presignedUrl(objectName, 24);
    }
}
