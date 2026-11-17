package dev.jackbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Captures the 3.5mm jack mic input and plays it to the Bluetooth A2DP headphones. */
public class BridgeService extends Service {
    static final String ACTION_STOP = "dev.jackbridge.STOP";
    static final int RATE = 48000;

    static volatile boolean running;
    static volatile float gain = 1f;
    static volatile int peak;          // last input peak, 0..32767
    static volatile String status = "Stopped";
    static volatile boolean registerOutputToo;

    private Thread worker;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            running = false;
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        if (worker == null || !worker.isAlive()) {
            running = true;
            worker = new Thread(this::loop, "jack-bridge");
            worker.setPriority(Thread.MAX_PRIORITY);
            worker.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (worker != null) try { worker.join(1000); } catch (InterruptedException ignored) {}
        new Thread(() -> runRoot("off")).start();
        status = "Stopped";
        super.onDestroy();
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("bridge", "Jack Bridge", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, BridgeService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "bridge")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Jack → Bluetooth running")
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(null, "Stop", stop).build())
                .setOngoing(true)
                .build();
    }

    /** Runs RootJack as root. Returns its output. */
    String runRoot(String mode) {
        try {
            String apk = getApplicationInfo().sourceDir;
            Process p = new ProcessBuilder("su", "-c",
                    "CLASSPATH=" + apk + " app_process /system/bin dev.jackbridge.RootJack " + mode)
                    .redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) sb.append(l).append('\n');
            }
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "su failed: " + e;
        }
    }

    private AudioDeviceInfo find(AudioManager am, int flag, int type) {
        for (AudioDeviceInfo d : am.getDevices(flag)) if (d.getType() == type) return d;
        return null;
    }

    private void loop() {
        AudioManager am = getSystemService(AudioManager.class);
        status = "Registering jack via root…";
        String rootOut = runRoot(registerOutputToo ? "both" : "in");

        AudioDeviceInfo in = null;
        for (int i = 0; i < 20 && in == null; i++) {
            in = find(am, AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_WIRED_HEADSET);
            if (in == null) try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        AudioDeviceInfo out = find(am, AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);

        int inMin = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int outMin = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord rec;
        try {
            rec = new AudioRecord(MediaRecorder.AudioSource.UNPROCESSED, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, inMin * 2);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                rec.release();
                rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, inMin * 2);
            }
        } catch (SecurityException e) {
            status = "Microphone permission missing";
            running = false;
            stopSelf();
            return;
        }
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(outMin * 2)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        if (in != null) rec.setPreferredDevice(in);
        if (out != null) track.setPreferredDevice(out);

        rec.startRecording();
        track.play();
        status = "Running\nroot: " + rootOut
                + "\nin:  " + (in != null ? "wired headset jack" : "NOT FOUND, using built-in mic")
                + "\nout: " + (out != null ? out.getProductName() + " (A2DP)" : "no A2DP device, default output");

        short[] buf = new short[256];
        while (running) {
            int n = rec.read(buf, 0, buf.length);
            if (n <= 0) continue;
            float g = gain;
            int pk = 0;
            for (int i = 0; i < n; i++) {
                int s = buf[i];
                if (Math.abs(s) > pk) pk = Math.abs(s);
                s = (int) (s * g);
                buf[i] = (short) Math.max(-32768, Math.min(32767, s));
            }
            peak = pk;
            track.write(buf, 0, n);
        }
        rec.stop(); rec.release();
        track.stop(); track.release();
        peak = 0;
    }
}
