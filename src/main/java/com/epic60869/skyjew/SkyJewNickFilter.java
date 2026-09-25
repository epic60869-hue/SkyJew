package com.epic60869.skyjew;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Blocks swearing and slurs in nicknames. Text is normalised first (lowercase, common
 * letter substitutions like 4→a and $→s, repeated letters collapsed) so simple tricks do not
 * get through. Short words that often appear inside normal words are only matched as whole words.
 */
public final class SkyJewNickFilter {
    /** Matched anywhere in the name. */
    private static final List<String> ANYWHERE = List.of(
        "fuck", "fuk", "shit", "cunt", "bitch", "nigger", "nigga", "niga", "faggot", "fagot", "retard",
        "whore", "slut", "kike", "tranny", "hitler", "asshole", "bastard", "pussy", "dildo",
        "porn", "nazi", "penis", "vagina", "rapist", "molest");
    /** Matched only as a whole word, because they appear inside normal words (class, peacock, grape, ...). */
    private static final List<String> WHOLE_WORD = List.of(
        "ass", "cock", "dick", "fag", "kys", "rape", "twat", "tits", "sex", "coon", "spic", "chink", "cum", "pedo", "wank");

    private static final Pattern REPEATS = Pattern.compile("(.)\\1+");

    private SkyJewNickFilter() {}

    /** Whether the nickname contains a blocked word. */
    public static boolean isBlocked(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.toLowerCase(Locale.ROOT)
            .replace('0', 'o').replace('1', 'i').replace('!', 'i').replace('3', 'e')
            .replace('4', 'a').replace('@', 'a').replace('5', 's').replace('$', 's')
            .replace('7', 't').replace('8', 'b');

        // Words separated by anything that is not a letter; collapsed version for "fuuuck" and "f.u.c.k".
        String spaced = lower.replaceAll("[^a-z]+", " ").trim();
        String joined = spaced.replace(" ", "");
        String collapsed = REPEATS.matcher(joined).replaceAll("$1");

        for (String word : ANYWHERE) {
            if (joined.contains(word) || collapsed.contains(REPEATS.matcher(word).replaceAll("$1"))) return true;
        }
        for (String word : spaced.split(" ")) {
            String single = REPEATS.matcher(word).replaceAll("$1");
            if (WHOLE_WORD.contains(word) || WHOLE_WORD.contains(single)) return true;
        }
        return false;
    }
}
