package kz.cyber.acf.auth.service;

import kz.cyber.acf.auth.dto.SmsLogDto;
import kz.cyber.acf.core.user.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SmsLogService {

    private static final Table<?>            SMS_LOG      = DSL.table(DSL.name("acf", "sms_log"));
    private static final Field<Long>         F_ID          = DSL.field(DSL.name("acf", "sms_log", "id"),          Long.class);
    private static final Field<String>       F_PHONE       = DSL.field(DSL.name("acf", "sms_log", "phone_number"), String.class);
    private static final Field<String>       F_CODE        = DSL.field(DSL.name("acf", "sms_log", "code"),         String.class);
    private static final Field<String>       F_ACTION      = DSL.field(DSL.name("acf", "sms_log", "action"),       String.class);
    private static final Field<OffsetDateTime> F_SENT_AT   = DSL.field(DSL.name("acf", "sms_log", "sent_at"),     OffsetDateTime.class);
    private static final Field<OffsetDateTime> F_EXPIRES_AT = DSL.field(DSL.name("acf", "sms_log", "expires_at"), OffsetDateTime.class);
    private static final Field<OffsetDateTime> F_VERIFIED_AT = DSL.field(DSL.name("acf", "sms_log", "verified_at"), OffsetDateTime.class);
    private static final Field<Boolean>      F_USED        = DSL.field(DSL.name("acf", "sms_log", "used"),         Boolean.class);

    private final DefaultDSLContext dsl;

    public PageResponse<SmsLogDto> getLogs(String phone, Boolean used, int page, int size) {
        int offset = page * size;

        Condition filter = buildFilter(phone, used);

        long total = dsl.selectCount()
                .from(SMS_LOG)
                .where(filter)
                .fetchOne(0, Long.class);

        List<SmsLogDto> content = dsl
                .select(F_ID, F_PHONE, F_CODE, F_ACTION, F_SENT_AT, F_EXPIRES_AT, F_VERIFIED_AT, F_USED)
                .from(SMS_LOG)
                .where(filter)
                .orderBy(F_SENT_AT.desc())
                .limit(size)
                .offset(offset)
                .fetch(r -> new SmsLogDto(
                        r.get(F_ID),
                        r.get(F_PHONE),
                        r.get(F_CODE),
                        r.get(F_ACTION),
                        r.get(F_SENT_AT),
                        r.get(F_EXPIRES_AT),
                        r.get(F_VERIFIED_AT),
                        r.get(F_USED)
                ));

        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    private Condition buildFilter(String phone, Boolean used) {
        Condition condition = DSL.trueCondition();

        if (phone != null && !phone.isBlank()) {
            condition = condition.and(F_PHONE.like("%" + phone.trim() + "%"));
        }

        if (used != null) {
            condition = condition.and(F_USED.eq(used));
        }

        return condition;
    }
}
