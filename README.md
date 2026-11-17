# Jack Bridge

An Android app that turns a rooted phone into a Bluetooth audio receiver for a wired source. Audio comes into the phone's 3.5 mm headphone jack from a PC, for example, and the app plays it live to the phone's connected Bluetooth headphones.

```
PC audio out ──cable──▶ phone headset jack (mic input) ──app──▶ Bluetooth A2DP headphones
```

## Features

- Plays to the headphones over A2DP, Bluetooth's high-quality media profile, not the low-quality call profile (SCO).
- Gain slider from 0 to 400%.
- Scrolling 5-second signal graph on a dB scale. It turns red when the input clips.
- Runs as a foreground service, so it keeps working with the screen off. The notification has a Stop button.

## Requirements

- **Root** (Magisk or similar). See [How it works](#how-it-works).
- Android 10 or later (minSdk 29). Developed on an Exynos Galaxy S8 running an Android 15 ROM.
- **A cable that puts the audio on the jack's mic contact.** A phone headset jack has four contacts: left out, right out, ground and **mic in**. The mic contact is the only input. A plain 3-ring aux cable fully inserted shorts the mic contact to ground, and you get near-silence. Use one of these:
  - a TRRS headset splitter, with the PC plugged into the pink **mic** socket
  - a TRS-to-TRRS "microphone" cable
  - an RCA adapter combination that routes one channel onto the mic contact

## Limitations

- **Mono only.** The headset jack has a single input channel. When the app requests stereo capture, it gets two identical copies of that channel. To get the whole mix into one channel, turn on Windows **Settings → Accessibility → Audio → Mono audio**.
- **Mic-level input.** A PC speaker or line output is much louder than the jack's mic input expects. Start with the PC volume around 10–20% and raise it until the graph is tall but not red. Fix clipping on the PC side; the app's gain slider is applied after the clipping happens.
- **Latency.** Bluetooth A2DP adds about 200 ms. That's fine for music and noticeable for video.

## How it works

On some ROMs the kernel detects a plugged-in headset (`/sys/class/switch/h2w/state` reads `1`), but `WiredAccessoryManager` never tells `AudioService`. The wired headset mic then never appears as an input device, and apps can't record from it.

When you press **Start**, the app uses `su` to run `RootJack` through `app_process`. `RootJack` calls `IAudioService.setWiredDeviceConnectionState()` to register `AUDIO_DEVICE_IN_WIRED_HEADSET`, the same call the framework would normally make. `BridgeService` then:

1. Records from the wired headset mic with an `AudioRecord` (source `UNPROCESSED`, falling back to `VOICE_RECOGNITION`) at 48 kHz mono, with `setPreferredDevice()` set to the jack.
2. Applies the gain and writes the audio to an `AudioTrack` (`USAGE_MEDIA`, low-latency mode) with `setPreferredDevice()` set to the A2DP headphones.

Pressing **Stop** unregisters the device again. The ROM doesn't report plug or unplug events, so after re-plugging the cable press Stop, then Start.

The **Also register jack as an output** checkbox also registers the headset output. Some audio hardware only powers the mic input (mic bias) when it thinks headphones are plugged in. It's off by default, because it can pull other apps' audio to the jack.

## Building

The build needs JDK 17+ and the Android SDK (platform 35). Point `local.properties` at the SDK:

```properties
sdk.dir=D:/Android/sdk
```

Then:

```sh
./gradlew assembleDebug
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
```

`-g` grants the runtime permissions (microphone, notifications, Bluetooth). On first start, Magisk asks for root access; grant it.

## Project layout

| File | Purpose |
| --- | --- |
| `app/src/main/java/dev/jackbridge/MainActivity.java` | UI: start/stop, gain, signal graph, status |
| `app/src/main/java/dev/jackbridge/BridgeService.java` | Foreground service that records from the jack and plays to Bluetooth |
| `app/src/main/java/dev/jackbridge/RootJack.java` | Root helper run through `app_process`; registers the wired headset with `AudioService` |
| `app/src/main/java/dev/jackbridge/SignalGraph.java` | Scrolling dB signal graph |
