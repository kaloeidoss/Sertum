# Third-Party Notices

Project-owned code is licensed under the Apache License 2.0 (see `LICENSE`).

This file will list every third-party component bundled with or linked by the application, with its license and notice text, as dependencies are added to the project.

| Component | Version | License | Notice / Link |
|-----------|---------|---------|---------------|
| AndroidX Compose (BOM-managed: ui, ui-graphics, material3, tooling-preview) | 2026.06.01 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/compose |
| AndroidX Activity Compose | 1.12.4 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/activity |
| JUnit | 4.13.2 | EPL-1.0 | https://junit.org/junit4/ |
| Google Truth | 1.4.5 | Apache-2.0 | https://github.com/google/truth |

Update policy: every dependency added in `gradle/libs.versions.toml` must be listed here in the same commit, including transitive runtime dependencies that ship inside the APK.
