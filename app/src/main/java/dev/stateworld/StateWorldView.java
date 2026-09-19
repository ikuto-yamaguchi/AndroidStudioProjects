package dev.stateworld;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StateWorldView extends View {
    private final StateModel state;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<PointF> trail = new ArrayList<>();
    private final RectF mapRect = new RectF();
    private final RectF anchorButton = new RectF();
    private final RectF markButton = new RectF();
    private final RectF clearButton = new RectF();
    private long lastTrailAt = 0L;
    private final float density;

    private static final int INK = Color.rgb(26, 29, 28);
    private static final int MUTED = Color.rgb(105, 108, 102);
    private static final int PAPER = Color.rgb(247, 246, 242);
    private static final int CARD = Color.rgb(255, 255, 252);
    private static final int ACCENT = Color.rgb(36, 90, 80);
    private static final int ACCENT_SOFT = Color.rgb(218, 233, 225);
    private static final int WARM = Color.rgb(212, 118, 70);

    StateWorldView(Context context, StateModel state) {
        super(context);
        this.state = state;
        density = getResources().getDisplayMetrics().density;
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        setBackgroundColor(PAPER);
        setFocusable(true);
        setClickable(true);
    }

    private float dp(float v) { return v * density; }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        float left = dp(18);
        float right = w - dp(18);

        drawHeader(c, left, right);

        float mapTop = dp(112);
        float mapBottom = Math.min(h * 0.54f, mapTop + dp(330));
        mapRect.set(left, mapTop, right, mapBottom);
        drawMap(c);

        float infoTop = mapBottom + dp(18);
        drawStateInfo(c, left, right, infoTop);

        float sensorsTop = infoTop + dp(76);
        drawSensors(c, left, right, sensorsTop);

        float buttonTop = Math.max(sensorsTop + dp(130), h - dp(88));
        drawButtons(c, left, right, buttonTop);
    }

    private void drawHeader(Canvas c, float left, float right) {
        p.setColor(INK);
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        p.setTextSize(dp(27));
        c.drawText("STATEWORLD", left, dp(48), p);

        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        p.setTextSize(dp(12));
        p.setColor(MUTED);
        c.drawText("Reality has another coordinate system.", left, dp(69), p);

        String badge = "GPS OFF  •  LOCAL ONLY";
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        p.setTextSize(dp(9.5f));
        float tw = p.measureText(badge);
        RectF r = new RectF(right - tw - dp(18), dp(31), right, dp(55));
        p.setColor(ACCENT_SOFT);
        c.drawRoundRect(r, dp(12), dp(12), p);
        p.setColor(ACCENT);
        c.drawText(badge, r.left + dp(9), r.centerY() + dp(3.5f), p);
    }

    private void drawMap(Canvas c) {
        p.setShader(new LinearGradient(mapRect.left, mapRect.top, mapRect.right, mapRect.bottom,
                Color.rgb(229, 237, 229), Color.rgb(239, 229, 218), Shader.TileMode.CLAMP));
        c.drawRoundRect(mapRect, dp(24), dp(24), p);
        p.setShader(null);

        c.save();
        Path clip = new Path();
        clip.addRoundRect(mapRect, dp(24), dp(24), Path.Direction.CW);
        c.clipPath(clip);

        stroke.setColor(Color.argb(30, 20, 40, 35));
        stroke.setStrokeWidth(dp(1));
        for (int i = 1; i < 6; i++) {
            float x = mapRect.left + mapRect.width() * i / 6f;
            c.drawLine(x, mapRect.top, x, mapRect.bottom, stroke);
            float y = mapRect.top + mapRect.height() * i / 6f;
            c.drawLine(mapRect.left, y, mapRect.right, y, stroke);
        }

        p.setColor(Color.argb(28, 27, 90, 78));
        c.drawCircle(mapRect.left + mapRect.width() * .22f, mapRect.top + mapRect.height() * .32f, dp(82), p);
        p.setColor(Color.argb(22, 188, 104, 65));
        c.drawCircle(mapRect.left + mapRect.width() * .79f, mapRect.top + mapRect.height() * .74f, dp(104), p);
        p.setColor(Color.argb(18, 72, 88, 135));
        c.drawCircle(mapRect.left + mapRect.width() * .73f, mapRect.top + mapRect.height() * .20f, dp(66), p);

        float[] xy = state.currentMap();
        PointF now = toMap(xy[0], xy[1]);
        recordTrail(now);

        if (trail.size() > 1) {
            Path path = new Path();
            path.moveTo(trail.get(0).x, trail.get(0).y);
            for (int i = 1; i < trail.size(); i++) path.lineTo(trail.get(i).x, trail.get(i).y);
            stroke.setColor(Color.argb(125, 36, 90, 80));
            stroke.setStrokeWidth(dp(2.4f));
            c.drawPath(path, stroke);
        }

        PointF anchor = toMap(0f, 0f);
        stroke.setColor(Color.argb(115, 26, 29, 28));
        stroke.setStrokeWidth(dp(1.5f));
        c.drawCircle(anchor.x, anchor.y, dp(8), stroke);
        c.drawLine(anchor.x - dp(12), anchor.y, anchor.x + dp(12), anchor.y, stroke);
        c.drawLine(anchor.x, anchor.y - dp(12), anchor.x, anchor.y + dp(12), stroke);

        if (state.hasMarker()) {
            float[] mm = state.markerMap();
            PointF marker = toMap(mm[0], mm[1]);
            p.setColor(WARM);
            c.drawCircle(marker.x, marker.y, dp(8), p);
            stroke.setColor(Color.argb(135, 212, 118, 70));
            stroke.setStrokeWidth(dp(2));
            c.drawCircle(marker.x, marker.y, dp(14), stroke);
        }

        p.setColor(Color.WHITE);
        c.drawCircle(now.x, now.y, dp(12), p);
        p.setColor(ACCENT);
        c.drawCircle(now.x, now.y, dp(8), p);
        stroke.setColor(Color.argb(80, 36, 90, 80));
        stroke.setStrokeWidth(dp(2));
        c.drawCircle(now.x, now.y, dp(19 + 5 * state.movementStrength()), stroke);

        c.restore();

        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        p.setTextSize(dp(11));
        p.setColor(INK);
        c.drawText(state.regionName(), mapRect.left + dp(16), mapRect.top + dp(25), p);

        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        p.setTextSize(dp(9.5f));
        p.setColor(MUTED);
        c.drawText("2D projection of your sensor-state • not a GPS map",
                mapRect.left + dp(16), mapRect.bottom - dp(14), p);
    }

    private void drawStateInfo(Canvas c, float left, float right, float top) {
        p.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        p.setTextSize(dp(17));
        p.setColor(INK);
        c.drawText(state.stateCode(), left, top + dp(18), p);

        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        p.setTextSize(dp(10.5f));
        p.setColor(MUTED);
        String sensorText = state.activeSensorFamilies() + "/6 sensor families active";
        c.drawText(sensorText, left, top + dp(38), p);

        String markerText;
        float d = state.markerDistance();
        if (Float.isNaN(d)) {
            markerText = "MARK a state, then try to return to it.";
        } else if (d < 0.18f) {
            markerText = "RESONANCE  •  state-distance " + String.format(Locale.US, "%.2f", d);
        } else {
            markerText = "Marked-state distance  " + String.format(Locale.US, "%.2f", d);
        }
        p.setTypeface(Typeface.create("sans", d < 0.18f && !Float.isNaN(d) ? Typeface.BOLD : Typeface.NORMAL));
        p.setColor(d < 0.18f && !Float.isNaN(d) ? WARM : MUTED);
        c.drawText(markerText, left, top + dp(57), p);
    }

    private void drawSensors(Canvas c, float left, float right, float top) {
        String[] names = {"LIGHT", "MAGNETIC", "PRESSURE", "MOTION", "GYRO", "HEADING"};
        float gap = dp(8);
        float cardW = (right - left - gap * 2f) / 3f;
        float cardH = dp(52);
        for (int i = 0; i < 6; i++) {
            int row = i / 3;
            int col = i % 3;
            float x = left + col * (cardW + gap);
            float y = top + row * (cardH + gap);
            RectF r = new RectF(x, y, x + cardW, y + cardH);
            p.setColor(CARD);
            c.drawRoundRect(r, dp(13), dp(13), p);

            p.setTypeface(Typeface.create("sans", Typeface.BOLD));
            p.setTextSize(dp(8.5f));
            p.setColor(state.isActive(i) ? ACCENT : Color.rgb(160, 160, 155));
            c.drawText(names[i], x + dp(10), y + dp(17), p);

            p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            p.setTextSize(dp(11));
            p.setColor(state.isActive(i) ? INK : Color.rgb(160, 160, 155));
            c.drawText(state.valueLabel(i), x + dp(10), y + dp(37), p);
        }
    }

    private void drawButtons(Canvas c, float left, float right, float top) {
        float gap = dp(8);
        float w = (right - left - gap * 2f) / 3f;
        float h = dp(48);
        anchorButton.set(left, top, left + w, top + h);
        markButton.set(left + w + gap, top, left + 2f * w + gap, top + h);
        clearButton.set(left + 2f * (w + gap), top, right, top + h);

        drawButton(c, anchorButton, "ANCHOR", ACCENT, Color.WHITE);
        drawButton(c, markButton, "MARK", WARM, Color.WHITE);
        drawButton(c, clearButton, "CLEAR", Color.rgb(231, 230, 225), INK);
    }

    private void drawButton(Canvas c, RectF r, String text, int bg, int fg) {
        p.setColor(bg);
        c.drawRoundRect(r, dp(15), dp(15), p);
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        p.setTextSize(dp(10.5f));
        p.setColor(fg);
        float tw = p.measureText(text);
        c.drawText(text, r.centerX() - tw / 2f, r.centerY() + dp(4), p);
    }

    private PointF toMap(float nx, float ny) {
        float pad = dp(27);
        float cx = mapRect.centerX();
        float cy = mapRect.centerY();
        float rx = mapRect.width() / 2f - pad;
        float ry = mapRect.height() / 2f - pad;
        return new PointF(cx + nx * rx, cy + ny * ry);
    }

    private void recordTrail(PointF now) {
        long t = SystemClock.elapsedRealtime();
        if (t - lastTrailAt < 120L) return;
        if (trail.isEmpty()) {
            trail.add(new PointF(now.x, now.y));
            lastTrailAt = t;
            return;
        }
        PointF prev = trail.get(trail.size() - 1);
        float dx = now.x - prev.x;
        float dy = now.y - prev.y;
        if (dx * dx + dy * dy > dp(2.2f) * dp(2.2f)) {
            trail.add(new PointF(now.x, now.y));
            if (trail.size() > 180) trail.remove(0);
            lastTrailAt = t;
        }
    }

    void clearTrail() {
        trail.clear();
        lastTrailAt = 0L;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) return true;
        float x = e.getX();
        float y = e.getY();
        if (anchorButton.contains(x, y)) {
            state.setAnchor();
            clearTrail();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            invalidate();
            return true;
        }
        if (markButton.contains(x, y)) {
            state.setMarker();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            invalidate();
            return true;
        }
        if (clearButton.contains(x, y)) {
            clearTrail();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            invalidate();
            return true;
        }
        return true;
    }
}
