package net.guesswhoami.uptime;

import java.time.Duration;
import java.time.Instant;

/**
 * Snapshot written to {@code uptime.json} for external readers (scripts, website).
 *
 * <p>A plain mutable class, not a record — historically because the Gson version this project
 * appeared to target predated record support, but the actually-bundled version (2.14.0, see the
 * {@code gson} dependency comment in {@code build.gradle.kts}) does support records, so that
 * reasoning no longer applies. Left as a plain class for now as a style choice, not a technical
 * constraint; converting to a record would be a reasonable follow-up.
 */
final class UptimeFile {

    private String startedAt;
    private long uptimeSeconds;
    private String uptimeHuman;

    UptimeFile(final String startedAt, final long uptimeSeconds, final String uptimeHuman) {
        this.startedAt = startedAt;
        this.uptimeSeconds = uptimeSeconds;
        this.uptimeHuman = uptimeHuman;
    }

    static UptimeFile of(final Instant startedAt, final Instant now) {
        final long uptimeSeconds = Duration.between(startedAt, now).getSeconds();
        return new UptimeFile(startedAt.toString(), uptimeSeconds, formatHuman(uptimeSeconds));
    }

    String startedAt() {
        return startedAt;
    }

    long uptimeSeconds() {
        return uptimeSeconds;
    }

    String uptimeHuman() {
        return uptimeHuman;
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
