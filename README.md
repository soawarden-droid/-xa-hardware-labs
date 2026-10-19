# XA PHANTOM — Mobile Cybersecurity Laboratory

An offline-first Android 8+ security learning and diagnostics application. Built as native Java without third-party runtime libraries.

## Features
- Device security overview (Android version, API level, patch date, architecture, model)
- Network interface addresses and connectivity status
- DNS hostname resolution (on-demand)
- Single TCP connection test for a user-entered host and port (on-demand; 3-second timeout)
- TLS certificate information for a user-entered HTTPS host (on-demand)
- Share diagnostics report using Android Sharesheet
- Permission-light: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE; no root or location access
- Visible errors and startup crash diagnostics

## Scope and safety
Use only with hosts or services you own or have permission to assess. No stealth scanning, brute force, exploit delivery, credential collection, or background monitoring. The application never uploads a report automatically. IP/DNS/TLS diagnostics require network access. Information shown is limited by Android permissions and sandboxing. A TCP connection success is not a vulnerability finding.

## Build
GitHub Actions builds a direct downloadable APK in [Releases](../../releases). The test APK is debug signed. It is not a Play Store production release. No USB, ZIP handling, Termux, or local Android Studio required for users downloading the APK.

## Test plan
1. Launch on Android 10 and verify no crash.
2. Tap Device Audit, Network Snapshot, DNS Lookup, TCP Check, TLS Check.
3. Test with a domain and port you control.
4. Test invalid hostname and offline behavior.
5. Share a report and verify only user-selected content leaves the app.

**Not yet verified on physical devices.**
