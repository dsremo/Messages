# Messages

A privacy-focused SMS/MMS app for Android, forked from [Fossify Messages](https://github.com/FossifyOrg/Messages) with:

- An ML-lite fraud / promotional-SMS classifier
- Indian-SMS-specific whitelists and heuristics (DLT sender parsing, bank sender icons, postal-code checks)
- OTP handling extras (auto-copy, guarded outbound OTPs, auto-delete of expired OTPs)
- Missed-call SMS detection and scheduled-send rate limiting
- Chakshu (India) fraud-report helper
- Similar-sender auto-block and message-thread insights

## Build

```
./gradlew --no-daemon assembleFossRelease
```

Signing config is read from `keystore.properties` (see `keystore.properties_sample`). The generated APK lands under `app/build/outputs/apk/foss/release/`.

## License

GPL-3.0. See [LICENSE](LICENSE). Upstream project: <https://github.com/FossifyOrg/Messages>.

This is a modified version of Fossify Messages, redistributed under the same license (GPL-3.0). Modifications include the fraud/promo classifier, Indian-SMS heuristics, and other extras listed above.
