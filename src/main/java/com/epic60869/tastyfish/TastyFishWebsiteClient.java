package com.epic60869.tastyfish;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Read-only client for the public TastyFish leaderboard API. */
public final class TastyFishWebsiteClient {
    public record Result(
        boolean available,
        String boardId,
        String boardName,
        long value,
        int position,
        String aheadName,
        long aheadValue,
        int aheadPosition
    ) {
        public static Result empty() {
            return new Result(false, "", "", 0L, -1, "", 0L, -1);
        }

        public long behind() {
            return Math.max(0L, aheadValue - value);
        }

        public boolean hasAheadPlayer() {
            return available && aheadPosition > 0 && !aheadName.isBlank() && behind() > 0;
        }
    }

    private record Row(String username, String uuid, long value, int position) {}

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private volatile Result result = Result.empty();
    private volatile long nextRefreshMillis = 0L;
    private volatile String lastCrop = "";
    private volatile String lastUsername = "";
    private volatile String lastUuid = "";

    public void tick(TastyFishConfig config, String username, UUID uuid, SkysoftSessionReader.Snapshot snapshot) {
        if (config == null || !config.guildLeaderboardHudEnabled || snapshot == null || !snapshot.valid()) return;
        if (username == null || username.isBlank()) return;

        String crop = currentCrop(snapshot);
        if (crop.isBlank()) return;

        String uuidText = uuid == null ? "" : uuid.toString().replace("-", "");
        long now = System.currentTimeMillis();
        long interval = Math.max(10L, config.guildLeaderboardRefreshSeconds) * 1000L;
        if (now < nextRefreshMillis && crop.equalsIgnoreCase(lastCrop)
            && username.equalsIgnoreCase(lastUsername) && uuidText.equalsIgnoreCase(lastUuid)) return;

        if (!refreshing.compareAndSet(false, true)) return;
        nextRefreshMillis = now + interval;
        lastCrop = crop;
        lastUsername = username;
        lastUuid = uuidText;

        refresh(config.guildLeaderboardWebsite, crop, username, uuidText)
            .whenComplete((value, error) -> {
                if (error == null && value != null) result = value;
                else if (error != null) System.err.println("[TastyFish] Guild leaderboard refresh failed: " + error.getMessage());
                refreshing.set(false);
            });
    }

    public Result result() {
        return result;
    }

    public void clear() {
        result = Result.empty();
        nextRefreshMillis = 0L;
    }

    private CompletableFuture<Result> refresh(String website, String crop, String username, String uuid) {
        String base = normalizeBase(website);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(base + "/api/leaderboards"))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("HTTP " + response.statusCode());
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                String boardId = findBoardId(root, crop);
                if (boardId == null) return CompletableFuture.completedFuture(Result.empty());
                return fetchBoard(base, boardId, crop, username, uuid);
            });
    }

    private CompletableFuture<Result> fetchBoard(String base, String boardId, String crop, String username, String uuid) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(base + "/api/leaderboards/" + encode(boardId)))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("HTTP " + response.statusCode());
                }
                return parseBoard(JsonParser.parseString(response.body()).getAsJsonObject(), boardId, crop, username, uuid);
            });
    }

    private static Result parseBoard(JsonObject board, String boardId, String crop, String username, String uuid) {
        String boardName = string(board, "name", boardId);
        List<Row> rows = new ArrayList<>();
        JsonArray array = board.has("rows") && board.get("rows").isJsonArray() ? board.getAsJsonArray("rows") : new JsonArray();

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String name = string(row, "username", string(row, "name", ""));
            String rowUuid = normalizeUuid(string(row, "uuid", ""));
            long value = number(row, "value", number(row, "amount", 0L));
            int position = intNumber(row, "position", i + 1);
            if (!name.isBlank()) rows.add(new Row(name, rowUuid, value, position));
        }

        rows.sort(Comparator.comparingInt(Row::position).thenComparing(Row::username));
        Row mine = null;
        for (Row row : rows) {
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(row.uuid())) {
                mine = row;
                break;
            }
            if (username.equalsIgnoreCase(row.username())) mine = row;
        }
        if (mine == null) return Result.empty();

        Row ahead = null;
        for (Row row : rows) {
            if (row.position() < mine.position()) {
                if (ahead == null || row.position() > ahead.position()) ahead = row;
            }
        }

        return new Result(
            true,
            boardId,
            boardName.isBlank() ? prettyCrop(crop) : boardName,
            mine.value(),
            mine.position(),
            ahead == null ? "" : ahead.username(),
            ahead == null ? 0L : ahead.value(),
            ahead == null ? -1 : ahead.position()
        );
    }

    private static String findBoardId(JsonObject root, String crop) {
        JsonArray boards = root.has("leaderboards") && root.get("leaderboards").isJsonArray()
            ? root.getAsJsonArray("leaderboards") : new JsonArray();
        String wanted = normalize(crop);
        String compactWanted = wanted.replace(" ", "");

        String fallback = null;
        for (JsonElement element : boards) {
            if (!element.isJsonObject()) continue;
            JsonObject board = element.getAsJsonObject();
            String id = string(board, "id", "");
            String name = string(board, "name", "");
            String apiValue = string(board, "apiValue", "");
            String label = string(board, "valueLabel", "");
            String haystack = normalize(id + " " + name + " " + apiValue + " " + label);
            String compact = haystack.replace(" ", "");
            if (haystack.contains(wanted) || compact.contains(compactWanted)) {
                return id;
            }
            if (aliases(crop).stream().anyMatch(alias -> haystack.contains(alias) || compact.contains(alias.replace(" ", "")))) {
                fallback = id;
            }
        }
        return fallback;
    }

    private static List<String> aliases(String crop) {
        String c = normalize(crop);
        List<String> values = new ArrayList<>();
        values.add(c);
        switch (c) {
            case "cocoa bean", "cocoa beans", "cocoabean", "cocoabeans" -> {
                values.add("cocoa"); values.add("cocoabean"); values.add("cocoabeans");
            }
            case "sugar cane", "sugarcane" -> { values.add("sugar cane"); values.add("sugarcane"); }
            case "nether wart", "netherwart" -> { values.add("nether wart"); values.add("netherwart"); }
            case "mushroom", "mushrooms" -> values.add("mushroom");
            default -> { }
        }
        return values;
    }

    private static String currentCrop(SkysoftSessionReader.Snapshot snapshot) {
        String best = "";
        long count = 0L;
        for (var entry : snapshot.items().entrySet()) {
            long value = entry.getValue() == null ? 0L : entry.getValue();
            if (value > count) {
                count = value;
                best = prettyCrop(entry.getKey());
            }
        }
        return best;
    }

    private static String prettyCrop(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replace('_', ' ')
            .replace(" item", "")
            .trim();
    }

    private static String normalizeBase(String value) {
        String base = value == null || value.isBlank() ? "https://tastyfish.org" : value.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String normalizeUuid(String value) {
        return value == null ? "" : value.replace("-", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull() ? fallback : value.getAsString();
        } catch (Exception ignored) { return fallback; }
    }

    private static long number(JsonObject object, String key, long fallback) {
        try {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull() ? fallback : Math.round(value.getAsDouble());
        } catch (Exception ignored) { return fallback; }
    }

    private static int intNumber(JsonObject object, String key, int fallback) {
        try { return object.get(key).getAsInt(); } catch (Exception ignored) { return fallback; }
    }
}
