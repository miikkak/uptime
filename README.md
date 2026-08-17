# uptime

A Velocity plugin that records how long the proxy has been running to a JSON file.

## Why

Anything that wants to show "the proxy has been up for X" - a status page, a monitoring script,
a Discord bot - would otherwise have to poll the proxy via RCON or infer it from process start
time. This plugin writes the current uptime to a file instead, so any process on the same host
can just read it.

## How it works

Records the proxy's start time once, at `ProxyInitializeEvent`, and writes `uptime.json` on
startup and every 60 seconds after that under its own plugin data directory
(`plugins/uptime/`, relative to wherever Velocity runs):

```json
{
  "startedAt": "2026-07-27T18:42:50Z",
  "uptimeSeconds": 4854,
  "uptimeHuman": "1 hour 20 minutes"
}
```

`uptimeHuman` rounds down to whole minutes, drops the minutes once the proxy has been up for at
least a day, and always includes the largest nonzero units (e.g. `"2 days 3 hours"`,
`"20 minutes"`, `"0 minutes"` right after startup).

## Requirements

- JDK 25 to build (Gradle toolchain-managed)
- Velocity 4.x

## Building

```bash
./gradlew build
```

## Releases

A merged PR labeled `release:major`, `release:minor`, or `release:patch` triggers
`semantic-release` on merge to `main`, which tags the resulting commit `vX.Y.Z`. That tag push
triggers the `Release` workflow, which builds the jar and attaches it to a GitHub Release.
`release:none` skips this entirely - use it for docs/CI-only changes.

## Testing a release build

Tagging `main` with `vX.Y.Z` (or running the `Release` workflow manually with a `tag` input)
builds the jar and attaches it to a GitHub Release. Download and drop it into a Velocity
server's `plugins/` directory to test:

```bash
gh release download vX.Y.Z -R miikkak/uptime -p '*.jar' -D /path/to/velocity/plugins/
```

There is no automated deploy yet - this is manual, on-demand testing only.

## Design notes

- Dependency versions are pinned in `gradle.lockfile` (`dependencyLocking` in `build.gradle.kts`)
  so CI vulnerability scanning has a real dependency graph to check. Gradle fails the build if a
  declared dependency's resolved version drifts from the lock - after bumping a version in
  `build.gradle.kts`, regenerate it with `./gradlew dependencies --write-locks` and commit the
  result alongside the change.
- `gson` is deliberately pinned to `2.8.0` and not shaded - it's the exact version Velocity
  itself bundles at runtime (verified against `META-INF/maven/com.google.code.gson/gson/pom.properties`
  in a production Velocity jar). `velocity-api:4.0.0` transitively declares `gson:2.14.0`, which
  would otherwise win ordinary conflict resolution over this pin - `configurations.all { resolutionStrategy.force(...) }`
  forces `2.8.0` project-wide so the build can't silently drift onto an API surface that isn't
  actually on the runtime classpath (was #45, closed once verified). Bumping the pin requires
  re-verifying against whatever Velocity actually ships, not an automated dependency update (see
  `renovate.json`, which excludes it from Renovate for this reason).
- The plugin's reported version (shown in Velocity's "Loaded plugin ..." log line) is generated
  from the Gradle project version at build time, so it can't drift from the jar filename.
- Unlike `online-players`, there's no "did anything change" check before writing - `uptimeSeconds`
  changes every write by definition, so the file is rewritten unconditionally on each tick.

## License

TBD
