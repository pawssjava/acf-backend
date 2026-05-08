package kz.cyber.acf.core.tournament.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static group.bi.postsales.database.Tables.TOURNAMENT;

@Slf4j
@Component
@RequiredArgsConstructor
public class TournamentStatusScheduler {

    private static final long STATUS_COMPLETED = 3L;

    private final DefaultDSLContext dsl;

    private static final ZoneOffset ZONE = ZoneOffset.ofHours(5);

    @Scheduled(cron = "0 0 0 * * *", zone = "GMT+5")
    public void closeFinishedTournaments() {
        LocalDate yesterday = LocalDate.now(ZONE).minusDays(1);
        OffsetDateTime now = OffsetDateTime.now(ZONE);

        int completed = dsl.update(TOURNAMENT)
                .set(TOURNAMENT.TOURNAMENT_STATUS, STATUS_COMPLETED)
                .set(TOURNAMENT.UPDATED_DATE, now)
                .where(TOURNAMENT.END_DATE.isNotNull()
                        .and(TOURNAMENT.END_DATE.le(yesterday)))
                .execute();

        log.info("Tournament status sync: closed={}", completed);
    }
}
