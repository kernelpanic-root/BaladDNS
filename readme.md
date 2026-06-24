# ADNS

ADNS is a lightweight DNS-based ad blocker for Android. No VPN, no background services, no battery drain, no hassle. NextDNS integration is built in for a seamless experience.

Download it from [GitHub Releases](https://github.com/eyalm2000/adns/releases).

## Features

Android makes DNS controls hard to find and slow to toggle. ADNS makes it fast and accessible.

- Toggle DNS on/off with a single tap
- Use your NextDNS account and edit its settings directly within the app,
- or use public servers like AdGuard, Cloudflare, or any other hostname
- Quick Settings tile for instant access
- State notification for at-a-glance status
  
Beautifully crafted with Material 3 Expressive and Jetpack Compose.

## Activation

ADNS writes to global DNS settings, which requires access for `WRITE_SECURE_SETTINGS`.

You can grant access using:

- [Shizuku](https://github.com/RikkaApps/Shizuku) (recommended)
- ADB shell

## State of NextDNS Implementation

Almost all of NextDNS's web dashboard features and settings are natively implemented into ADNS. However, there are a few exceptions:
- Full Logs view 
- Setup guides for other devices 
- Account settings (change email, password, manage plan)
- Recreation Time in Parental Control settings
- A few options within the Profile Settings: Logs configurations, Rewrites, and Access. 

Everything else is fully integrated, meaning you can manage almost any NextDNS setting straight from ADNS! The ultimate goal is to have all of these remaining features natively implemented within the app.

<br>
<br>
<p align="center">
    <img src="fastlane\metadata\android\en-US\images\phoneScreenshots\3.png" alt="ADNS main screen" width="30%">
    <img src="fastlane\metadata\android\en-US\images\phoneScreenshots\2.png" alt="ADNS settings screen" width="30%">
    <img src="fastlane\metadata\android\en-US\images\phoneScreenshots\1.png" alt="ADNS settings screen" width="30%">
</p>
