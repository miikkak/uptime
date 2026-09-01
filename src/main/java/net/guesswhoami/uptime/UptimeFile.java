package net.guesswhoami.uptime;

import java.time.Duration;
import java.time.Instant;

/** Snapshot written to {@code uptime.json} for external readers (scripts, website). */
record UptimeFile(String startedAt, long uptimeSeconds, String uptimeHuman) {

    static UptimeFile of(final Instant startedAt, final Instant now) {
        final long uptimeSeconds = Duration.between(startedAt, now).getSeconds();
        return new UptimeFile(startedAt.toString(), uptimeSeconds, formatHuman(uptimeSeconds));
    }

    // Rounds down to whole minutes - second-level precision isn't meaningful for a value that's
    // primarily read by humans on a status page.
    static String formatHuman(final long uptimeSeconds) {
        final long totalMinutes = uptimeSeconds / 60;
        final long days = totalMinutes / (24 * 60);
        final long hours = (totalMinutes % (24 * 60)) / 60;
        final long minutes = totalMinutes % 60;

        final StringBuilder result = new StringBuilder();
        appendUnit(result, days, "day");
        appendUnit(result, hours, "hour");
        if (days == 0) {
            // Minutes only matter for a fresh proxy - once it's been up for a day, nobody cares
            // about the trailing minutes.
            appendUnit(result, minutes, "minute");
        }

        return result.length() == 0 ? "0 minutes" : result.toString().trim();
    }

    private static void appendUnit(final StringBuilder result, final long value, final String unit) {
        if (value == 0) {
            return;
        }
        result.append(value).append(' ').append(unit).append(value == 1 ? "" : "s").append(' ');
    }
}
