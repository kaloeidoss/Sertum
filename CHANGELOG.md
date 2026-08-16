# Changelog

Human-readable iteration notes for the public repository. Fine-grained history lives in commit messages.

## 2026-08-16 — M1 USB-exclusive spike

- Built and ran Spike-1 (Java AudioTrack + native AAudio) and Spike-2 (UAC2 takeover probe) on the reference device.
- Evidence: Java system path resamples everything to a fixed 384 kHz mixer (fail); native AAudio EXCLUSIVE passes the full 16/24-bit × 6-rate matrix with zero mismatches (pass); UAC2 control plane is claimable as a fallback.
- Decision: native AAudio EXCLUSIVE is the V1 USB-exclusive backend; UAC2 is a conditional fallback (ADR-0001, private workspace).

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
