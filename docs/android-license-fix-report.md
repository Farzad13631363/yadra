# Android license verification and duration fix

Backups: `backups/license-fix-20261108-225134/` (six original files, preserving paths).

## Findings and changes

- The authenticated license list already supplies `isPerpetual`, `durationDays`, and `expiresAt`. The Android DTO previously dropped all three. Verification completion supplies only status, so its absent expiry previously replaced the real license expiry with an empty string.
- Duration now uses license metadata from the authenticated list. Only explicit `isPerpetual: true` with no expiry displays Persian lifetime. A real expiry takes precedence; missing plan information displays unknown. Challenge expiry is never used as license expiry. Dates display the actual server timestamp without inventing a calculated expiration from durationDays.
- Only successful device-bound verification stores a process-local grant. Initial activation is followed by verification. SharedPreferences retains identity hints only and cannot restore ACTIVE or duration claims. Starting verification invalidates the prior in-memory proof without deleting saved identity. Failure leaves access locked.
- MainActivity now passes verified diag status to the dashboard on resume. The locked-click handler is registered independently of Charts. Existing ECU connection conditions and transport code are unchanged.

## Confirmed product policy (implemented)

The product owner confirmed that a valid server-verified `softwareCode == "diag"` license includes ALL professional features. The former empty allowlist has been replaced with: Professional dashboard, ECU, Unknown ECU, Errors (read/clear DTCs), Sensors, Charts, Trip, AI, Actuators, KWP TEST, and ECU CODE TEST. Buy, Profile, Connection, Warning, Settings, and Support remain free. Unknown feature identifiers fail closed.

Dashboard draw/click authorization reads `LicenseManager.isActive(context)` directly rather than retaining a Boolean snapshot. Returning from BuyActivity redraws the dashboard in `MainActivity.onResume`; professional mode also requires the current verified grant. Existing ECU mutual-exclusion and transport conditions remain unchanged. All main-source Kotlin/Java and XML license references were inspected: no separate AI, Trip, DTC, chart, or advanced-test license gate was found to integrate.

`LicenseManager.isActive` now explicitly reads only the process-local verified proof and validates ACTIVE, exact diag software, nonempty license identity, and matching nonempty device identity. The device-bound challenge/signature verification flow is unchanged. SharedPreferences ACTIVE alone never grants access. No backend, database, order, payment, or live license operation was performed.

The contract provides no signed offline grant, offline TTL, or refresh endpoint. After process restart, users must sign in on the license screen and verify with the existing device key. No persisted ACTIVE value bypasses this requirement. No token is persisted by this change.

The earlier server snapshot's null expiration does not confirm that a purchased license is perpetual. Its live authenticated `isPerpetual` value is required.

## Confirmed-policy files and backups

Before every existing-file edit, originals were copied with paths preserved to `backups/diag-policy-20261108-225735/`.

Changed for this policy:
- `app/src/main/java/license/DiagEntitlement.kt`
- `app/src/main/java/license/LicenseManager.kt`
- `app/src/main/java/com/example/yadra/RotatingDashboardView.kt`
- `app/src/main/java/com/example/yadra/MainActivity.kt`
- `app/src/test/java/com/example/yadra/LicensePolicyTest.kt`
- `app/build.gradle.kts` (JUnit and instrumentation dependencies using existing catalog entries)
- `docs/android-license-fix-report.md`

New: `app/src/androidTest/java/com/example/yadra/LicenseManagerSecurityTest.kt`.

Policy unit cases cover active, inactive, expired, device mismatch, software mismatch, absent proof after restart, free items, and unknown identifiers. An instrumentation regression test covers actual persisted ACTIVE claims without process proof; run it only on a test device/emulator. Source checks confirm every dashboard item is mapped and no stale Boolean gate remains. Tests have not been executed: Gradle and APK builds are prohibited. Android Studio build/test and live device verification remain outstanding. No implementation blocker remains for the confirmed mapping; seamless offline restart access remains unsupported by the server contract.

## Validation

Added `LicensePolicyTest` for unknown duration, confirmed lifetime, conflicting expiry, and the confirmed professional entitlement/security policy. Tests have not been executed: Gradle and APK builds were expressly prohibited. Source-level checks inspected call sites, refresh wiring, and duration handling. No live server calls or license operations were performed. Wi-Fi, Bluetooth, OBD, RPM, and dashboard connection logic were not modified; device behavior remains untested.

## Files changed by this task

- `app/src/main/java/license/LicenseInfo.kt`
- `app/src/main/java/license/LicenseManager.kt`
- `app/src/main/java/license/YadraLicenseApi.kt`
- `app/src/main/java/license/DiagEntitlement.kt` (new)
- `app/src/main/java/com/example/yadra/BuyActivity.kt`
- `app/src/main/java/com/example/yadra/MainActivity.kt`
- `app/src/main/java/com/example/yadra/RotatingDashboardView.kt`
- `app/src/test/java/com/example/yadra/LicensePolicyTest.kt` (new)
- This report.

Other pre-existing workspace changes were preserved.
