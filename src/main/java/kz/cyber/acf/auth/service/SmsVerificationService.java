package kz.cyber.acf.auth.service;

import kz.cyber.acf.auth.SmsAction;
import lombok.RequiredArgsConstructor;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SmsVerificationService {

    private static final String HARDCODED_CODE = "1111";
    private static final int TTL_MINUTES = 5;

    private static final org.jooq.Table<?> SMS_LOG = DSL.table(DSL.name("acf", "sms_log"));

    private final DefaultDSLContext dsl;

    private final Map<String, SmsEntry> pending  = new ConcurrentHashMap<>();
    private final Map<String, Long>     verified = new ConcurrentHashMap<>();

    public void sendCode(String phone, SmsAction action) {
        pending.remove(phone);
        verified.remove(phone);

        OffsetDateTime now       = OffsetDateTime.now();
        OffsetDateTime expiresAt = now.plusMinutes(TTL_MINUTES);

        Long logId = dsl.insertInto(SMS_LOG)
                .set(DSL.field(DSL.name("phone_number"), String.class), phone)
                .set(DSL.field(DSL.name("code"),         String.class), HARDCODED_CODE)
                .set(DSL.field(DSL.name("action"),       String.class), action.name())
                .set(DSL.field(DSL.name("sent_at"),      OffsetDateTime.class), now)
                .set(DSL.field(DSL.name("expires_at"),   OffsetDateTime.class), expiresAt)
                .returning(DSL.field(DSL.name("id"), Long.class))
                .fetchOne()
                .get(DSL.field(DSL.name("id"), Long.class));

        pending.put(phone, new SmsEntry(HARDCODED_CODE, expiresAt.toInstant(), logId));
    }

    public boolean verifyCode(String phone, String code) {
        SmsEntry[] captured = new SmsEntry[1];

        // Atomically remove the entry only if it is valid and matches the code.
        pending.compute(phone, (k, entry) -> {
            if (entry != null && !entry.isExpired() && entry.code.equals(code)) {
                captured[0] = entry;
                return null; // remove from pending
            }
            return entry; // leave unchanged (bad code / expired)
        });

        if (captured[0] == null) {
            return false;
        }

        dsl.update(SMS_LOG)
                .set(DSL.field(DSL.name("verified_at"), OffsetDateTime.class), OffsetDateTime.now())
                .where(DSL.field(DSL.name("id"), Long.class).eq(captured[0].logId))
                .execute();

        verified.put(phone, captured[0].logId);
        return true;
    }

    public boolean isVerified(String phone) {
        return verified.containsKey(phone);
    }

    public void invalidate(String phone) {
        Long logId = verified.remove(phone);
        pending.remove(phone);

        if (logId != null) {
            dsl.update(SMS_LOG)
                    .set(DSL.field(DSL.name("used"), Boolean.class), true)
                    .where(DSL.field(DSL.name("id"), Long.class).eq(logId))
                    .execute();
        }
    }

    private static class SmsEntry {
        final String  code;
        final Instant expiresAt;
        final long    logId;

        SmsEntry(String code, Instant expiresAt, long logId) {
            this.code      = code;
            this.expiresAt = expiresAt;
            this.logId     = logId;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
