# Changelog

Human-readable iteration notes for the public repository. Fine-grained history lives in commit messages.

## 2026-08-16 — M3 scanning and library

- Room schema v1: tracks/albums/artists/covers with album identity key (album artist > artist > folder fallback).
- Metadata model and GBK/GB18030 ID3v2.3 text fixer with sample-based unit tests.
- Three scan sources behind one ScanCandidate model: MediaStore, SAF folders, optional full-disk scan (20k cap).
- ScanEngine: path-normalized dedupe, incremental add/update diff, orphan cleanup.
- Cover storage (non-destructive app-private files) and four-level cover priority resolver.

## 2026-08-16 — M2 audio core

- AudioOutputBackend contract, BitPerfectState and volume policy with unit tests.
- Media3 1.11 playback engine, StandardBackend, custom AIFF extractor (16/24/32-bit PCM).
- Queue engine (repeat/shuffle/remove/move), resume position store, playback coordinator with gapless queue and sample-rate hooks.
- Production native AAudio EXCLUSIVE backend; device smoke passed 8/8 (16/24-bit × 4 sample rates, zero mismatches).
- Gapless smoke: same-rate transitions near-seamless; sample-rate reconfiguration latency on the standard path is tracked as a follow-up before the bit-perfect acceptance gate.

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
