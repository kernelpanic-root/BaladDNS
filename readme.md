# BaladDNS

BaladDNS is a lightweight DNS-based ad blocker for Android. No VPN, no background services, no battery drain, no hassle. NextDNS integration is built in for a seamless experience.

> This project is a fork of [adns](https://github.com/eyalm2000/adns) by Eyal Meirom, modified and maintained independently.

## Features

Android makes DNS controls hard to find and slow to toggle. BaladDNS makes it fast and accessible.

- Toggle DNS on/off with a single tap
- Use your NextDNS account and edit its settings directly within the app
- Or use public servers like AdGuard, Cloudflare, or any other hostname
- Quick Settings tile for instant access
- State notification for at-a-glance status

Built with Material 3 Expressive and Jetpack Compose.

## Activation

BaladDNS writes to global DNS settings, which requires access for `WRITE_SECURE_SETTINGS`.

You can grant access using:

- [Shizuku](https://github.com/RikkaApps/Shizuku) (recommended)
- ADB shell

```
adb shell pm grant com.kernelpanic.baladdns android.permission.WRITE_SECURE_SETTINGS
```

## Building

```bash
git clone https://github.com/kernelpanic-root/BaladDNS.git
cd BaladDNS
./gradlew assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/`

## License

MIT — see [LICENSE](LICENSE). Original copyright retained per license terms; modifications and additions are maintained by [kernelpanic-root](https://github.com/kernelpanic-root).
