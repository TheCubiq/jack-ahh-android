# How it works

What the app does under the hood, and how to build it. The [README](../README.md) covers using it.

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
