# Android license integration — 2026-11-08

Implemented against android-license-server-contract.md without changing the server.

- HTTPS API base: https://yadran.ir/api/v1. No license ID or customer credentials embedded.
- BuyActivity opens the existing trusted HTTPS login in a WebView, including the site's real Turnstile. JavaScript and DOM storage are enabled; mixed content and file/content access are disabled. Token capture is restricted to the exact origin and token is held in activity memory. No JavaScript native bridge or SSL bypass is installed.
- Customer licenses are read from data.licenses and diag licenses are offered for selection. Selecting a row confirms device binding. Previously activated rows use verify rather than activation.
- Both handshake purposes use eight UTF-8 lines with LF separators and no final newline. Android ID hashing, SPKI fingerprint and Keystore SHA256withECDSA DER signatures use existing DeviceIdentity/DeviceKeyStore implementations. Existing aliases are retained.
- API sockets and DNS use a validated internet Network; diagnostic Wi-Fi socket selection is unchanged. No process-wide network binding is added.
- HTTP errors include status and the sanitized server error code. 401 clears the in-memory customer token. No secrets or response bodies are logged. Complete is not automatically retried; after an uncertain result, reread the account list and verify an activated row.
- Stored ACTIVE is accepted only following successful proof in this process. Restart requires login and verification again; the server contract supplies no offline grant. Bearer tokens are not written to license preferences.

## Validation and remaining device checks

Git has existing staged/unstaged changes; these were preserved. Repository access used a per-command safe.directory setting without changing global Git configuration.

assembleDebug was attempted with locally discovered JDKs. JDK 25 cannot read conf/security/java.security (AccessDeniedException). JDK 21 starts Gradle, but this project's daemon criteria require JDK 25. With a writable project-local Gradle user home and the installed Gradle 9.8.2, provisioning JDK 25 fails with network Permission denied: getsockopt. SSL verification was not disabled. Compilation and runtime tests therefore have NOT passed.

An existing app/build/outputs/apk/debug/app-debug.apk is dated 2026-11-05 and predates these changes. It is NOT the output of this implementation. No new APK was produced.

On an unrestricted build environment, set JAVA_HOME to a readable JDK matching gradle/gradle-daemon-jvm.properties, then run gradlew.bat assembleDebug. Expected output: app/build/outputs/apk/debug/app-debug.apk.

Device acceptance checks still required:
1. Sign into the order owner's existing account and finish real Turnstile; verify API errors and 401 re-login.
2. Confirm the account list provides the test license ID (expected snapshot: 2, software diag), then explicitly select it. No real server activation was executed during development.
3. Verify successful first activation, restart/login/reverification, incorrect-device and expired/revoked responses.
4. Check real Keystore signature verification against the server, and byte-for-byte payload/SPKI fingerprint with a local test fixture.
5. Repeat with diagnostic Wi-Fi and mobile internet, then after adapter reconnect/disconnect, and confirm diagnostic commands still work.
6. Check Turnstile in the installed device WebView and rotation/cancellation/error recovery. Web login requires a default internet route; API calls separately select a validated internet network.
