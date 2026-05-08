package kz.cyber.acf.core.tournament.match.service;

import group.bi.postsales.database.tables.User;
import group.bi.postsales.database.tables.records.TournamentMatchRecord;
import group.bi.postsales.database.tables.records.TournamentRecord;
import kz.cyber.acf.core.tournament.match.dto.*;
import kz.cyber.acf.core.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static group.bi.postsales.database.Tables.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class TournamentMatchService {

    private static final ZoneOffset ZONE = ZoneOffset.ofHours(5);

    private static final String FORMAT_SINGLE = "SINGLE_ELIMINATION";
    private static final String FORMAT_SWISS  = "SWISS";
    private static final String FORMAT_EKPL   = "EKPL";
    private static final String PHASE_PLAYOFF  = "PLAYOFF";
    private static final String PHASE_SWISS    = "SWISS";
    private static final String PHASE_REGULAR  = "REGULAR_SEASON";
    private static final String PHASE_COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING     = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED   = "COMPLETED";
    private static final String STATUS_BYE         = "BYE";

    private final DefaultDSLContext dsl;
    private final TournamentService tournamentService;

    // ── Public API ───────────────────────────────────────────────────────────

    public void start(Long tournamentId, TournamentStartRequest req) {
        var tournament = loadTournament(tournamentId);
        String format = tournament.getFormat();
        if (format == null) format = FORMAT_SINGLE;

        boolean alreadyStarted = dsl.fetchExists(TOURNAMENT_MATCH,
                TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId));
        if (alreadyStarted) throw badRequest("Tournament already started");

        List<Long> participants = fetchParticipants(tournamentId);
        if (participants.size() < 2) throw badRequest("At least 2 registered participants required");

        Collections.shuffle(participants);
        OffsetDateTime now = OffsetDateTime.now(ZONE);

        switch (format) {
            case FORMAT_SWISS -> {
                generateSwissRound(tournamentId, participants, 1, now);
                updateTournamentMeta(tournamentId, FORMAT_SWISS, PHASE_SWISS, now);
            }
            case FORMAT_EKPL -> {
                generateRegularSeason(tournamentId, participants, now);
                updateTournamentMeta(tournamentId, FORMAT_EKPL, PHASE_REGULAR, now);
            }
            default -> {
                generatePlayoffBracket(tournamentId, participants, now);
                updateTournamentMeta(tournamentId, FORMAT_SINGLE, PHASE_PLAYOFF, now);
            }
        }

        tournamentService.activateTournament(tournamentId);
    }

    public void nextSwissRound(Long tournamentId) {
        var tournament = loadTournament(tournamentId);
        if (!FORMAT_SWISS.equals(tournament.getFormat())) throw badRequest("Tournament is not SWISS format");
        if (!PHASE_SWISS.equals(tournament.getPhase())) throw badRequest("Tournament is not in Swiss phase");

        Integer currentRound = dsl.select(TOURNAMENT_MATCH.ROUND_NUMBER.max())
                .from(TOURNAMENT_MATCH)
                .where(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_MATCH.PHASE.eq(PHASE_SWISS)))
                .fetchOneInto(Integer.class);
        if (currentRound == null) currentRound = 0;

        boolean incomplete = dsl.fetchExists(TOURNAMENT_MATCH,
                TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_MATCH.PHASE.eq(PHASE_SWISS))
                        .and(TOURNAMENT_MATCH.ROUND_NUMBER.eq(currentRound))
                        .and(TOURNAMENT_MATCH.STATUS.ne(STATUS_COMPLETED))
                        .and(TOURNAMENT_MATCH.STATUS.ne(STATUS_BYE)));
        if (incomplete) throw badRequest("Current round has incomplete matches");

        Integer totalRounds = tournament.getTotalRounds();
        OffsetDateTime now = OffsetDateTime.now(ZONE);

        if (totalRounds != null && currentRound >= totalRounds) {
            saveSwissFinalResults(tournamentId, now);
            return;
        }

        List<Long> sortedParticipants = computeSwissStandings(tournamentId).stream()
                .map(GroupStandingDto::getUserId)
                .collect(Collectors.toList());

        generateSwissRound(tournamentId, sortedParticipants, currentRound + 1, now);
    }

    public void advanceRegularSeason(Long tournamentId, int advancers) {
        var tournament = loadTournament(tournamentId);
        if (!FORMAT_EKPL.equals(tournament.getFormat())) throw badRequest("Tournament is not eKPL format");
        if (!PHASE_REGULAR.equals(tournament.getPhase())) throw badRequest("Tournament is not in Regular Season phase");

        boolean incomplete = dsl.fetchExists(TOURNAMENT_MATCH,
                TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_MATCH.PHASE.eq(PHASE_REGULAR))
                        .and(TOURNAMENT_MATCH.STATUS.ne(STATUS_COMPLETED)));
        if (incomplete) throw badRequest("All regular season matches must be completed first");

        List<Long> topPlayers = computeRegularSeasonStandings(tournamentId).stream()
                .limit(advancers)
                .map(GroupStandingDto::getUserId)
                .collect(Collectors.toList());

        if (topPlayers.size() < 2) throw badRequest("Not enough participants to advance");

        OffsetDateTime now = OffsetDateTime.now(ZONE);
        generatePlayoffBracket(tournamentId, topPlayers, now);

        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.PHASE, PHASE_PLAYOFF)
                .set(TOURNAMENT.UPDATED_DATE, now)
                .where(TOURNAMENT.ID.eq(tournamentId))
                .execute();
    }

    public TournamentMatchDto updateScore(Long tournamentId, Long matchId, ScoreUpdateRequest req) {
        var match = loadMatch(matchId, tournamentId);
        if (STATUS_COMPLETED.equals(match.getStatus())) throw badRequest("Match already completed");

        dsl.update(TOURNAMENT_MATCH)
                .set(TOURNAMENT_MATCH.SCORE1, req.getScore1())
                .set(TOURNAMENT_MATCH.SCORE2, req.getScore2())
                .set(TOURNAMENT_MATCH.STATUS, STATUS_IN_PROGRESS)
                .set(TOURNAMENT_MATCH.UPDATED_DATE, OffsetDateTime.now(ZONE))
                .where(TOURNAMENT_MATCH.ID.eq(matchId))
                .execute();

        return fetchMatchDto(matchId, tournamentId);
    }

    public TournamentMatchDto completeMatch(Long tournamentId, Long matchId) {
        var match = loadMatch(matchId, tournamentId);
        if (STATUS_COMPLETED.equals(match.getStatus())) throw badRequest("Match already completed");

        int s1 = match.getScore1() != null ? match.getScore1() : 0;
        int s2 = match.getScore2() != null ? match.getScore2() : 0;

        if (s1 == s2 && !PHASE_SWISS.equals(match.getPhase())) {
            throw badRequest("Scores are tied — use setWinner to manually declare the winner");
        }

        if (s1 == s2) {
            // Swiss draw — record with no winner
            return finalizeMatch(tournamentId, match, null, null);
        }

        Long winnerId = s1 > s2 ? match.getParticipant1Id() : match.getParticipant2Id();
        Long loserId  = s1 > s2 ? match.getParticipant2Id() : match.getParticipant1Id();
        return finalizeMatch(tournamentId, match, winnerId, loserId);
    }

    public TournamentMatchDto setWinner(Long tournamentId, Long matchId, Long winnerId) {
        var match = loadMatch(matchId, tournamentId);
        if (STATUS_COMPLETED.equals(match.getStatus())) throw badRequest("Match already completed");

        Long p1 = match.getParticipant1Id();
        Long p2 = match.getParticipant2Id();
        if (!winnerId.equals(p1) && !winnerId.equals(p2))
            throw badRequest("Winner must be one of the match participants");

        Long loserId = winnerId.equals(p1) ? p2 : p1;
        return finalizeMatch(tournamentId, match, winnerId, loserId);
    }

    @Transactional(readOnly = true)
    public List<TournamentMatchDto> getMatches(Long tournamentId) {
        return fetchMatchDtos(tournamentId);
    }

    @Transactional(readOnly = true)
    public BracketDto getBracket(Long tournamentId) {
        var tournament = loadTournament(tournamentId);
        List<TournamentMatchDto> all = fetchMatchDtos(tournamentId);

        BracketDto bracket = new BracketDto();
        bracket.setFormat(tournament.getFormat());
        bracket.setPhase(tournament.getPhase());

        // Swiss rounds — represented as grouped rounds
        List<TournamentMatchDto> swissMatches = all.stream()
                .filter(m -> PHASE_SWISS.equals(m.getPhase()))
                .collect(Collectors.toList());
        if (!swissMatches.isEmpty()) {
            List<GroupDto> swissGroups = swissMatches.stream()
                    .collect(Collectors.groupingBy(m -> m.getRoundNumber() != null ? m.getRoundNumber() : 0))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        GroupDto g = new GroupDto();
                        g.setGroupName("Round " + e.getKey());
                        g.setMatches(e.getValue());
                        return g;
                    })
                    .collect(Collectors.toList());
            bracket.setGroups(swissGroups);
        }

        // Regular season (eKPL)
        List<TournamentMatchDto> regularMatches = all.stream()
                .filter(m -> PHASE_REGULAR.equals(m.getPhase()))
                .collect(Collectors.toList());
        if (!regularMatches.isEmpty()) {
            GroupDto rs = new GroupDto();
            rs.setGroupName("Regular Season");
            rs.setMatches(regularMatches);
            rs.setStandings(computeRegularSeasonStandings(tournamentId));
            bracket.setGroups(List.of(rs));
        }

        // Playoff
        List<TournamentMatchDto> playoffMatches = all.stream()
                .filter(m -> PHASE_PLAYOFF.equals(m.getPhase()))
                .collect(Collectors.toList());
        if (!playoffMatches.isEmpty()) {
            int maxRound = playoffMatches.stream()
                    .mapToInt(m -> m.getRoundNumber() != null ? m.getRoundNumber() : 0)
                    .max().orElse(1);
            List<PlayoffRoundDto> rounds = playoffMatches.stream()
                    .collect(Collectors.groupingBy(m -> m.getRoundNumber() != null ? m.getRoundNumber() : 0))
                    .entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        PlayoffRoundDto r = new PlayoffRoundDto();
                        r.setRoundNumber(e.getKey());
                        r.setRoundName(roundName(e.getKey(), maxRound));
                        r.setMatches(e.getValue());
                        return r;
                    })
                    .collect(Collectors.toList());
            bracket.setPlayoffRounds(rounds);
        }

        return bracket;
    }

    @Transactional(readOnly = true)
    public List<GroupStandingDto> getStandings(Long tournamentId) {
        var tournament = loadTournament(tournamentId);
        String format = tournament.getFormat();

        if (FORMAT_SWISS.equals(format)) {
            return computeSwissStandings(tournamentId);
        }

        if (FORMAT_EKPL.equals(format)) {
            return computeRegularSeasonStandings(tournamentId);
        }

        return List.of();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private TournamentRecord loadTournament(Long tournamentId) {
        var t = dsl.selectFrom(TOURNAMENT).where(TOURNAMENT.ID.eq(tournamentId)).fetchOne();
        if (t == null) throw notFound("Tournament not found");
        return t;
    }

    private List<Long> fetchParticipants(Long tournamentId) {
        return dsl.select(TOURNAMENT_REGISTRATION.USER_ID)
                .from(TOURNAMENT_REGISTRATION)
                .where(TOURNAMENT_REGISTRATION.TOURNAMENT_ID.eq(tournamentId))
                .fetch(TOURNAMENT_REGISTRATION.USER_ID);
    }

    private TournamentMatchRecord loadMatch(Long matchId, Long tournamentId) {
        var match = dsl.selectFrom(TOURNAMENT_MATCH)
                .where(TOURNAMENT_MATCH.ID.eq(matchId)
                        .and(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId)))
                .fetchOne();
        if (match == null) throw notFound("Match not found");
        return match;
    }

    private TournamentMatchDto finalizeMatch(Long tournamentId, TournamentMatchRecord match, Long winnerId, Long loserId) {
        OffsetDateTime now = OffsetDateTime.now(ZONE);

        dsl.update(TOURNAMENT_MATCH)
                .set(TOURNAMENT_MATCH.WINNER_ID, winnerId)
                .set(TOURNAMENT_MATCH.LOSER_ID, loserId)
                .set(TOURNAMENT_MATCH.STATUS, STATUS_COMPLETED)
                .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                .where(TOURNAMENT_MATCH.ID.eq(match.getId()))
                .execute();

        // Advance winner in playoff (only when there is a winner)
        if (PHASE_PLAYOFF.equals(match.getPhase()) && match.getNextMatchId() != null && winnerId != null) {
            Integer slot = match.getNextMatchSlot();
            if (slot != null && slot == 2) {
                dsl.update(TOURNAMENT_MATCH)
                        .set(TOURNAMENT_MATCH.PARTICIPANT2_ID, winnerId)
                        .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                        .where(TOURNAMENT_MATCH.ID.eq(match.getNextMatchId()))
                        .execute();
            } else {
                dsl.update(TOURNAMENT_MATCH)
                        .set(TOURNAMENT_MATCH.PARTICIPANT1_ID, winnerId)
                        .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                        .where(TOURNAMENT_MATCH.ID.eq(match.getNextMatchId()))
                        .execute();
            }
        }

        if (PHASE_PLAYOFF.equals(match.getPhase()) && match.getNextMatchId() == null) {
            saveFinalResults(tournamentId, now);
        }

        return fetchMatchDto(match.getId(), tournamentId);
    }

    private void generatePlayoffBracket(Long tournamentId, List<Long> participants, OffsetDateTime now) {
        int n      = participants.size();
        int size   = nextPow2(n);
        int rounds = Integer.numberOfTrailingZeros(size);

        List<Long> seeded = seedBracket(participants, size);
        Map<String, Long> ids = new LinkedHashMap<>();

        for (int round = 1; round <= rounds; round++) {
            int count = size >> round;
            for (int m = 1; m <= count; m++) {
                Long p1 = null, p2 = null;
                if (round == 1) {
                    p1 = seeded.get((m - 1) * 2);
                    p2 = seeded.get((m - 1) * 2 + 1);
                }
                String status = STATUS_PENDING;
                if (round == 1 && p1 != null && p2 == null) status = STATUS_BYE;

                Long matchId = dsl.insertInto(TOURNAMENT_MATCH)
                        .set(TOURNAMENT_MATCH.TOURNAMENT_ID, tournamentId)
                        .set(TOURNAMENT_MATCH.PHASE, PHASE_PLAYOFF)
                        .set(TOURNAMENT_MATCH.ROUND_NUMBER, round)
                        .set(TOURNAMENT_MATCH.MATCH_NUMBER, m)
                        .set(TOURNAMENT_MATCH.PARTICIPANT1_ID, p1)
                        .set(TOURNAMENT_MATCH.PARTICIPANT2_ID, p2)
                        .set(TOURNAMENT_MATCH.SCORE1, 0)
                        .set(TOURNAMENT_MATCH.SCORE2, 0)
                        .set(TOURNAMENT_MATCH.STATUS, status)
                        .set(TOURNAMENT_MATCH.CREATED_DATE, now)
                        .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                        .returning(TOURNAMENT_MATCH.ID)
                        .fetchOne(TOURNAMENT_MATCH.ID);

                ids.put(round + "-" + m, matchId);
            }
        }

        for (int round = 1; round < rounds; round++) {
            int count = size >> round;
            for (int m = 1; m <= count; m++) {
                Long matchId  = ids.get(round + "-" + m);
                int  nextM    = (m + 1) / 2;
                int  nextSlot = (m % 2 == 1) ? 1 : 2;
                Long nextId   = ids.get((round + 1) + "-" + nextM);

                dsl.update(TOURNAMENT_MATCH)
                        .set(TOURNAMENT_MATCH.NEXT_MATCH_ID, nextId)
                        .set(TOURNAMENT_MATCH.NEXT_MATCH_SLOT, nextSlot)
                        .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                        .where(TOURNAMENT_MATCH.ID.eq(matchId))
                        .execute();

                if (round == 1) {
                    var rec = dsl.selectFrom(TOURNAMENT_MATCH).where(TOURNAMENT_MATCH.ID.eq(matchId)).fetchOne();
                    if (rec != null && STATUS_BYE.equals(rec.getStatus())) {
                        Long advancer = rec.getParticipant1Id() != null ? rec.getParticipant1Id() : rec.getParticipant2Id();
                        if (nextSlot == 2) {
                            dsl.update(TOURNAMENT_MATCH).set(TOURNAMENT_MATCH.PARTICIPANT2_ID, advancer)
                                    .set(TOURNAMENT_MATCH.UPDATED_DATE, now).where(TOURNAMENT_MATCH.ID.eq(nextId)).execute();
                        } else {
                            dsl.update(TOURNAMENT_MATCH).set(TOURNAMENT_MATCH.PARTICIPANT1_ID, advancer)
                                    .set(TOURNAMENT_MATCH.UPDATED_DATE, now).where(TOURNAMENT_MATCH.ID.eq(nextId)).execute();
                        }
                    }
                }
            }
        }
    }

    private List<Long> seedBracket(List<Long> participants, int size) {
        int n         = participants.size();
        int byes      = size - n;
        int numPairs  = size / 2;
        int fullPairs = numPairs - byes;

        List<Long> seeded = new ArrayList<>(Collections.nCopies(size, null));
        int idx = 0;
        for (int i = 0; i < fullPairs; i++) {
            seeded.set(i * 2, participants.get(idx++));
            seeded.set(i * 2 + 1, participants.get(idx++));
        }
        for (int i = fullPairs; i < numPairs && idx < n; i++) {
            seeded.set(i * 2, participants.get(idx++));
        }
        return seeded;
    }

    private void generateSwissRound(Long tournamentId, List<Long> participants, int roundNumber, OffsetDateTime now) {
        Set<String> playedPairs = getPlayedPairs(tournamentId);
        List<Long[]> pairs = pairGreedy(participants, playedPairs);

        int matchNum = 1;
        for (Long[] pair : pairs) {
            String status = pair[1] == null ? STATUS_BYE : STATUS_PENDING;
            dsl.insertInto(TOURNAMENT_MATCH)
                    .set(TOURNAMENT_MATCH.TOURNAMENT_ID, tournamentId)
                    .set(TOURNAMENT_MATCH.PHASE, PHASE_SWISS)
                    .set(TOURNAMENT_MATCH.ROUND_NUMBER, roundNumber)
                    .set(TOURNAMENT_MATCH.MATCH_NUMBER, matchNum++)
                    .set(TOURNAMENT_MATCH.PARTICIPANT1_ID, pair[0])
                    .set(TOURNAMENT_MATCH.PARTICIPANT2_ID, pair[1])
                    .set(TOURNAMENT_MATCH.SCORE1, 0)
                    .set(TOURNAMENT_MATCH.SCORE2, 0)
                    .set(TOURNAMENT_MATCH.STATUS, status)
                    .set(TOURNAMENT_MATCH.CREATED_DATE, now)
                    .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                    .execute();
        }
    }

    private List<Long[]> pairGreedy(List<Long> participants, Set<String> playedPairs) {
        List<Long[]> pairs = new ArrayList<>();
        List<Long> remaining = new ArrayList<>(participants);

        while (remaining.size() >= 2) {
            Long p1 = remaining.remove(0);
            boolean paired = false;
            for (int i = 0; i < remaining.size(); i++) {
                Long p2 = remaining.get(i);
                String key = Math.min(p1, p2) + "_" + Math.max(p1, p2);
                if (!playedPairs.contains(key)) {
                    pairs.add(new Long[]{p1, p2});
                    remaining.remove(i);
                    paired = true;
                    break;
                }
            }
            if (!paired) {
                pairs.add(new Long[]{p1, remaining.remove(0)});
            }
        }

        if (!remaining.isEmpty()) {
            pairs.add(new Long[]{remaining.get(0), null});
        }

        return pairs;
    }

    private Set<String> getPlayedPairs(Long tournamentId) {
        return dsl.select(TOURNAMENT_MATCH.PARTICIPANT1_ID, TOURNAMENT_MATCH.PARTICIPANT2_ID)
                .from(TOURNAMENT_MATCH)
                .where(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_MATCH.PHASE.eq(PHASE_SWISS))
                        .and(TOURNAMENT_MATCH.PARTICIPANT2_ID.isNotNull()))
                .fetch()
                .stream()
                .map(r -> {
                    Long p1 = r.get(TOURNAMENT_MATCH.PARTICIPANT1_ID);
                    Long p2 = r.get(TOURNAMENT_MATCH.PARTICIPANT2_ID);
                    return Math.min(p1, p2) + "_" + Math.max(p1, p2);
                })
                .collect(Collectors.toSet());
    }

    private void generateRegularSeason(Long tournamentId, List<Long> participants, OffsetDateTime now) {
        int matchNum = 1;
        for (int i = 0; i < participants.size(); i++) {
            for (int j = i + 1; j < participants.size(); j++) {
                dsl.insertInto(TOURNAMENT_MATCH)
                        .set(TOURNAMENT_MATCH.TOURNAMENT_ID, tournamentId)
                        .set(TOURNAMENT_MATCH.PHASE, PHASE_REGULAR)
                        .set(TOURNAMENT_MATCH.MATCH_NUMBER, matchNum++)
                        .set(TOURNAMENT_MATCH.PARTICIPANT1_ID, participants.get(i))
                        .set(TOURNAMENT_MATCH.PARTICIPANT2_ID, participants.get(j))
                        .set(TOURNAMENT_MATCH.SCORE1, 0)
                        .set(TOURNAMENT_MATCH.SCORE2, 0)
                        .set(TOURNAMENT_MATCH.STATUS, STATUS_PENDING)
                        .set(TOURNAMENT_MATCH.CREATED_DATE, now)
                        .set(TOURNAMENT_MATCH.UPDATED_DATE, now)
                        .execute();
            }
        }
    }

    private List<GroupStandingDto> computeStandings(String groupName, List<TournamentMatchDto> allMatches) {
        List<TournamentMatchDto> matches = groupName == null ? allMatches : allMatches.stream()
                .filter(m -> groupName.equals(m.getGroupName()))
                .collect(Collectors.toList());

        Map<Long, GroupStandingDto> map = new LinkedHashMap<>();

        for (TournamentMatchDto m : matches) {
            if (m.getParticipant1Id() != null)
                map.computeIfAbsent(m.getParticipant1Id(), id -> blankStanding(id, groupName,
                        m.getParticipant1Username(), m.getParticipant1FirstName(), m.getParticipant1LastName()));
            if (m.getParticipant2Id() != null)
                map.computeIfAbsent(m.getParticipant2Id(), id -> blankStanding(id, groupName,
                        m.getParticipant2Username(), m.getParticipant2FirstName(), m.getParticipant2LastName()));

            if (!STATUS_COMPLETED.equals(m.getStatus())) continue;

            int s1 = m.getScore1() != null ? m.getScore1() : 0;
            int s2 = m.getScore2() != null ? m.getScore2() : 0;
            GroupStandingDto d1 = m.getParticipant1Id() != null ? map.get(m.getParticipant1Id()) : null;
            GroupStandingDto d2 = m.getParticipant2Id() != null ? map.get(m.getParticipant2Id()) : null;

            if (d1 != null) { d1.setPlayed(d1.getPlayed() + 1); d1.setScoreDiff(d1.getScoreDiff() + s1 - s2); }
            if (d2 != null) { d2.setPlayed(d2.getPlayed() + 1); d2.setScoreDiff(d2.getScoreDiff() + s2 - s1); }

            if (s1 > s2) {
                if (d1 != null) { d1.setWins(d1.getWins() + 1);   d1.setPoints(d1.getPoints() + 3); }
                if (d2 != null)   d2.setLosses(d2.getLosses() + 1);
            } else if (s2 > s1) {
                if (d2 != null) { d2.setWins(d2.getWins() + 1);   d2.setPoints(d2.getPoints() + 3); }
                if (d1 != null)   d1.setLosses(d1.getLosses() + 1);
            } else {
                if (d1 != null) { d1.setDraws(d1.getDraws() + 1); d1.setPoints(d1.getPoints() + 1); }
                if (d2 != null) { d2.setDraws(d2.getDraws() + 1); d2.setPoints(d2.getPoints() + 1); }
            }
        }

        return map.values().stream()
                .sorted(Comparator.comparingInt(GroupStandingDto::getPoints).reversed()
                        .thenComparingInt(GroupStandingDto::getScoreDiff).reversed()
                        .thenComparingInt(GroupStandingDto::getWins).reversed())
                .collect(Collectors.toList());
    }

    private List<GroupStandingDto> computeSwissStandings(Long tournamentId) {
        List<TournamentMatchDto> matches = fetchMatchDtos(tournamentId).stream()
                .filter(m -> PHASE_SWISS.equals(m.getPhase()))
                .collect(Collectors.toList());

        Map<Long, GroupStandingDto> standingMap = new LinkedHashMap<>();
        Map<Long, Double> swissPoints = new HashMap<>();

        for (TournamentMatchDto m : matches) {
            if (m.getParticipant1Id() != null) {
                standingMap.computeIfAbsent(m.getParticipant1Id(), id -> blankStanding(id, null,
                        m.getParticipant1Username(), m.getParticipant1FirstName(), m.getParticipant1LastName()));
                swissPoints.putIfAbsent(m.getParticipant1Id(), 0.0);
            }
            if (m.getParticipant2Id() != null) {
                standingMap.computeIfAbsent(m.getParticipant2Id(), id -> blankStanding(id, null,
                        m.getParticipant2Username(), m.getParticipant2FirstName(), m.getParticipant2LastName()));
                swissPoints.putIfAbsent(m.getParticipant2Id(), 0.0);
            }

            if (!STATUS_COMPLETED.equals(m.getStatus()) && !STATUS_BYE.equals(m.getStatus())) continue;

            Long p1 = m.getParticipant1Id();
            Long p2 = m.getParticipant2Id();

            if (STATUS_BYE.equals(m.getStatus())) {
                if (p1 != null) swissPoints.merge(p1, 1.0, Double::sum);
                continue;
            }

            GroupStandingDto d1 = p1 != null ? standingMap.get(p1) : null;
            GroupStandingDto d2 = p2 != null ? standingMap.get(p2) : null;
            if (d1 != null) d1.setPlayed(d1.getPlayed() + 1);
            if (d2 != null) d2.setPlayed(d2.getPlayed() + 1);

            Long winnerId = m.getWinnerId();
            if (winnerId == null) {
                // Draw
                if (p1 != null) { swissPoints.merge(p1, 0.5, Double::sum); if (d1 != null) d1.setDraws(d1.getDraws() + 1); }
                if (p2 != null) { swissPoints.merge(p2, 0.5, Double::sum); if (d2 != null) d2.setDraws(d2.getDraws() + 1); }
            } else {
                Long loserId = winnerId.equals(p1) ? p2 : p1;
                swissPoints.merge(winnerId, 1.0, Double::sum);
                GroupStandingDto ws = standingMap.get(winnerId);
                GroupStandingDto ls = loserId != null ? standingMap.get(loserId) : null;
                if (ws != null) { ws.setWins(ws.getWins() + 1); ws.setPoints(ws.getPoints() + 3); }
                if (ls != null) ls.setLosses(ls.getLosses() + 1);
            }
        }

        // Buchholz = sum of all opponents' swiss points
        Map<Long, List<Long>> opponentsMap = new HashMap<>();
        for (TournamentMatchDto m : matches) {
            if (!STATUS_COMPLETED.equals(m.getStatus())) continue;
            Long p1 = m.getParticipant1Id();
            Long p2 = m.getParticipant2Id();
            if (p1 != null && p2 != null) {
                opponentsMap.computeIfAbsent(p1, k -> new ArrayList<>()).add(p2);
                opponentsMap.computeIfAbsent(p2, k -> new ArrayList<>()).add(p1);
            }
        }

        for (Map.Entry<Long, GroupStandingDto> e : standingMap.entrySet()) {
            Long uid = e.getKey();
            GroupStandingDto dto = e.getValue();
            dto.setSwissPoints(swissPoints.getOrDefault(uid, 0.0));
            double buchholz = opponentsMap.getOrDefault(uid, Collections.emptyList()).stream()
                    .mapToDouble(opp -> swissPoints.getOrDefault(opp, 0.0))
                    .sum();
            dto.setBuchholz(buchholz);
        }

        return standingMap.values().stream()
                .sorted(Comparator.comparingDouble(GroupStandingDto::getSwissPoints).reversed()
                        .thenComparingDouble(GroupStandingDto::getBuchholz).reversed())
                .collect(Collectors.toList());
    }

    private List<GroupStandingDto> computeRegularSeasonStandings(Long tournamentId) {
        List<TournamentMatchDto> matches = fetchMatchDtos(tournamentId).stream()
                .filter(m -> PHASE_REGULAR.equals(m.getPhase()))
                .collect(Collectors.toList());
        return computeStandings(null, matches);
    }

    private GroupStandingDto blankStanding(Long userId, String groupName, String username, String firstName, String lastName) {
        GroupStandingDto s = new GroupStandingDto();
        s.setUserId(userId);
        s.setGroupName(groupName);
        s.setUsername(username);
        s.setFirstName(firstName);
        s.setLastName(lastName);
        return s;
    }

    private void saveFinalResults(Long tournamentId, OffsetDateTime now) {
        var playoffMatches = dsl.selectFrom(TOURNAMENT_MATCH)
                .where(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_MATCH.PHASE.eq(PHASE_PLAYOFF))
                        .and(TOURNAMENT_MATCH.STATUS.eq(STATUS_COMPLETED)))
                .orderBy(TOURNAMENT_MATCH.ROUND_NUMBER.desc())
                .fetch();

        int maxRound = playoffMatches.stream()
                .mapToInt(m -> m.getRoundNumber() != null ? m.getRoundNumber() : 0)
                .max().orElse(0);

        for (var match : playoffMatches) {
            int round = match.getRoundNumber() != null ? match.getRoundNumber() : 0;
            if (round == maxRound) {
                if (match.getWinnerId() != null) upsertResult(tournamentId, match.getWinnerId(), 1, now);
                if (match.getLoserId()  != null) upsertResult(tournamentId, match.getLoserId(),  2, now);
            } else if (match.getLoserId() != null) {
                int place = (1 << (maxRound - round)) + 1;
                upsertResult(tournamentId, match.getLoserId(), place, now);
            }
        }

        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.PHASE, PHASE_COMPLETED)
                .set(TOURNAMENT.UPDATED_DATE, now)
                .where(TOURNAMENT.ID.eq(tournamentId))
                .execute();
        log.info("Tournament {} completed — results saved", tournamentId);
    }

    private void saveSwissFinalResults(Long tournamentId, OffsetDateTime now) {
        List<GroupStandingDto> standings = computeSwissStandings(tournamentId);
        for (int i = 0; i < standings.size(); i++) {
            upsertResult(tournamentId, standings.get(i).getUserId(), i + 1, now);
        }
        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.PHASE, PHASE_COMPLETED)
                .set(TOURNAMENT.UPDATED_DATE, now)
                .where(TOURNAMENT.ID.eq(tournamentId))
                .execute();
        log.info("Swiss tournament {} completed — results saved", tournamentId);
    }

    private void upsertResult(Long tournamentId, Long userId, int place, OffsetDateTime now) {
        boolean exists = dsl.fetchExists(TOURNAMENT_RESULT,
                TOURNAMENT_RESULT.TOURNAMENT_ID.eq(tournamentId)
                        .and(TOURNAMENT_RESULT.USER_ID.eq(userId)));
        if (exists) {
            dsl.update(TOURNAMENT_RESULT)
                    .set(TOURNAMENT_RESULT.PLACE, place)
                    .where(TOURNAMENT_RESULT.TOURNAMENT_ID.eq(tournamentId)
                            .and(TOURNAMENT_RESULT.USER_ID.eq(userId)))
                    .execute();
        } else {
            dsl.insertInto(TOURNAMENT_RESULT)
                    .set(TOURNAMENT_RESULT.TOURNAMENT_ID, tournamentId)
                    .set(TOURNAMENT_RESULT.USER_ID, userId)
                    .set(TOURNAMENT_RESULT.PLACE, place)
                    .set(TOURNAMENT_RESULT.CREATED_DATE, now)
                    .execute();
        }
    }

    private void updateTournamentMeta(Long tournamentId, String format, String phase, OffsetDateTime now) {
        dsl.update(TOURNAMENT)
                .set(TOURNAMENT.FORMAT, format)
                .set(TOURNAMENT.PHASE, phase)
                .set(TOURNAMENT.UPDATED_DATE, now)
                .where(TOURNAMENT.ID.eq(tournamentId))
                .execute();
    }

    private List<TournamentMatchDto> fetchMatchDtos(Long tournamentId) {
        User u1 = USER.as("u1");
        User u2 = USER.as("u2");

        return dsl.select(
                        TOURNAMENT_MATCH.ID,
                        TOURNAMENT_MATCH.TOURNAMENT_ID,
                        TOURNAMENT_MATCH.PHASE,
                        TOURNAMENT_MATCH.ROUND_NUMBER,
                        TOURNAMENT_MATCH.MATCH_NUMBER,
                        TOURNAMENT_MATCH.GROUP_NAME,
                        TOURNAMENT_MATCH.PARTICIPANT1_ID,
                        TOURNAMENT_MATCH.PARTICIPANT2_ID,
                        TOURNAMENT_MATCH.SCORE1,
                        TOURNAMENT_MATCH.SCORE2,
                        TOURNAMENT_MATCH.WINNER_ID,
                        TOURNAMENT_MATCH.STATUS,
                        TOURNAMENT_MATCH.NEXT_MATCH_ID,
                        u1.USERNAME.as("u1_username"),
                        u1.FIRST_NAME.as("u1_first_name"),
                        u1.LAST_NAME.as("u1_last_name"),
                        u2.USERNAME.as("u2_username"),
                        u2.FIRST_NAME.as("u2_first_name"),
                        u2.LAST_NAME.as("u2_last_name")
                )
                .from(TOURNAMENT_MATCH)
                .leftJoin(u1).on(TOURNAMENT_MATCH.PARTICIPANT1_ID.eq(u1.ID))
                .leftJoin(u2).on(TOURNAMENT_MATCH.PARTICIPANT2_ID.eq(u2.ID))
                .where(TOURNAMENT_MATCH.TOURNAMENT_ID.eq(tournamentId))
                .orderBy(TOURNAMENT_MATCH.PHASE.asc(),
                        TOURNAMENT_MATCH.GROUP_NAME.asc().nullsLast(),
                        TOURNAMENT_MATCH.ROUND_NUMBER.asc(),
                        TOURNAMENT_MATCH.MATCH_NUMBER.asc())
                .fetch(r -> {
                    TournamentMatchDto dto = new TournamentMatchDto();
                    dto.setId(r.get(TOURNAMENT_MATCH.ID));
                    dto.setTournamentId(r.get(TOURNAMENT_MATCH.TOURNAMENT_ID));
                    dto.setPhase(r.get(TOURNAMENT_MATCH.PHASE));
                    dto.setRoundNumber(r.get(TOURNAMENT_MATCH.ROUND_NUMBER));
                    dto.setMatchNumber(r.get(TOURNAMENT_MATCH.MATCH_NUMBER));
                    dto.setGroupName(r.get(TOURNAMENT_MATCH.GROUP_NAME));
                    dto.setParticipant1Id(r.get(TOURNAMENT_MATCH.PARTICIPANT1_ID));
                    dto.setParticipant1Username(r.get("u1_username", String.class));
                    dto.setParticipant1FirstName(r.get("u1_first_name", String.class));
                    dto.setParticipant1LastName(r.get("u1_last_name", String.class));
                    dto.setParticipant2Id(r.get(TOURNAMENT_MATCH.PARTICIPANT2_ID));
                    dto.setParticipant2Username(r.get("u2_username", String.class));
                    dto.setParticipant2FirstName(r.get("u2_first_name", String.class));
                    dto.setParticipant2LastName(r.get("u2_last_name", String.class));
                    dto.setScore1(r.get(TOURNAMENT_MATCH.SCORE1));
                    dto.setScore2(r.get(TOURNAMENT_MATCH.SCORE2));
                    dto.setWinnerId(r.get(TOURNAMENT_MATCH.WINNER_ID));
                    dto.setStatus(r.get(TOURNAMENT_MATCH.STATUS));
                    dto.setNextMatchId(r.get(TOURNAMENT_MATCH.NEXT_MATCH_ID));
                    return dto;
                });
    }

    private TournamentMatchDto fetchMatchDto(Long matchId, Long tournamentId) {
        return fetchMatchDtos(tournamentId).stream()
                .filter(m -> matchId.equals(m.getId()))
                .findFirst()
                .orElseThrow(() -> notFound("Match not found"));
    }

    private String roundName(int round, int maxRound) {
        int diff = maxRound - round;
        return switch (diff) {
            case 0 -> "Final";
            case 1 -> "Semifinal";
            case 2 -> "Quarterfinal";
            default -> "Round of " + (1 << (diff + 1));
        };
    }

    private static int nextPow2(int n) {
        if (n <= 1) return 1;
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    private ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private ResponseStatusException notFound(String msg) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
    }
}
