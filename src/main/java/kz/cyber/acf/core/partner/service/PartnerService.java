package kz.cyber.acf.core.partner.service;

import kz.cyber.acf.core.partner.dto.PartnerDto;
import kz.cyber.acf.core.partner.dto.PartnerRequest;
import kz.cyber.acf.core.user.service.UserService;
import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

import static group.bi.postsales.database.Tables.PARTNER;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private static final String BUCKET = "partners";

    private final DefaultDSLContext dsl;
    private final MinioService minioService;
    private final UserService userService;

    public List<PartnerDto> findAll() {
        return dsl.selectFrom(PARTNER)
                .orderBy(PARTNER.CREATED_DATE.desc())
                .fetch(r -> toDto(r.getId(), r.getName(), r.getLogo(), r.getDescription(), r.getHyperlink(), r.getCreatedDate(), r.getUpdatedDate()));
    }

    public PartnerDto findById(Long id) {
        return dsl.selectFrom(PARTNER)
                .where(PARTNER.ID.eq(id))
                .fetchOptional()
                .map(r -> toDto(r.getId(), r.getName(), r.getLogo(), r.getDescription(), r.getHyperlink(), r.getCreatedDate(), r.getUpdatedDate()))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Серіктес табылмады", "Партнёр не найден", "Partner not found"));
    }

    public PartnerDto create(String username, PartnerRequest req) {
        userService.requireAdmin(username);
        var record = dsl.insertInto(PARTNER)
                .set(PARTNER.NAME, req.getName())
                .set(PARTNER.DESCRIPTION, req.getDescription())
                .set(PARTNER.HYPERLINK, req.getHyperlink())
                .set(PARTNER.CREATED_DATE, OffsetDateTime.now())
                .set(PARTNER.UPDATED_DATE, OffsetDateTime.now())
                .returning()
                .fetchOne();
        return toDto(record.getId(), record.getName(), record.getLogo(), record.getDescription(), record.getHyperlink(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public PartnerDto update(String username, Long id, PartnerRequest req) {
        userService.requireAdmin(username);
        var record = dsl.update(PARTNER)
                .set(PARTNER.NAME, req.getName())
                .set(PARTNER.DESCRIPTION, req.getDescription())
                .set(PARTNER.HYPERLINK, req.getHyperlink())
                .set(PARTNER.UPDATED_DATE, OffsetDateTime.now())
                .where(PARTNER.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Серіктес табылмады",
                "Партнёр не найден",
                "Partner not found");
        return toDto(record.getId(), record.getName(), record.getLogo(), record.getDescription(), record.getHyperlink(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public PartnerDto uploadLogo(String username, Long id, MultipartFile file) {
        userService.requireAdmin(username);
        String objectName = minioService.upload(BUCKET, file);
        var record = dsl.update(PARTNER)
                .set(PARTNER.LOGO, objectName)
                .set(PARTNER.UPDATED_DATE, OffsetDateTime.now())
                .where(PARTNER.ID.eq(id))
                .returning()
                .fetchOne();
        if (record == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Серіктес табылмады",
                "Партнёр не найден",
                "Partner not found");
        return toDto(record.getId(), record.getName(), record.getLogo(), record.getDescription(), record.getHyperlink(), record.getCreatedDate(), record.getUpdatedDate());
    }

    public void delete(String username, Long id) {
        userService.requireAdmin(username);
        int deleted = dsl.deleteFrom(PARTNER).where(PARTNER.ID.eq(id)).execute();
        if (deleted == 0) {
            throw new AppException(HttpStatus.NOT_FOUND,
                "Серіктес табылмады",
                "Партнёр не найден",
                "Partner not found");
        }
    }

    private PartnerDto toDto(Long id, String name, String logo, String description, String hyperlink,
                              OffsetDateTime createdDate, OffsetDateTime updatedDate) {
        return new PartnerDto(id, name, resolveUrl(logo), description, hyperlink, createdDate, updatedDate);
    }

    private String resolveUrl(String objectName) {
        if (objectName == null || objectName.startsWith("http")) return objectName;
        return minioService.presignedUrl(objectName, 24);
    }
}
