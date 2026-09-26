package com.epic60869.skyjew.features.dungeons;

import com.epic60869.skyjew.SkyJewConfig;
import com.epic60869.skyjew.custom.util.Compat;
import com.epic60869.skyjew.features.FeatureConfigs;
import com.epic60869.skyjew.features.core.SkyJewLocation;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.DungeonManager;
import com.epic60869.skyjew.sb.skyblock.dungeon.secrets.Room;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NoammAddons' legit dungeon map (https://github.com/Noamm9/NoammAddons, CC0-1.0), ported from its
 * HotbarMapColorParser, MapUpdater and MapRenderer. The vanilla map is read into an 11x11 grid of rooms
 * and doors and redrawn in clean colours with checkmarks, room names or secret counts. Like the legit
 * build, only what the vanilla map shows is drawn. Room names and secret counts come from Skyblocker's
 * room matching ({@link DungeonManager}).
 * <p>
 * Everything is drawn in the vanilla map's 128x128 pixel space, so player heads line up with it.
 */
public final class NoammMap {
    private static final Identifier CHECK_GREEN = Compat.id("textures/gui/dungeonmap/green_check.png");
    private static final Identifier CHECK_WHITE = Compat.id("textures/gui/dungeonmap/white_check.png");
    private static final Identifier CHECK_UNKNOWN = Compat.id("textures/gui/dungeonmap/question.png");
    private static final Identifier CHECK_FAIL = Compat.id("textures/gui/dungeonmap/cross.png");

    /** World x/z of the centre of the north-west room, and the room grid size (NoammAddons' DungeonScanner). */
    private static final int START_X = -185;
    private static final int START_Z = -185;

    /** In the order a room can progress through; a room's state only ever moves to a lower ordinal. */
    enum State { GREEN, CLEARED, DISCOVERED, FAILED, UNOPENED, UNDISCOVERED }

    enum RoomType {
        BLOOD, CHAMPION, ENTRANCE, FAIRY, NORMAL, PUZZLE, RARE, TRAP;

        static RoomType fromMapColor(int color) {
            return switch (color) {
                case 18 -> BLOOD;
                case 74 -> CHAMPION;
                case 30 -> ENTRANCE;
                case 82 -> FAIRY;
                case 63, 85 -> NORMAL;
                case 66 -> PUZZLE;
                case 34 -> RARE;
                case 62 -> TRAP;
                default -> null;
            };
        }
    }

    enum DoorType {
        BLOOD, ENTRANCE, NORMAL, WITHER;

        static DoorType fromMapColor(int color) {
            return switch (color) {
                case 18 -> BLOOD;
                case 30 -> ENTRANCE;
                case 74, 82, 66, 62, 85, 63 -> NORMAL;
                case 119 -> WITHER;
                default -> null;
            };
        }
    }

    static final class Tile {
        final boolean room;
        final boolean door;
        final int x;
        final int z;
        RoomType type;
        DoorType doorType;
        State state = State.UNDISCOVERED;
        boolean separator;
        boolean opened;

        Tile(boolean room, boolean door, int x, int z) {
            this.room = room;
            this.door = door;
            this.x = x;
            this.z = z;
        }

        boolean unknown() {
            return !room && !door;
        }
    }

    private static final Tile[] GRID = new Tile[121];
    private static final byte[] CENTER_COLORS = new byte[121];
    private static final byte[] SIDE_COLORS = new byte[121];

    private static boolean calibrated;
    private static int startCornerX = 5;
    private static int startCornerY = 5;
    private static int mapRoomSize = 16;
    private static int halfRoom = 8;
    private static int halfTile = 10;
    private static int quarterRoom = 4;
    private static int parseStartX = 13;
    private static int parseStartY = 13;

    private NoammMap() {}

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> reset());
        SkyJewLocation.onAreaChange(area -> reset());
    }

    public static void reset() {
        for (int i = 0; i < GRID.length; i++) GRID[i] = null;
        calibrated = false;
        startCornerX = 5;
        startCornerY = 5;
        mapRoomSize = 16;
    }

    private static FeatureConfigs.DungeonMap config() {
        SkyJewConfig config = SkyJewConfig.current();
        return config == null ? null : config.dungeons.map;
    }

    public static boolean enabled() {
        FeatureConfigs.DungeonMap config = config();
        return config != null && config.style == FeatureConfigs.MapStyle.NOAMM;
    }

    public static int height() {
        FeatureConfigs.DungeonMap config = config();
        return config != null && config.extraInfo ? 140 : 128;
    }

    // ----- Map parsing (HotbarMapColorParser / MapUpdater / MapUtils) -----

    /** Called whenever Hypixel sends a map update for the dungeon map. */
    public static void onMapUpdate(MapItemSavedData data) {
        if (data == null || DungeonManager.isInBoss()) return;
        if (!calibrated) calibrated = calibrate(data);
        if (calibrated) updateRooms(data);
    }

    private static boolean calibrate(MapItemSavedData data) {
        int start = 0;
        int length = 0;
        int foundStart = -1;
        int foundLength = 0;
        for (int i = 0; i < data.colors.length; i++) {
            if (data.colors[i] == 30) {
                if (length == 0) start = i;
                length++;
            } else {
                if (length >= 16) {
                    foundStart = start;
                    foundLength = length;
                    break;
                }
                length = 0;
            }
        }
        if (foundStart < 0 || (foundLength != 16 && foundLength != 18)) return false;

        mapRoomSize = foundLength;
        switch (ScoreCalculator.floorNumber()) {
            case 0 -> { startCornerX = 22; startCornerY = 22; }
            case 1 -> { startCornerX = 22; startCornerY = 11; }
            case 2, 3 -> { startCornerX = 11; startCornerY = 11; }
            default -> {
                int x = foundStart & 127;
                int y = foundStart >> 7;
                startCornerX = x % (mapRoomSize + 4);
                startCornerY = y % (mapRoomSize + 4);
            }
        }
        halfRoom = mapRoomSize / 2;
        halfTile = halfRoom + 2;
        quarterRoom = halfRoom / 2;
        parseStartX = startCornerX + halfRoom;
        parseStartY = startCornerY + halfRoom;
        return true;
    }

    private static void readColors(MapItemSavedData data) {
        for (int x = 0; x <= 10; x++) {
            for (int y = 0; y <= 10; y++) {
                int mapX = parseStartX + x * halfTile;
                int mapY = parseStartY + y * halfTile;
                int index = y * 11 + x;
                if (mapX >= 128 || mapY >= 128) {
                    CENTER_COLORS[index] = 0;
                    SIDE_COLORS[index] = 0;
                    continue;
                }
                CENTER_COLORS[index] = data.colors[mapY * 128 + mapX];
                int sideIndex;
                if (x % 2 == 0 && y % 2 == 0) sideIndex = (mapY - halfRoom) * 128 + (mapX - halfRoom);
                else if (y % 2 == 1) sideIndex = mapY * 128 + mapX - 4;
                else sideIndex = (mapY - 4) * 128 + mapX;
                SIDE_COLORS[index] = sideIndex >= 0 && sideIndex < data.colors.length ? data.colors[sideIndex] : 0;
            }
        }
    }

    private static Tile scanTile(int x, int y) {
        int center = CENTER_COLORS[y * 11 + x] & 0xFF;
        int side = SIDE_COLORS[y * 11 + x] & 0xFF;
        int worldX = START_X + x * 16;
        int worldZ = START_Z + y * 16;
        Tile unknown = new Tile(false, false, worldX, worldZ);
        if (center == 0) return unknown;

        if (x % 2 == 0 && y % 2 == 0) {
            RoomType type = RoomType.fromMapColor(side);
            if (type == null) return unknown;
            Tile tile = new Tile(true, false, worldX, worldZ);
            tile.type = type;
            tile.state = switch (center) {
                case 18 -> type == RoomType.BLOOD ? State.DISCOVERED : type == RoomType.PUZZLE ? State.FAILED : tile.state;
                case 30 -> type == RoomType.ENTRANCE ? State.DISCOVERED : State.GREEN;
                case 34 -> State.CLEARED;
                case 85, 119 -> State.UNOPENED;
                default -> State.DISCOVERED;
            };
            return tile;
        }
        if (side == 0) {
            DoorType type = DoorType.fromMapColor(center);
            if (type == null) return unknown;
            Tile tile = new Tile(false, true, worldX, worldZ);
            tile.doorType = type;
            tile.state = center == 85 ? State.UNOPENED : State.DISCOVERED;
            return tile;
        }
        RoomType type = RoomType.fromMapColor(side);
        if (type == null) return unknown;
        Tile tile = new Tile(true, false, worldX, worldZ);
        tile.type = type;
        tile.state = State.DISCOVERED;
        tile.separator = true;
        return tile;
    }

    private static void updateRooms(MapItemSavedData data) {
        readColors(data);
        Minecraft mc = Minecraft.getInstance();
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                int index = z * 11 + x;
                Tile tile = GRID[index];
                Tile mapTile = scanTile(x, z);
                if (tile == null || tile.unknown()) {
                    GRID[index] = mapTile;
                    continue;
                }
                if (mapTile.unknown()) continue;

                if (mapTile.state.ordinal() < tile.state.ordinal() || (mapTile.room && tile.room && mapTile.type == RoomType.PUZZLE)) {
                    tile.state = mapTile.state;
                }
                if (mapTile.room && tile.room && mapTile.type != tile.type) tile.type = mapTile.type;
                if (mapTile.door && tile.door && mapTile.doorType == DoorType.WITHER && tile.doorType != DoorType.WITHER) {
                    tile.doorType = DoorType.WITHER;
                }

                if (tile.door && tile.doorType != DoorType.NORMAL) {
                    if (mapTile.door && mapTile.doorType == DoorType.WITHER) {
                        tile.opened = false;
                    } else if (!tile.opened) {
                        BlockPos pos = new BlockPos(tile.x, 69, tile.z);
                        if (mc.level != null && mc.level.isLoaded(pos)) {
                            BlockState state = mc.level.getBlockState(pos);
                            if (state.isAir() || state.is(Blocks.BARRIER)) tile.opened = true;
                        } else if (mapTile.door && mapTile.state == State.DISCOVERED) {
                            if (tile.doorType == DoorType.BLOOD) {
                                for (Tile t : GRID) {
                                    if (t != null && t.room && t.type == RoomType.BLOOD && t.state != State.UNOPENED) {
                                        tile.opened = true;
                                        break;
                                    }
                                }
                            } else {
                                tile.opened = true;
                            }
                        }
                    }
                }
            }
        }
    }

    // ----- Rooms grouped into unique rooms (UniqueRoom) -----

    private record Group(List<int[]> tiles, Tile main, int checkX, int checkY, String name, int found, int secrets, int tilesWide, int tilesTall) {}

    private static Tile tile(int x, int y) {
        if (x < 0 || y < 0 || x > 10 || y > 10) return null;
        return GRID[y * 11 + x];
    }

    private static List<Group> groups(boolean centerStyle) {
        List<Group> groups = new ArrayList<>();
        boolean[] seen = new boolean[121];
        for (int y = 0; y <= 10; y += 2) {
            for (int x = 0; x <= 10; x += 2) {
                Tile start = tile(x, y);
                if (start == null || !start.room || seen[y * 11 + x]) continue;
                List<int[]> rooms = new ArrayList<>();
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, y});
                seen[y * 11 + x] = true;
                while (!queue.isEmpty()) {
                    int[] p = queue.poll();
                    if (p[0] % 2 == 0 && p[1] % 2 == 0) rooms.add(p);
                    int[][] next = {{p[0] + 1, p[1]}, {p[0] - 1, p[1]}, {p[0], p[1] + 1}, {p[0], p[1] - 1}};
                    for (int[] n : next) {
                        Tile t = tile(n[0], n[1]);
                        if (t == null || !t.room || seen[n[1] * 11 + n[0]]) continue;
                        // Rooms only join through separators, never straight across a gap.
                        boolean fromSeparator = !(p[0] % 2 == 0 && p[1] % 2 == 0);
                        if (!t.separator && !fromSeparator) continue;
                        seen[n[1] * 11 + n[0]] = true;
                        queue.add(n);
                    }
                }
                groups.add(group(rooms, centerStyle));
            }
        }
        return groups;
    }

    private static Group group(List<int[]> rooms, boolean centerStyle) {
        rooms.sort(Comparator.<int[]>comparingInt(p -> p[0]).thenComparingInt(p -> p[1]));
        int[] topLeft = rooms.getFirst();
        Tile main = tile(topLeft[0], topLeft[1]);

        int cx = topLeft[0];
        int cy = topLeft[1];
        if (centerStyle && rooms.size() > 1) {
            Map<Integer, Integer> xCounts = new LinkedHashMap<>();
            Map<Integer, Integer> zCounts = new LinkedHashMap<>();
            for (int[] p : rooms) {
                xCounts.merge(p[0], 1, Integer::sum);
                zCounts.merge(p[1], 1, Integer::sum);
            }
            List<Map.Entry<Integer, Integer>> xs = new ArrayList<>(xCounts.entrySet());
            List<Map.Entry<Integer, Integer>> zs = new ArrayList<>(zCounts.entrySet());
            xs.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());
            zs.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());
            if (zs.size() == 1 || !zs.get(0).getValue().equals(zs.get(1).getValue())) {
                cx = xs.stream().mapToInt(Map.Entry::getKey).sum() / xs.size();
                cy = zs.getFirst().getKey();
            } else if (xs.size() == 1 || !xs.get(0).getValue().equals(xs.get(1).getValue())) {
                cx = xs.getFirst().getKey();
                cy = zs.stream().mapToInt(Map.Entry::getKey).sum() / zs.size();
            } else {
                cx = (xs.get(0).getKey() + xs.get(1).getKey()) / 2;
                cy = (zs.get(0).getKey() + zs.get(1).getKey()) / 2;
            }
        }

        String name = null;
        int found = 0;
        int secrets = 0;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (int[] p : rooms) {
            minX = Math.min(minX, p[0]);
            maxX = Math.max(maxX, p[0]);
            minZ = Math.min(minZ, p[1]);
            maxZ = Math.max(maxZ, p[1]);
            if (name != null) continue;
            Room room = DungeonManager.getRoomAtCorner(-200 + (p[0] / 2) * 32, -200 + (p[1] / 2) * 32);
            if (room != null && room.isMatched() && room.getName() != null) {
                name = room.getName();
                found = room.getFoundSecretCount();
                secrets = room.getMaxSecretCount();
            }
        }
        return new Group(rooms, main, cx, cy, name, found, secrets, (maxX - minX) / 2 + 1, (maxZ - minZ) / 2 + 1);
    }

    // ----- Rendering (MapRenderer) -----

    private static int colour(String value, int fallback) {
        try {
            return ChromaColour.Companion.specialToChromaRGB(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int tileColour(FeatureConfigs.DungeonMap c, Tile tile) {
        if (tile.door) {
            if (tile.state == State.UNOPENED) return colour(c.colorUnopenedDoor, 0xFF414141);
            return switch (tile.doorType) {
                case BLOOD -> colour(c.colorBloodDoor, 0xFFB20000);
                case ENTRANCE -> colour(c.colorEntranceDoor, 0xFF00FF00);
                case WITHER -> tile.opened ? colour(c.colorOpenWitherDoor, 0xFF794600) : colour(c.colorWitherDoor, 0xFF101010);
                default -> colour(c.colorRoomDoor, 0xFF794600);
            };
        }
        if (tile.state == State.UNOPENED) return colour(c.colorUnopened, 0xFF414141);
        return switch (tile.type) {
            case BLOOD -> colour(c.colorBlood, 0xFFB20000);
            case CHAMPION -> colour(c.colorMiniboss, 0xFFFFC800);
            case ENTRANCE -> colour(c.colorEntrance, 0xFF00FF00);
            case FAIRY -> colour(c.colorFairy, 0xFFE39BE2);
            case PUZZLE -> colour(c.colorPuzzle, 0xFF7B007B);
            case RARE -> colour(c.colorRare, 0xFFB2B2B2);
            case TRAP -> colour(c.colorTrap, 0xFFFF8200);
            default -> colour(c.colorRoom, 0xFF794600);
        };
    }

    /** Draws the map background, border, rooms and labels at the map origin (0, 0). Player heads are drawn by DungeonMap. */
    public static void render(GuiGraphicsExtractor graphics) {
        FeatureConfigs.DungeonMap c = config();
        if (c == null) return;
        int height = height();
        graphics.fill(0, 0, 128, height, colour(c.backgroundColor, 0x32FFFFFF));
        int border = colour(c.borderColor, 0xFFFFFFFF);
        for (int i = 0; i < c.borderWidth; i++) graphics.outline(-1 - i, -1 - i, 130 + 2 * i, height + 2 + 2 * i, border);
        if (!calibrated) return;

        graphics.pose().pushMatrix();
        graphics.pose().translate(startCornerX, startCornerY);
        renderRooms(graphics, c);
        renderLabels(graphics, c);
        graphics.pose().popMatrix();
    }

    private static void renderRooms(GuiGraphicsExtractor graphics, FeatureConfigs.DungeonMap c) {
        int connector = quarterRoom > 0 ? quarterRoom : 4;
        for (int y = 0; y <= 10; y++) {
            for (int x = 0; x <= 10; x++) {
                Tile tile = GRID[y * 11 + x];
                if (tile == null || tile.unknown() || tile.state == State.UNDISCOVERED) continue;
                if (tile.door && !doorVisible(x, y)) continue;
                int colour = tileColour(c, tile);
                int xOffset = (x >> 1) * (mapRoomSize + connector);
                int yOffset = (y >> 1) * (mapRoomSize + connector);
                boolean xEven = (x & 1) == 0;
                boolean yEven = (y & 1) == 0;
                if (xEven && yEven) {
                    if (tile.room) graphics.fill(xOffset, yOffset, xOffset + mapRoomSize, yOffset + mapRoomSize, colour);
                } else if (!xEven && !yEven) {
                    graphics.fill(xOffset, yOffset, xOffset + mapRoomSize + connector, yOffset + mapRoomSize + connector, colour);
                } else {
                    connector(graphics, xOffset, yOffset, connector, tile.door, !xEven, colour);
                }
            }
        }
    }

    /** A door is only drawn once both rooms it joins are on the map. */
    private static boolean doorVisible(int x, int y) {
        Tile a = (x & 1) == 1 ? tile(x - 1, y) : tile(x, y - 1);
        Tile b = (x & 1) == 1 ? tile(x + 1, y) : tile(x, y + 1);
        return a != null && b != null && a.room && b.room && a.state != State.UNDISCOVERED && b.state != State.UNDISCOVERED;
    }

    private static void connector(GuiGraphicsExtractor graphics, int x, int y, int doorWidth, boolean doorway, boolean vertical, int colour) {
        int doorwayOffset = mapRoomSize == 16 ? 5 : 6;
        int width = doorway ? 6 : mapRoomSize;
        int x1 = vertical ? x + mapRoomSize : x;
        int y1 = vertical ? y : y + mapRoomSize;
        if (doorway) {
            if (vertical) y1 += doorwayOffset;
            else x1 += doorwayOffset;
        }
        int w = vertical ? doorWidth : width;
        int h = vertical ? width : doorWidth;
        graphics.fill(x1, y1, x1 + w, y1 + h, colour);
    }

    private static void renderLabels(GuiGraphicsExtractor graphics, FeatureConfigs.DungeonMap c) {
        Font font = Minecraft.getInstance().font;
        float roomSize = mapRoomSize;
        float gap = quarterRoom;
        float fullCell = roomSize + gap;
        float checkSize = c.checkmarkScale * 10;

        for (Group group : groups(c.centerCheckmark)) {
            Tile main = group.main();
            if (main == null || main.state == State.UNDISCOVERED) continue;
            float cX = (group.checkX() / 2f) * fullCell + halfRoom;
            float cY = (group.checkY() / 2f) * fullCell + halfRoom;

            // Unopened rooms (grey) and rooms Skyblocker hasn't matched get a checkmark; entrance gets nothing.
            boolean named = group.name() != null && main.state != State.UNOPENED;
            if (main.type == RoomType.ENTRANCE && main.state != State.UNOPENED) continue;
            if (!named || c.checkmarkStyle == FeatureConfigs.CheckmarkStyle.CHECKMARKS) {
                checkmark(graphics, c, main.state, cX - checkSize / 2, cY - checkSize / 2, checkSize);
                continue;
            }

            int textColour = switch (main.state) {
                case GREEN -> 0xFF55FF55;
                case CLEARED -> 0xFFFFFFFF;
                case FAILED -> 0xFFFF0000;
                default -> 0xFFAAAAAA;
            };
            String secretText = group.found() + "/" + group.secrets();
            if (c.checkmarkStyle == FeatureConfigs.CheckmarkStyle.SECRETS) {
                centered(graphics, font, group.secrets() == 0 ? "0" : secretText, cX, cY - font.lineHeight / 2f * c.textScale, textColour, c.textScale);
                continue;
            }

            List<String> lines = new ArrayList<>(List.of(group.name().split(" ")));
            if (c.checkmarkStyle == FeatureConfigs.CheckmarkStyle.ROOM_NAME_SECRETS && group.secrets() > 0) lines.add(secretText);
            float scale = c.textScale;
            if (c.limitRoomNameSize) {
                float maxWidth = group.tilesWide() * roomSize + Math.max(0, group.tilesWide() - 1) * gap;
                float maxHeight = group.tilesTall() * roomSize + Math.max(0, group.tilesTall() - 1) * gap;
                float widest = 0;
                for (String line : lines) widest = Math.max(widest, font.width(line) * scale);
                float totalH = lines.size() * font.lineHeight * scale;
                if (widest > 0 && totalH > 0) {
                    scale = Math.max(0.39f, Math.min(c.textScale, Math.min(maxWidth / widest, maxHeight / totalH) * scale));
                }
            }
            float totalH = lines.size() * font.lineHeight * scale;
            float lineY = cY - totalH / 2;
            for (String line : lines) {
                centered(graphics, font, line, cX, lineY, textColour, scale);
                lineY += totalH / lines.size();
            }
        }
    }

    private static void checkmark(GuiGraphicsExtractor graphics, FeatureConfigs.DungeonMap c, State state, float x, float y, float size) {
        Identifier texture = switch (state) {
            case CLEARED -> CHECK_WHITE;
            case GREEN -> CHECK_GREEN;
            case FAILED -> CHECK_FAIL;
            case UNOPENED -> c.hideQuestionCheckmarks ? null : CHECK_UNKNOWN;
            default -> null;
        };
        if (texture == null) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(size / 16f, size / 16f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0f, 0f, 16, 16, 16, 16);
        graphics.pose().popMatrix();
    }

    private static void centered(GuiGraphicsExtractor graphics, Font font, String text, float x, float y, int colour, float scale) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, -font.width(text) / 2, 0, colour, true);
        graphics.pose().popMatrix();
    }

    /** NoammAddons' "Show Extra Info Under Map": two small lines under the 128px map. */
    public static void renderExtraInfo(GuiGraphicsExtractor graphics) {
        FeatureConfigs.DungeonMap c = config();
        if (c == null || !c.extraInfo || !ScoreCalculator.started()) return;
        Font font = Minecraft.getInstance().font;
        int crypts = ScoreCalculator.crypts();
        int deaths = ScoreCalculator.deaths();
        String secrets = "§6Secrets: §b" + ScoreCalculator.foundSecrets() + "§f/§e" + String.format(java.util.Locale.US, "%.0f%%", ScoreCalculator.secretPercentage());
        String cryptsStr = ScoreCalculator.colorByPercent(crypts, 6, false) + "Crypts: " + crypts;
        String score = "§eScore: " + ScoreCalculator.colorizeScore(ScoreCalculator.score()) + "§r";
        String deathsStr = "§cDeaths: " + ScoreCalculator.colorByPercent(deaths, 4, true) + deaths + "§r";
        String mimic = "§cM: " + (ScoreCalculator.mimicKilled() ? "§a§l✔§r" : "§c§l✖§r");
        String prince = "§eP: " + (ScoreCalculator.princeKilled() ? "§a§l✔§r" : "§c§l✖§r");
        centered(graphics, font, secrets + "    " + cryptsStr, 64, 124, 0xFFFFFFFF, 0.7f);
        centered(graphics, font, score + "   " + deathsStr + "   " + mimic + " " + prince, 64, 130, 0xFFFFFFFF, 0.7f);
    }
}
