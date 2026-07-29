package net.guesswhoami.uptime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.slf4j.Logger;

/**
 * Writes {@code uptime.json} once on startup and then on a fixed interval, so other processes can
 * read how long the proxy has been running without an RCON round trip.
 */
final class UptimeService {

    // Files.createTempFile() defaults to owner-only permissions on POSIX (a deliberate JDK
    // security default for temp files); since uptime.json is produced by an atomic move of that
    // temp file, it would otherwise inherit those restrictive permissions and be unreadable to
    // anything but the proxy's own user - defeating the point of writing it for external readers.
    // Must be applied via setPosixFilePermissions() after creation, not createTempFile()'s
    // FileAttribute varargs - confirmed empirically that this JDK silently ignores permissions
    // requested that way and creates the file at the umask-restricted default regardless.
    private static final Set<PosixFilePermission> WORLD_READABLE_PERMISSIONS =
            PosixFilePermissions.fromString("rw-r--r--");

    private static final Duration WRITE_INTERVAL = Duration.ofSeconds(60);

    private final UptimePlugin plugin;
    private final ProxyServer server;
    private final Path dataDirectory;
    private final Logger logger;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path outputFile;
    private final Instant startedAt = Instant.now();

    // System.nanoTime() is monotonic and immune to wall-clock adjustments (NTP sync, manual
    // changes), unlike Instant.now(). Elapsed time is derived from this baseline rather than
    // Instant.now() - startedAt, so an admin correcting the system clock can't make uptime jump
    // or go negative.
    private final long startNanos = System.nanoTime();

    UptimeService(final UptimePlugin plugin, final ProxyServer server, final Path dataDirectory, final Logger logger) {
        this.plugin = plugin;
        this.server = server;
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.outputFile = dataDirectory.resolve("uptime.json");
    }

    void start() {
        try {
            Files.createDirectories(dataDirectory);
        } catch (final IOException e) {
            logger.error("Could not create data directory {}: {}", dataDirectory, e.getMessage());
            return;
        }

        write();

        server.getScheduler()
                .buildTask(plugin, this::write)
                .delay(WRITE_INTERVAL)
                .repeat(WRITE_INTERVAL)
                .schedule();

        logger.info("Writing {} now and every {}", outputFile, WRITE_INTERVAL);
    }

    private void write() {
        final Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        final UptimeFile snapshot = UptimeFile.of(startedAt, startedAt.plus(elapsed));
        writeAtomic(outputFile, gson.toJson(snapshot));
    }

    // Package-private (not private) so the permissions behavior is directly unit-testable.
    void writeAtomic(final Path target, final String content) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile(dataDirectory, target.getFileName().toString(), ".tmp");
            Files.setPosixFilePermissions(tmp, WORLD_READABLE_PERMISSIONS);
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            tmp = null; // moved successfully - nothing left to clean up
        } catch (final IOException e) {
            logger.error("Failed to write {}: {}", target, e.getMessage());
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (final IOException ignored) {
                    // best effort - a leftover .tmp file isn't worth a second error log
                }
            }
        }
    }
}
