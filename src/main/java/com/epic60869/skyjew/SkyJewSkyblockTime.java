package com.epic60869.skyjew;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class SkyJewSkyblockTime {
    private static final long SKYBLOCK_EPOCH_MILLIS = 1_560_275_700_000L;

    private static final long HOUR_MILLIS = 50_000L;
    private static final long DAY_MILLIS = HOUR_MILLIS * 24L;
    private static final long MONTH_MILLIS = DAY_MILLIS * 31L;
    private static final long YEAR_MILLIS = MONTH_MILLIS * 12L;

    private static final DateTimeFormatter REAL_TIME_FORMAT =
        DateTimeFormatter.ofPattern("EEE d MMM yyyy HH:mm:ss VV", Locale.US);

    private SkyJewSkyblockTime() {}

    /**
     * Converts a SkyBlock calendar date (year, zero-based month, day) to Earth time.
     * The returned time is rendered in the player's local/system time zone.
     */
    public static ZonedDateTime toRealWorld(int year, int monthIndex, int day) {
        if (year < 1 || monthIndex < 0 || monthIndex >= 12 || day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid SkyBlock calendar date");
        }

        long offset = (long) (year - 1) * YEAR_MILLIS
            + (long) monthIndex * MONTH_MILLIS
            + (long) (day - 1) * DAY_MILLIS;

        return Instant.ofEpochMilli(SKYBLOCK_EPOCH_MILLIS + offset)
            .atZone(ZoneId.systemDefault());
    }

    public static String formatRealWorld(int year, int monthIndex, int day) {
        return REAL_TIME_FORMAT.format(toRealWorld(year, monthIndex, day));
    }
}
