package dev.jackbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView statusView, gainLabel;
    private SignalGraph graph;
    private Button toggle;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.BLUETOOTH_CONNECT}, 1);

        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText("Jack → Bluetooth");
        title.setTextSize(26);
        root.addView(title);

        toggle = new Button(this);
        toggle.setTextSize(20);
        toggle.setOnClickListener(v -> {
            Intent i = new Intent(this, BridgeService.class);
            if (BridgeService.running) startService(i.setAction(BridgeService.ACTION_STOP));
            else startForegroundService(i);
        });
        root.addView(toggle, new LinearLayout.LayoutParams(-1, -2));

        gainLabel = new TextView(this);
        gainLabel.setPadding(0, pad, 0, 0);
        root.addView(gainLabel);
        SeekBar gain = new SeekBar(this);
        gain.setMax(400);
        gain.setProgress((int) (BridgeService.gain * 100));
        gain.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean u) { BridgeService.gain = p / 100f; }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        root.addView(gain);

        TextView meterLabel = new TextView(this);
        meterLabel.setText("Input signal, dB (last ~5 s, red = clipping)");
        meterLabel.setPadding(0, pad, 0, 0);
        root.addView(meterLabel);
        graph = new SignalGraph(this);
        root.addView(graph, new LinearLayout.LayoutParams(-1, pad * 6));

        CheckBox outToo = new CheckBox(this);
        outToo.setText("Also register jack as an output (try if the input stays silent)");
        outToo.setChecked(BridgeService.registerOutputToo);
        outToo.setOnCheckedChangeListener((b, c) -> BridgeService.registerOutputToo = c);
        outToo.setPadding(0, pad, 0, 0);
        root.addView(outToo);

        statusView = new TextView(this);
        statusView.setPadding(0, pad, 0, 0);
        statusView.setGravity(Gravity.START);
        root.addView(statusView);

        setContentView(root);
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            toggle.setText(BridgeService.running ? "Stop" : "Start");
            gainLabel.setText("Gain: " + Math.round(BridgeService.gain * 100) + "%");
            graph.invalidate();
            statusView.setText(BridgeService.status);
            ui.postDelayed(this, 100);
        }
    };

    @Override protected void onResume() { super.onResume(); ui.post(tick); }
    @Override protected void onPause() { super.onPause(); ui.removeCallbacks(tick); }
}
