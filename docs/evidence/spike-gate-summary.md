# M1 Spike Gate Summary (sanitized)

Date: 2026-08-16

## Findings

| Probe | Result |
|-------|--------|
| Java AudioTrack system path | FAIL — all source sample rates were resampled into a fixed 384 kHz mixer thread on the USB device |
| Native AAudio EXCLUSIVE, 16-bit | PASS — 6/6 sample rates opened with `actualRate == requested`, sharing mode EXCLUSIVE |
| Native AAudio EXCLUSIVE, 24-bit (PCM_I24_PACKED) | PASS — 6/6 sample rates, `actualFormat == requested` |
| Full matrix | 12/12 streams pass, 0 rate/format mismatches |
| Custom UAC2 takeover (control plane) | Feasible — all interfaces claimable with force=true, SET_INTERFACE accepted; ISO data plane unverified |

## Decision (ADR-0001)

- Primary USB-exclusive backend: **native AAudio EXCLUSIVE streams**.
- Custom UAC2 driver remains a **conditional fallback** with explicit triggers recorded in the private ADR.
- Instrument-grade bit-exact loopback measurement remains out of scope for V1.

Raw logs are archived in the private project workspace; only this sanitized summary is published.
