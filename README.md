**Languages:** [English](README.md) | [简体中文](README.zh.md)

# Sertum

**Sertum** (花环播放器) is a fully offline, local-first music player for Android, built for listeners who keep their own audio library and want unaltered, bit-perfect playback through USB DACs.

## Status

Early development. M0 is complete: the repository builds a runnable Compose app skeleton with CI (unit tests, lint, offline-manifest guard). No app features have shipped yet. See `CHANGELOG.md` for human-readable iteration notes.

## Planned V1 features

- 100% offline; no account, no telemetry, no network permission
- USB exclusive bit-perfect output (implementation path decided by a real-device spike)
- Standard Android output and Bluetooth as regular playback paths
- Library scanning: system media index + user-selected folders + optional full-disk scan
- Library views: Songs / Albums / Artists, with Artist → Albums → Tracks browsing
- Gapless playback and per-track resume position
- Album cover completion for albums without embedded artwork
- Chinese and English UI; dark/light themes with near-black + warm-gold design tokens
- Resilient behavior: DAC hotplug, corrupt files, permission revocation

## Planned tech stack

- Kotlin, Jetpack Compose, Media3 ExoPlayer, Room, Hilt, Coil
- Android 10+ (minSdk 29), targetSdk 36, arm64-v8a first
- Apache License 2.0 for project-owned code

## Repository layout

- `app/` — Android application module
- `gradle/` — Gradle wrapper and version catalog
- `scripts/` — offline guard and device check helpers
- `.github/workflows/` — CI pipeline
- `CHANGELOG.md` — human-readable iteration notes
- `README.zh.md` — Simplified Chinese version of this document
- `LICENSE` — Apache License 2.0

## License

Project-owned code is licensed under the Apache License 2.0. Third-party components keep their own licenses; a complete third-party notice file will be added with the first code commit.
