# Crypto Overlay App

A real-time BTC/USDT price monitoring Android application with an always-on-top overlay display and audio alerts.

## Features

✅ **Real-time Price Monitoring** - Live BTC/USDT price from Binance WebSocket  
✅ **Android Overlay Display** - Always-on-top floating window showing current price  
✅ **Price Alerts** - Automatic alerts when price hits ±50 target ranges  
✅ **Beep Sound Notification** - Audio alert when conditions are met  
✅ **Draggable Overlay** - Move the overlay window anywhere on screen  
✅ **Price Configuration** - Set base price and target price at startup  
✅ **Reset Function** - Reset prices and reconfigure anytime  
✅ **Persistent Storage** - Saved prices using SharedPreferences  

## How It Works

1. **Launch the App** - Opens main activity
2. **Set Prices** - Enter base price and target price (e.g., Base: 45000, Target: 100)
3. **Start Monitoring** - Begins WebSocket connection to Binance
4. **Overlay Appears** - Floating overlay shows live BTC/USDT price
5. **Alert Triggers** - When price reaches Base ± Target:
   - 🔔 Displays alert message
   - 🔊 Plays beep sound (1ms start)
   - Distinguishes between +50 and -50 targets

## Price Calculation

```
Upper Bound = Base Price + Target Price  (+50 target)
Lower Bound = Base Price - Target Price  (-50 target)
```

**Example:**
- Base Price: 45000
- Target Price: 100
- Upper Alert Trigger: 45100
- Lower Alert Trigger: 44900

## Technical Stack

- **Language:** Java/Kotlin
- **Architecture:** Android Native
- **WebSocket:** OkHttp 4.11.0
- **JSON:** Gson 2.10.1
- **UI:** Material Design
- **Overlay:** Android WindowManager
- **Storage:** SharedPreferences
- **Sound:** Android MediaPlayer

## Permissions Required

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## API Integration

- **Binance WebSocket Stream:** `wss://stream.binance.com:9443/ws/btcusdt@trade`
- **Public Stream** - No API keys required
- **Real-time Updates** - Trade price updates
- **Auto-Reconnect** - Reconnects every 5 seconds on failure

## Building the APK

### Prerequisites
- Android Studio 2021.3+
- Android SDK 34
- Java 11+
- Gradle 7.0+

### Build Steps

```bash
# Clone the repository
git clone https://github.com/sagarsingasane123-collab/Cryptooverlayapp.git
cd Cryptooverlayapp

# Build the APK
./gradlew assembleRelease

# Output APK
# app/build/outputs/apk/release/app-release.apk
```

### Install on Device

```bash
# Via adb
adb install -r app/build/outputs/apk/release/app-release.apk

# Or manually - Transfer APK to device and tap to install
```

## Usage

### First Launch
1. Grant overlay permission when prompted
2. Enter base price (current BTC price)
3. Enter target price (alert trigger offset)
4. Tap "Start Monitoring"

### During Monitoring
- Overlay appears in top-right corner
- Shows real-time BTC/USDT price
- Displays target range and status
- Audio alert plays when conditions met
- Drag overlay to reposition

### Reset
- Tap "Reset App" to clear prices
- Reconfigure new prices
- Restart monitoring

## Troubleshooting

### Overlay Not Showing
- Ensure "Display over other apps" permission is granted
- Check Android version (API 24+)
- Restart the app

### No Price Updates
- Check internet connection
- Verify Binance WebSocket is accessible
- Check app logs for errors

### Sound Not Playing
- Verify device volume is not muted
- Check media player configuration
- Ensure audio file exists (if using file-based beep)

## Future Enhancements

- [ ] Add percentage-based price alerts
- [ ] Multiple price target monitoring
- [ ] Custom beep sound selection
- [ ] Price history chart
- [ ] Multiple trading pairs (ETH/USDT, etc.)
- [ ] Notification panel alerts
- [ ] Export price history CSV

## License

MIT License - See LICENSE file

## Support

For issues or feature requests, create an issue on GitHub.

---

**Made with ❤️ for crypto traders**
