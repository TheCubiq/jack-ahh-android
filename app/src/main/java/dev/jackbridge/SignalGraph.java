package dev.jackbridge;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Scrolling signal-over-time graph of the input peaks. Newest on the right; red = clipping. */
public class SignalGraph extends View {
    private static final int CLIP = 32000;
    private final Paint bar = new Paint();
    private final Paint clip = new Paint();
    private final Paint grid = new Paint();
    private final Paint bg = new Paint();

    public SignalGraph(Context c) {
        super(c);
        bar.setColor(Color.rgb(64, 196, 255));
        clip.setColor(Color.rgb(255, 82, 82));
        grid.setColor(Color.argb(60, 255, 255, 255));
        bg.setColor(Color.argb(40, 255, 255, 255));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        float mid = h / 2f;
        canvas.drawRect(0, 0, w, h, bg);
        canvas.drawLine(0, mid, w, mid, grid);

        short[] hist = BridgeService.history;
        int n = hist.length, end = BridgeService.historyPos;
        float step = (float) w / n;
        for (int i = 0; i < n; i++) {
            int v = hist[(end - n + i) & (n - 1)];
            float half = v / 32767f * mid;
            float x = i * step;
            canvas.drawRect(x, mid - half, x + Math.max(step, 1f), mid + half, v >= CLIP ? clip : bar);
        }
        if (BridgeService.running) postInvalidateOnAnimation();
    }
}
