# jack-ahh-android

Turn a rooted Android phone into a Bluetooth receiver for anything with a headphone output. Plug a PC into the phone's 3.5 mm
jack, and the app plays it live to your Bluetooth headphones.

Unofficial, rooted phones only. Android 10 or later.

```
PC audio out ──cable──▶ phone's headset jack ──app──▶ Bluetooth headphones
```

## Features

| Feature | What it does |
|---|---|
| Bluetooth playback | Plays over A2DP, Bluetooth's media profile, not the low-quality call one |
| Gain | A slider from 0 to 400% |
| Signal graph | The last 5 seconds on a dB scale, so quiet input still shows. It turns red when the input clips |
| Screen off | Runs as a foreground service. The notification has a **Stop** button |

## What you need

- **Root**, with Magisk or similar. [Why →](#why-does-it-need-root)
- **A cable that puts the audio on the jack's mic contact.** The headset jack's only input is its mic contact, and a plain
  3-ring aux cable shorts that to ground, so you hear near-silence. Use one of these:
  - a TRRS headset splitter, with the PC in the pink **mic** socket
  - a TRS-to-TRRS "microphone" cable
  - an RCA adapter combination that puts one channel on the mic contact

## Install

1. Build the app and install it. See [Building](docs/how-it-works.md#building).
2. Open it, and grant root access when Magisk asks.

## Use

1. Connect your Bluetooth headphones to the phone.
2. Plug the PC into the jack and start playing something.
3. Press **Start**.
4. Turn the PC's volume up from about 10–20% until the graph is tall but not red.

Press **Stop** to end it. After unplugging and plugging the cable back in, press **Stop**, then **Start**.

## FAQ

<details>
<summary><strong>Why does it need root?</strong></summary>

On some ROMs Android sees the headset plugged in but never offers its mic to apps, so nothing can record from the jack. The app
uses root to tell Android about it, the way Android would itself. [How it works →](docs/how-it-works.md)

</details>

<details>
<summary><strong>Why is it near silent?</strong></summary>

The cable isn't putting the audio on the mic contact. See [What you need](#what-you-need).

</details>

<details>
<summary><strong>Why is it mono?</strong></summary>

The headset jack has one input channel. To get the whole mix into it, turn on **Settings → Accessibility → Audio → Mono audio**
in Windows.

</details>

<details>
<summary><strong>Why is it distorted?</strong></summary>

A PC's output is much louder than a mic input expects. Turn the PC's volume down until the graph stops turning red. The gain
slider comes after the clipping, so it can't fix it.

</details>

<details>
<summary><strong>Is there a delay?</strong></summary>

Bluetooth adds about 200 ms. That's fine for music and noticeable for video.

</details>

<details>
<summary><strong>What does "Also register jack as an output" do?</strong></summary>

Some phones only power the mic input when they think headphones are plugged in. This also tells Android about the jack's
output. It's off by default, because it can pull other apps' sound to the jack.

</details>

## Contributing

[How it works](docs/how-it-works.md) covers what the app does under the hood, building it and the project layout.
