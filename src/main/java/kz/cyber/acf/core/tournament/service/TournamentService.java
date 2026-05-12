package kz.cyber.acf.core.tournament.service;

import kz.cyber.acf.core.tournament.dto.TournamentDto;
import kz.cyber.acf.core.tournament.dto.TournamentRequest;
import kz.cyber.acf.storage.MinioService;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DefaultDSLContext;
import kz.cyber.acf.config.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static group.bi.postsales.database.Tables.*;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private static final ZoneOffset ZONE          = ZoneOffset.ofHours(5);
    private static final String FOLDER           = "tournaments";
    private static final String FORMAT_EKPL      = "EKPL";
    private static final Long   TYPE_EKPL        = 5L;
    private static final Long   STATUS_UPCOMING   = 2L;
    private static final Long   STATUS_ACTIVE     = 1L;
    private static final Long   STATUS_COMPLETED  = 3L;
    private static final Set<String> VALID_FORMATS = Set.of("SINGLE_ELIMINATION", "SWISS", "EKPL");

    private final DefaultDSLContext dsl;
    private final MinioService minioService;

    public List<TournamentDto> findAll(Long tournamentTypeId) {
        var query = dsl.select(
                        TOURNAMENT.ID,
                        TOURNAMENT.NAME,
                        TOURNAMENT.LOGO,
                        TOURNAMENT.START_DATE,
                        TOURNAMENT.END_DATE,
                        TOURNAMENT.CAPACITY,
                        TOURNAMENT.PRIZE_MONEY,
                        TOURNAMENT.TOURNAMENT_STATUS,
                        D_TOURNAMENT_STATUS.NAME.as("status_name"),
                        TOURNAMENT.TOURNAMENT_TYPE,
                        D_TOURNAMENT_TYPE.NAME.as("type_name"),
                        TOURNAMENT.CREATED_DATE,
                        TOURNAMENT.UPDATED_DATE,
                        TOURNAMENT.FORMAT,
                        TOURNAMENT.PHASE,
                        TOURNAMENT.TOTAL_ROUNDS
                )
                .from(TOURNAMENT)
                .leftJoin(D_TOURNAMENT_STATUS).on(TOURNAMENT.TOURNAMENT_STATUS.eq(D_TOURNAMENT_STATUS.ID))
                .leftJoin(D_TOURNAMENT_TYPE).on(TOURNAMENT.TOURNAMENT_TYPE.eq(D_TOURNAMENT_TYPE.ID))
                .where(tournamentTypeId == null
                        ? org.jooq.impl.DSL.noCondition()
                        : TOURNAMENT.TOURNAMENT_TYPE.eq(tournamentTypeId))
                .orderBy(TOURNAMENT.CREATED_DATE.desc());
        return query.fetch(r -> new TournamentDto(
                        r.get(TOURNAMENT.ID),
                        r.get(TOURNAMENT.NAME),
                        resolveUrl(r.get(TOURNAMENT.LOGO)),
                        r.get(TOURNAMENT.START_DATE),
                        r.get(TOURNAMENT.END_DATE),
                        r.get(TOURNAMENT.CAPACITY),
                        r.get(TOURNAMENT.PRIZE_MONEY),
                        r.get(TOURNAMENT.TOURNAMENT_STATUS),
                        r.get("status_name", String.class),
                        r.get(TOURNAMENT.TOURNAMENT_TYPE),
                        r.get("type_name", String.class),
                        r.get(TOURNAMENT.CREATED_DATE),
                        r.get(TOURNAMENT.UPDATED_DATE),
                        r.get(TOURNAMENT.FORMAT),
                        r.get(TOURNAMENT.PHASE),
                        r.get(TOURNAMENT.TOTAL_ROUNDS)
                ));
    }

    public TournamentDto findById(Long id) {
        return dsl.select(
                        TOURNAMENT.ID,
                        TOURNAMENT.NAME,
                        TOURNAMENT.LOGO,
                        TOURNAMENT.START_DATE,
                        TOURNAMENT.END_DATE,
                        TOURNAMENT.CAPACITY,
                        TOURNAMENT.PRIZE_MONEY,
                        TOURNAMENT.TOURNAMENT_STATUS,
                        D_TOURNAMENT_STATUS.NAME.as("status_name"),
                        TOURNAMENT.TOURNAMENT_TYPE,
                        D_TOURNAMENT_TYPE.NAME.as("type_name"),
                        TOURNAMENT.CREATED_DATE,
                        TOURNAMENT.UPDATED_DATE,
                        TOURNAMENT.FORMAT,
                        TOURNAMENT.PHASE,
                        TOURNAMENT.TOTAL_ROUNDS
                )
                .from(TOURNAMENT)
                .leftJoin(D_TOURNAMENT_STATUS).on(TOURNAMENT.TOURNAMENT_STATUS.eq(D_TOURNAMENT_STATUS.ID))
                .leftJoin(D_TOURNAMENT_TYPE).on(TOURNAMENT.TOURNAMENT_TYPE.eq(D_TOURNAMENT_TYPE.ID))
                .where(TOURNAMENT.ID.eq(id))
                .fetchOptional(r -> new TournamentDto(
                        r.get(TOURNAMENT.ID),
                        r.get(TOURNAMENT.NAME),
                        resolveUrl(r.get(TOURNAMENT.LOGO)),
                        r.get(TOURNAMENT.START_DATE),
                        r.get(TOURNAMENT.END_DATE),
                        r.get(TOURNAMENT.CAPACITY),
                        r.get(TOURNAMENT.PRIZE_MONEY),
                        r.get(TOURNAMENT.TOURNAMENT_STATUS),
                        r.get("status_name", String.class),
                        r.get(TOURNAMENT.TOURNAMENT_TYPE),
                        r.get("type_name", String.class),
                        r.get(TOURNAMENT.CREATED_DATE),
                        r.get(TOURNAMENT.UPDATED_DATE),
                        r.get(TOURNAMENT.FORMAT),
                        r.get(TOURNAMENT.PHASE),
                        r.get(TOURNAMENT.TOTAL_ROUNDS)
                ))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Турнир табылмады", "Турнир не найден", "Tournament not found"));
    }

    public TournamentDto create(TournamentRequest req) {
        validateFormat(req.getFormat(), req.getTournamentTypeId());

        Long id = dsl.insertInto(TOURNAMENT)
                .set(TOURNAMENT.NAME, req.getName())
                .set(TOURNAMENT.START_DATE, req.getStartDate())
                .set(TOURNAMENT.END_DATE, req.getEndDate())
                .set(TOURNAMENT.CAPACITY, req.getCapacity())
                .set(TOURNAMENT.PRIZE_MONEY, req.getPrizeMoney())
                .set(TOURNAMENT.TOURNAMENT_STATUS, STATUS_UPCOMING)
                .set(TOURNAMENT.TOURNAMENT_TYPE, req.getTournamentTypeId())
                .set(TOURNAMENT.FORMAT, req.getFormat())
                .set(TOURNAMENT.TOTAL_ROUNDS, req.getTotalRounds())
                .set(TOURNAMENT.CREATED_DATE, OffsetDateTime.now(ZONE))
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now(ZONE))
                .returning(TOURNAMENT.ID)
                .fetchOne(TOURNAMENT.ID);
        return findById(id);
    }

    public TournamentDto update(Long id, TournamentRequest req) {
        var existing = dsl.selectFrom(TOURNAMENT).where(TOURNAMENT.ID.eq(id)).fetchOne();
        if (existing == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир табылмады", "Турнир не найден", "Tournament not found");

        rejectIfNotEditable(existing.getTournamentStatus());

        if (existing.getPhase() != null && req.getFormat() != null
                && !req.getFormat().equals(existing.getFormat())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Турнир басталғаннан кейін форматты өзгерту мүмкін емес",
                    "Нельзя изменить формат после начала турнира",
                    "Cannot change format after the tournament has started");
        }

        String effectiveFormat = req.getFormat() != null ? req.getFormat() : existing.getFormat();
        Long effectiveType = req.getTournamentTypeId() != null ? req.getTournamentTypeId() : existing.getTournamentType();
        validateFormat(effectiveFormat, effectiveType);

        int updated = dsl.update(TOURNAMENT)
                .set(TOURNAMENT.NAME, req.getName())
                .set(TOURNAMENT.START_DATE, req.getStartDate())
                .set(TOURNAMENT.END_DATE, req.getEndDate())
                .set(TOURNAMENT.CAPACITY, req.getCapacity())
                .set(TOURNAMENT.PRIZE_MONEY, req.getPrizeMoney())
                .set(TOURNAMENT.TOURNAMENT_STATUS, req.getTournamentStatusId())
                .set(TOURNAMENT.TOURNAMENT_TYPE, req.getTournamentTypeId())
                .set(TOURNAMENT.FORMAT, req.getFormat())
                .set(TOURNAMENT.TOTAL_ROUNDS, req.getTotalRounds())
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now(ZONE))
                .where(TOURNAMENT.ID.eq(id))
                .execute();

        if (updated == 0) throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир табылмады", "Турнир не найден", "Tournament not found");
        return findById(id);
    }

    public TournamentDto uploadLogo(Long id, MultipartFile file) {
        var existing = dsl.selectFrom(TOURNAMENT).where(TOURNAMENT.ID.eq(id)).fetchOne();
        if (existing == null) throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир табылмады", "Турнир не найден", "Tournament not found");
        rejectIfNotEditable(existing.getTournamentStatus());
        String objectName = minioService.upload(FOLDER, file);
        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.LOGO, objectName)
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now(ZONE))
                .where(TOURNAMENT.ID.eq(id))
                .execute();
        return findById(id);
    }

    public void activateTournament(Long id) {
        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.TOURNAMENT_STATUS, STATUS_ACTIVE)
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now(ZONE))
                .where(TOURNAMENT.ID.eq(id))
                .execute();
    }

    public void completeTournament(Long id) {
        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.TOURNAMENT_STATUS, STATUS_COMPLETED)
                .set(TOURNAMENT.UPDATED_DATE, OffsetDateTime.now(ZONE))
                .where(TOURNAMENT.ID.eq(id))
                .execute();
    }

    public void delete(Long id) {
        int deleted = dsl.deleteFrom(TOURNAMENT).where(TOURNAMENT.ID.eq(id)).execute();
        if (deleted == 0) throw new AppException(HttpStatus.NOT_FOUND,
                "Турнир табылмады", "Турнир не найден", "Tournament not found");
    }

    private void rejectIfNotEditable(Long statusId) {
        if (STATUS_ACTIVE.equals(statusId) || STATUS_COMPLETED.equals(statusId)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Турнир басталған немесе аяқталғаннан кейін өңдеу мүмкін емес",
                    "Турнир нельзя редактировать после начала или завершения",
                    "Tournament cannot be edited after it has started or finished");
        }
    }

    private void validateFormat(String format, Long tournamentTypeId) {
        if (format != null && !VALID_FORMATS.contains(format)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Жарамсыз формат. Рұқсат берілген: SINGLE_ELIMINATION, SWISS, EKPL",
                    "Неверный формат. Допустимые: SINGLE_ELIMINATION, SWISS, EKPL",
                    "Invalid format. Allowed values: SINGLE_ELIMINATION, SWISS, EKPL");
        }
        if (FORMAT_EKPL.equals(format) && !TYPE_EKPL.equals(tournamentTypeId)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "EKPL форматы тек 5 (eKPL) турнир түріне рұқсат етілген",
                    "Формат EKPL разрешён только для типа турнира 5 (eKPL)",
                    "EKPL format is only allowed for tournament type 5 (eKPL)");
        }
        if (TYPE_EKPL.equals(tournamentTypeId) && format != null && !FORMAT_EKPL.equals(format)) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "eKPL турнир түрі EKPL форматын қолдануы керек",
                    "Тип турнира eKPL должен использовать формат EKPL",
                    "Tournament type eKPL must use EKPL format");
        }
    }

    private String resolveUrl(String objectName) {
        if (objectName == null || objectName.startsWith("http")) return objectName;
        return minioService.presignedUrl(objectName, 24);
    }
}
