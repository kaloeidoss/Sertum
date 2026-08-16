# Contract-test media fixtures

Source: AndroidX Media3 test data
(`https://github.com/androidx/media`, `libraries/test_data/src/test/assets`),
Apache-2.0 licensed (same license as this project).

- `bear.flac`, `bear_32bit.flac`, `bear_uncommon_sample_rate.flac` — media/flac/
- `sample_alac.mp4`, `sample_alac_20bit.mp4` — media/mp4/
- `sample_96khz.wav`, `sample_192khz.wav`, `sine_24le.wav`, `sample_44khz.wav` — media/wav/

Additionally `gen-flac-*-*-*.flac` and `gen-alac-*-*-*.m4a` are 0.25 s synthetic
440 Hz sine files (public domain content) generated with FFmpeg 9.0 for the
44.1/48/96/192 kHz × 16/24-bit matrix.

Used only by `DecoderContractTest` (decode → PCM → backend contract). WAV/AIFF matrix
files for 44.1/48/96/192 kHz × 16/24-bit are generated at runtime by the test itself.
