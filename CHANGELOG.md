# Changelog

Human-readable iteration notes for the public repository. Fine-grained history lives in commit messages.

## 2026-08-15 — M0 project scaffold

- Buildable Android app skeleton: AGP 9.2 (built-in Kotlin), Jetpack Compose, minSdk 29 / targetSdk 36.
- Offline boundary: manifest declares zero permissions (no INTERNET).
- CI workflow: unit tests, lint, offline-manifest guard, debug APK assembly.
- Gradle wrapper 9.7.0; Compose BOM pinned to 2026.06.01 for compileSdk 36 compatibility.

## 2026-08-15 — bilingual README

- Added `README.zh.md` (Simplified Chinese) and a language switcher at the top of `README.md`.

## 2026-08-15 — repository bootstrap

- Created the public repository skeleton: `README.md`, Apache-2.0 `LICENSE`, `.gitignore`, and this changelog policy.
- No application code yet. Private planning documents (requirements, decisions, plans) are maintained outside this repository.
