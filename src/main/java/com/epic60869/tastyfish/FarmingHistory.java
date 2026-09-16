package com.epic60869.tastyfish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persistent farming analytics built from SkySoft's live cumulative FARMING session. */
public final class FarmingHistory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Data>() {}.getType();
    private static final long HOUR_MS = 60L * 60L * 1000L;
    private static final long FIVE_MINUTES_MS = 5L * 60L * 1000L;
    private static final long STREAK_GAP_MS = FIVE_MINUTES_MS;

    private final Path path;
    private Data data;
    private String currentSessionId = "";
    private long currentStartWall = 0L;
    private long lastWall = 0L;
    private long lastActive = -1L;
    private double lastProfit = 0.0;
    private long lastActions = 0L;
    private final Map<String, Long> lastItems = new HashMap<>();
    private final Map<String, Long> lastPests = new HashMap<>();
    private final List<Sample> samples = new ArrayList<>();
    private long streakStartWall = 0L;
    private long bestStreakMs = 0L;
    private boolean sessionEnded = true;

    public FarmingHistory(Path path) {
        this.path = path;
        this.data = load();
        this.bestStreakMs = data.bestStreakMs;
    }

    public synchronized Update update(String sessionId, SkysoftSessionReader.Snapshot snapshot) {
        if (snapshot == null || !snapshot.valid()) return Update.none();
        long now = System.currentTimeMillis();

        if (!sessionId.equals(currentSessionId) || sessionEnded) {
            currentSessionId = sessionId;
            currentStartWall = now;
            lastWall = now;
            lastActive = snapshot.activeMillis();
            lastProfit = snapshot.profit();
            lastActions = snapshot.actions();
            lastItems.clear(); lastItems.putAll(snapshot.items());
            lastPests.clear(); lastPests.putAll(snapshot.pests());
            samples.clear();
            samples.add(new Sample(now, snapshot.activeMillis(), snapshot.profit()));
            streakStartWall = now;
            sessionEnded = false;
            return Update.none();
        }

        boolean activeProgress = snapshot.activeMillis() > lastActive || snapshot.profit() > lastProfit;
        if (activeProgress) {
            if (now - lastWall > STREAK_GAP_MS) {
                bestStreakMs = Math.max(bestStreakMs, Math.max(0L, lastWall - streakStartWall));
                data.bestStreakMs = bestStreakMs;
                streakStartWall = now;
            }

            addDeltas(data.totalItems, lastItems, snapshot.items());
            addDeltas(data.totalPests, lastPests, snapshot.pests());
            data.totalActiveMillis += Math.max(0L, snapshot.activeMillis() - lastActive);
            data.totalProfit += Math.max(0.0, snapshot.profit() - lastProfit);
            data.totalActions += Math.max(0L, snapshot.actions() - lastActions);

            lastItems.clear(); lastItems.putAll(snapshot.items());
            lastPests.clear(); lastPests.putAll(snapshot.pests());
            lastActions = snapshot.actions();
            lastActive = snapshot.activeMillis();
            lastProfit = snapshot.profit();
            lastWall = now;
            samples.add(new Sample(now, snapshot.activeMillis(), snapshot.profit()));
            pruneSamples(snapshot.activeMillis());
            save();
        }

        long streakMs = Math.max(0L, now - streakStartWall);
        boolean newStreak = streakMs > bestStreakMs;
        if (newStreak) {
            bestStreakMs = streakMs;
            data.bestStreakMs = streakMs;
            save();
        }

        long oneHourProfit = rollingOneHourProfit(snapshot);
        long fiveMinuteProfit = rollingFiveMinuteProfit(snapshot);
        long sessionProfit = Math.max(0L, Math.round(Math.max(0.0, snapshot.profit() - samples.get(0).profit())));
        int completedFiveMinuteIntervals = (int) Math.max(0L, snapshot.activeMillis() / FIVE_MINUTES_MS);
        boolean fiveMinuteReport = completedFiveMinuteIntervals > 0 &&
            completedFiveMinuteIntervals > lastReportedFiveMinuteInterval;
        if (fiveMinuteReport) lastReportedFiveMinuteInterval = completedFiveMinuteIntervals;

        if (oneHourProfit > data.bestOneHourProfit) {
            data.bestOneHourProfit = oneHourProfit;
            data.bestOneHourAt = now;
            data.bestOneHourCrop = bestCrop(snapshot.items());
            save();
            return new Update(false, true, newStreak, oneHourProfit, streakMs, null,
                fiveMinuteReport, fiveMinuteProfit, sessionProfit, completedFiveMinuteIntervals);
        }
        return new Update(false, false, newStreak, oneHourProfit, streakMs, null,
            fiveMinuteReport, fiveMinuteProfit, sessionProfit, completedFiveMinuteIntervals);
    }

    private int lastReportedFiveMinuteInterval = 0;

    public synchronized Update finish(String reason, SkysoftSessionReader.Snapshot snapshot) {
        if (sessionEnded || snapshot == null || !snapshot.valid()) return Update.none();
        long now = System.currentTimeMillis();
        long duration = Math.max(0L, snapshot.activeMillis());
        long streak = Math.max(0L, lastWall - streakStartWall);
        bestStreakMs = Math.max(bestStreakMs, streak);
        data.bestStreakMs = bestStreakMs;

        Session session = new Session(currentSessionId, Instant.ofEpochMilli(currentStartWall).toString(),
            Instant.ofEpochMilli(now).toString(), reason == null ? "ended" : reason, duration,
            snapshot.profit(), snapshot.actions(), snapshot.items(), snapshot.pests(), bestCrop(snapshot.items()));
        data.sessions.add(session);
        while (data.sessions.size() > 100) data.sessions.remove(0);
        save();
        sessionEnded = true;
        return new Update(true, false, false, rollingOneHourProfit(snapshot), streak, session,
            false, rollingFiveMinuteProfit(snapshot), sessionProfit(snapshot), lastReportedFiveMinuteInterval);
    }

    private static void addDeltas(Map<String, Long> totals, Map<String, Long> previous, Map<String, Long> current) {
        for (Map.Entry<String, Long> entry : current.entrySet()) {
            long before = previous.getOrDefault(entry.getKey(), 0L);
            long after = entry.getValue() == null ? 0L : entry.getValue();
            if (after > before) totals.merge(entry.getKey(), after - before, Long::sum);
        }
    }

    private long rollingOneHourProfit(SkysoftSessionReader.Snapshot snapshot) {
        long currentActive = snapshot.activeMillis();
        double currentProfit = snapshot.profit();
        double oldestProfit = 0.0;
        for (Sample sample : samples) {
            if (currentActive - sample.activeMillis >= HOUR_MS) oldestProfit = sample.profit();
        }
        return Math.max(0L, Math.round(Math.max(0.0, currentProfit - oldestProfit)));
    }

    private long rollingFiveMinuteProfit(SkysoftSessionReader.Snapshot snapshot) {
        long currentActive = snapshot.activeMillis();
        double currentProfit = snapshot.profit();
        double oldestProfit = samples.isEmpty() ? currentProfit : samples.get(0).profit();
        for (Sample sample : samples) {
            if (currentActive - sample.activeMillis >= FIVE_MINUTES_MS) oldestProfit = sample.profit();
            else break;
        }
        return Math.max(0L, Math.round(Math.max(0.0, currentProfit - oldestProfit)));
    }

    private long sessionProfit(SkysoftSessionReader.Snapshot snapshot) {
        if (samples.isEmpty()) return 0L;
        return Math.max(0L, Math.round(Math.max(0.0, snapshot.profit() - samples.get(0).profit())));
    }

    private void pruneSamples(long currentActive) {
        samples.removeIf(sample -> currentActive - sample.activeMillis > HOUR_MS + FIVE_MINUTES_MS);
    }

    private static String bestCrop(Map<String, Long> items) {
        String best = "Unknown"; long value = 0L;
        for (Map.Entry<String, Long> e : items.entrySet()) {
            if (e.getValue() != null && e.getValue() > value) { value = e.getValue(); best = e.getKey(); }
        }
        return best;
    }

    public synchronized Data data() { return data; }
    public synchronized long bestOneHourProfit() { return data.bestOneHourProfit; }
    public synchronized long bestStreakMs() { return data.bestStreakMs; }

    public synchronized List<String> newlyUnlockedAchievements() {
        List<String> unlocked = new ArrayList<>();
        long crops = data.totalItems.values().stream().mapToLong(Long::longValue).sum();
        long pests = data.totalPests.values().stream().mapToLong(Long::longValue).sum();
        check(unlocked, "FIRST_HARVEST", crops >= 1);
        check(unlocked, "MILLION_CROPS", crops >= 1_000_000);
        check(unlocked, "TEN_MILLION_CROPS", crops >= 10_000_000);
        check(unlocked, "MILLIONAIRE", data.totalProfit >= 1_000_000);
        check(unlocked, "BILLIONAIRE", data.totalProfit >= 1_000_000_000);
        check(unlocked, "ONE_HOUR_FARMER", data.bestOneHourProfit >= 1_000_000);
        check(unlocked, "FIVE_HOUR_STREAK", data.bestStreakMs >= 5L * 60L * 60L * 1000L);
        check(unlocked, "PEST_CONTROL", pests >= 1_000);
        data.unlockedAchievements.addAll(unlocked);
        if (!unlocked.isEmpty()) save();
        return unlocked;
    }

    private void check(List<String> out, String id, boolean condition) {
        if (condition && !data.unlockedAchievements.contains(id)) out.add(id);
    }

    private Data load() {
        try {
            if (Files.notExists(path)) return new Data();
            Data loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), DATA_TYPE);
            return loaded == null ? new Data() : loaded;
        } catch (Exception e) { System.err.println("[TastyFish] Failed to load farming history: " + e.getMessage()); return new Data(); }
    }

    private void save() {
        try { Files.createDirectories(path.getParent()); Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8); }
        catch (IOException e) { System.err.println("[TastyFish] Failed to save farming history: " + e.getMessage()); }
    }

    public record Update(boolean sessionEnded, boolean newOneHourPb, boolean newStreak, long oneHourProfit, long streakMs, Session session,
                         boolean fiveMinuteReport, long fiveMinuteProfit, long sessionProfit, int completedFiveMinuteIntervals) {
        static Update none() { return new Update(false, false, false, 0L, 0L, null, false, 0L, 0L, 0); }
    }

    public static final class Data {
        long totalActiveMillis;
        double totalProfit;
        long totalActions;
        long bestOneHourProfit;
        long bestOneHourAt;
        String bestOneHourCrop = "Unknown";
        long bestStreakMs;
        final Map<String, Long> totalItems = new HashMap<>();
        final Map<String, Long> totalPests = new HashMap<>();
        final List<String> unlockedAchievements = new ArrayList<>();
        final List<Session> sessions = new ArrayList<>();
    }

    public record Session(String sessionId, String startedAt, String endedAt, String reason, long activeMillis,
                          double profit, long actions, Map<String, Long> items, Map<String, Long> pests, String crop) {}
    private record Sample(long wallMillis, long activeMillis, double profit) {}
}
