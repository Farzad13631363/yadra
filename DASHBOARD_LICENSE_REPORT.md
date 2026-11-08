# Dashboard licensing repair

Backups of every pre-existing file changed in this repair are in `backups/dashboard-complete-20261108-230537/`, preserving their original relative paths. New test/report files had no prior version. Earlier workspace changes were preserved.

## Findings for every item

| Stable ID | Existing action/screen | Licensing and other blockers found before this repair |
|---|---|---|
| PROFILE | VehicleProfileActivity | Free; no license blocker |
| BUY | BuyActivity | Free; explicit account/purchase action |
| CONNECTION | ConnectionActivity | Free; no license blocker |
| WARNING | WarningsActivity | Free; no license blocker |
| SENSORS | SensorsActivity | Requires verified diag; no additional purchase gate |
| ERRORS | ErrorsActivity | Requires verified diag; no additional purchase gate |
| ECU | EcuTestActivity | Requires verified diag; blocked while Unknown ECU is connected; incorrectly showed purchase dialog even with verified diag |
| UNKNOWN_ECU | UnknownEcuScannerActivity | Requires verified diag; blocked while normal ECU is connected over Wi-Fi or Bluetooth; incorrectly showed purchase dialog even with verified diag |
| KWP_TEST | KwpTestActivity | Requires verified diag; no additional purchase gate |
| ECU_CODE_TEST | ActuatorCodeTestActivity | Requires verified diag; no additional purchase gate |
| ACTUATORS | ActuatorTestActivity | Requires verified diag; no additional purchase gate |
| AI | AIActivity | Requires verified diag; no additional purchase gate |
| SETTINGS | SettingsActivity | Free; no license blocker |
| TRIP | TripActivity | Requires verified diag; no additional purchase gate |
| CHARTS | ChartsActivity | Requires verified diag; no additional purchase gate |

The supplied source already listed all 15 labels across paid/free policy sets. There is no evidence that an omitted label was blocking a verified license in this snapshot. The concrete conflicting gates were ECU mode safety restrictions routed through the same purchase callback as licensing. Label-based matching was also fragile, and verification had no immediate dashboard notification (refresh depended on MainActivity resuming).

## Changes

- `app/src/main/java/license/DiagEntitlement.kt`: defines all 15 stable IDs; verified diag covers every ID, with the five existing free dashboard items still free. Compatibility string checks use the same ID policy.
- `app/src/main/java/license/LicenseManager.kt`: emits process-local verification-change notifications. Existing device-bound server-proof validation and refusal to restore persisted ACTIVE remain in place.
- `app/src/main/java/com/example/yadra/MainActivity.kt`: assigns all IDs, retains all 15 original destination callbacks, subscribes to verification notifications for immediate redraw/mode refresh, removes the listener on destruction, and displays a connection-specific message for ECU mode conflicts. Exposes the production item factory internally for callback regression tests.
- `app/src/main/java/com/example/yadra/RotatingDashboardView.kt`: both menu touch paths dispatch through stable-ID licensing followed by separate ECU connection checks. Only a license denial invokes the purchase callback.
- Added `app/src/test/java/com/example/yadra/DashboardEntitlementTest.kt`, `app/src/androidTest/java/com/example/yadra/DashboardClickPathsTest.kt`, `tools/check_dashboard_policy.py`, and this report.

Inspected BuyActivity and YadraLicenseApi: account-selected diag licenses use existing server challenge/signature verification. BuyActivity remains the explicit purchase/account screen; it is not an extra purchase requirement on licensed diagnostic items. Neither file needed modification. No backend, database, Wi-Fi, Bluetooth, OBD, diagnostic operation, stored license, or activation state was changed by this repair.

## Verification and limits

`python tools/check_dashboard_policy.py`: 3 tests passed. These inspect all 15 production ID-to-destination callbacks, both touch dispatch paths, immediate refresh wiring, and absence of purchase/license gates in the destination activity source.

Added JVM coverage for entitlement decisions for all 15 IDs and Android coverage that executes every production callback and captures its actual destination Intent. These Kotlin tests were not run: Gradle and APK builds were explicitly prohibited. Compilation and device interaction remain unverified. No fake activation or server call was performed.

A verified diag license removes all dashboard license restrictions. ECU mode mutual exclusion intentionally remains, with a connection explanation. Destination screens still require their existing connection, supported ECU/protocol, data, permissions, and hardware prerequisites. This repair does not add unavailable diagnostic implementations. Process restart still requires fresh online proof; saved ACTIVE is not accepted. No additional premium gate was found in the inspected dashboard or its 15 direct destination activities.
