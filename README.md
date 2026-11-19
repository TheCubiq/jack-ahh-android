# jack-ahh-android

Plug a PC into a rooted Android phone's headphone jack, hear it on your Bluetooth headphones.

Android 10+, root.

```
PC ──cable──▶ phone's jack ──▶ Bluetooth headphones
```

## Features

| Feature | What it does |
|---|---|
| A2DP | Media-quality Bluetooth, not call quality |
| Gain | 0–400% |
| Signal graph | Last 5 s, red when clipping |
| Screen off | Keeps going, stop from the notification |

## Setup

You need a cable that puts the audio on the jack's **mic** contact: a TRRS splitter (PC in the pink socket) or a TRS-to-TRRS mic cable. A plain aux cable gives near-silence.

1. [Build](docs/how-it-works.md#building) and install the app. Grant root.
2. Connect your headphones, plug in the PC, press **Start**.
3. Raise the PC's volume from ~15% until the graph is tall but not red.

Replugged the cable? **Stop**, then **Start**.

## FAQ

<details>
<summary><strong>Why root?</strong></summary>

Some ROMs never offer the headset mic to apps. Root tells Android it's there.

</details>

<details>
<summary><strong>Why mono?</strong></summary>

The jack has one input. Turn on Windows **Mono audio** to get the whole mix.

</details>

<details>
<summary><strong>Delay?</strong></summary>

About 200 ms from Bluetooth.

</details>

## More

[How it works](docs/how-it-works.md): the root trick, building, layout.
