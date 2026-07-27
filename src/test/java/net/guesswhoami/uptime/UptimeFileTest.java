package net.guesswhoami.uptime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UptimeFileTest {

    private final Gson gson = new Gson();

    @Test
    void roundTripsThroughJson() {
        final Instant startedAt = Instant.parse("2026-07-27T18:42:50Z");
        final Instant now = startedAt.plusSeconds(90);
        final UptimeFile snapshot = UptimeFile.of(startedAt, now);

        final UptimeFile parsed = gson.fromJson(gson.toJson(snapshot), UptimeFile.class);

        assertEquals(snapshot.startedAt(), parsed.startedAt());
        assertEquals(snapshot.uptimeSeconds(), parsed.uptimeSeconds());
        assertEquals(snapshot.uptimeHuman(), parsed.uptimeHuman());
    }

    @Test
    void computesUptimeSecondsFromStartedAtAndNow() {
        final Instant startedAt = Instant.parse("2026-07-27T18:42:50Z");
        final Instant now = startedAt.plusSeconds(4854);

        final UptimeFile snapshot = UptimeFile.of(startedAt, now);

        assertEquals(4854, snapshot.uptimeSeconds());
    }

    @Test
    void formatsMinutesOnlyWhenUnderAnHour() {
        assertEquals("20 minutes", UptimeFile.formatHuman(20 * 60));
    }

    @Test
    void formatsHoursAndMinutes() {
        assertEquals("1 hour 20 minutes", UptimeFile.formatHuman(80 * 60));
    }

    @Test
    void formatsSingularUnits() {
        assertEquals("1 hour 1 minute", UptimeFile.formatHuman(61 * 60));
    }

    @Test
    void omitsMinutesOnceAtLeastADayOld() {
        assertEquals("1 day 2 hours", UptimeFile.formatHuman((26 * 60 + 30) * 60));
    }

    @Test
    void formatsZeroUptimeAsZeroMinutes() {
        assertEquals("0 minutes", UptimeFile.formatHuman(0));
    }

    @Test
    void formatsWholeDaysWithoutTrailingZeroHours() {
        assertEquals("2 days", UptimeFile.formatHuman(2 * 24 * 60 * 60));
    }
}
