# Lyra AI (personal Android YKS assistant)
Minimal black screen / animated blue orb / single input and mic. No menu bars, banners, filters, or titles. Long-press the blue orb to enter your own Google AI Studio API key.
Model priority: Gemini 4 Hydrogen, Gemini 3.8 Flash as backup. Model API identifiers and free-tier availability must be checked against the linked AI Studio project; the app does not switch billing on.
The Android system assistant can be selected via Settings > Default Apps > Digital Assistant App where OEM settings permit it. Screen capture is conditional on Android delivering it.
Build: gradle :app:assembleDebug (JDK 17; Android SDK 35; Gradle 8.11.1). Debug APK: app/build/outputs/apk/debug/app-debug.apk.
First build is an untested prototype. No guarantee of compatibility on every Samsung firmware.
