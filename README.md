# 📍 GPS Spoofing App

An Android application for mocking GPS location using the built-in **Mock Location** feature in Developer Options. The app allows users to simulate any geographic location on their device.

## 🚀 Features

- Select a fake location by tapping on an integrated map (Google Maps or MapView)
- Manually enter latitude and longitude coordinates
- Start / stop mock location service
- Display real vs. fake location
- Works on Android 6.0 (API 23) to Android 13+ (API 33)

## 🛠 Installation & Setup

1. Install the APK on your Android device.
2. Enable **Developer Options**:
   - Go to `Settings > About Phone`
   - Tap `Build Number` 7 times
3. Go to `Settings > System > Developer Options` (location may vary by device)
4. Find **Select mock location app** (or similar name)
5. Choose your **GPS Spoofing** app from the list
6. Grant location permission when prompted after launching the app

## 📱 How to Use

1. Open the app.
2. Tap on the map to select a fake location OR enter latitude/longitude manually.
3. Press **Start Spoofing**.
4. Open any other app (Google Maps, WhatsApp, etc.) – your device will report the fake location.
5. Press **Stop Spoofing** to revert to the real GPS.

## 📸 Permissions Required

- `ACCESS_FINE_LOCATION` – to get real GPS and send mock location
- `ACCESS_MOCK_LOCATION` (automatically granted via Developer Options)

## 🧪 Technical Details

- Written in **Java** (Android SDK)
- Uses `LocationManager` and `setTestProviderLocation()`
- Mock location provider enabled via Developer Settings
- Works by overriding the system's last known location with user-provided coordinates

## ⚠️ Important Legal & Ethical Notice

> **This app is intended for development, testing, and educational purposes only.**  
> Spoofing your location may violate the Terms of Service of third-party apps (games, social media, ride-sharing, banking apps, etc.). Use at your own risk. The developer assumes no legal liability.

## 📄 License

MIT License – free for personal and educational use.

## 🤝 Contributions

Pull requests and improvements are welcome. Please keep the code ethical and respect user privacy.

## 📬 Contact

For issues or questions, open a GitHub issue or contact the developer directly.