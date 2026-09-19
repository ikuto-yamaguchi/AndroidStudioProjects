package dev.stateworld;

import android.hardware.Sensor;

import java.util.Arrays;
import java.util.Locale;

final class StateModel {
    static final int N = 9;

    private final float[] f = new float[N];
    private final float[] anchor = new float[N];
    private final float[] marker = new float[N];
    private final boolean[] active = new boolean[N];

    private boolean hasAnchor = false;
    private boolean hasMarker = false;

    private float lightLux = 0f;
    private float magneticUt = 0f;
    private float pressureHpa = 0f;
    private float motionMs2 = 0f;
    private float gyroRad = 0f;
    private float yaw = 0f, pitch = 0f, roll = 0f;

    private static final float[] SCALE = {
            0.16f, 0.14f, 0.10f, 0.18f, 0.18f,
            0.65f, 0.65f, 0.42f, 0.48f
    };

    StateModel() {
        Arrays.fill(f, 0f);
        f[6] = 1f;
    }

    synchronized void setSensorAvailable(int sensorType, boolean value) {
        switch (sensorType) {
            case Sensor.TYPE_LIGHT:
                active[0] = value;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                active[1] = value;
                break;
            case Sensor.TYPE_PRESSURE:
                active[2] = value;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                active[3] = value;
                break;
            case Sensor.TYPE_GYROSCOPE:
                active[4] = value;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                active[5] = value;
                active[6] = value;
                active[7] = value;
                active[8] = value;
                break;
        }
    }

    synchronized void update(int sensorType, float[] v) {
        if (v == null || v.length == 0) return;
        switch (sensorType) {
            case Sensor.TYPE_LIGHT: {
                lightLux = ema(lightLux, Math.max(0f, v[0]), 0.18f);
                float norm = (float) (Math.log1p(Math.min(lightLux, 20000f)) / Math.log1p(20000f));
                f[0] = ema(f[0], norm, 0.16f);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD: {
                if (v.length < 3) return;
                float mag = magnitude(v[0], v[1], v[2]);
                magneticUt = ema(magneticUt, mag, 0.12f);
                f[1] = ema(f[1], clamp(magneticUt / 120f, 0f, 1.5f), 0.12f);
                break;
            }
            case Sensor.TYPE_PRESSURE: {
                pressureHpa = ema(pressureHpa == 0f ? v[0] : pressureHpa, v[0], 0.08f);
                f[2] = clamp((pressureHpa - 950f) / 100f, -0.3f, 1.3f);
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                if (v.length < 3) return;
                float a = magnitude(v[0], v[1], v[2]);
                float dynamic = Math.abs(a - 9.80665f);
                motionMs2 = ema(motionMs2, dynamic, 0.16f);
                f[3] = ema(f[3], clamp(motionMs2 / 5f, 0f, 1.4f), 0.18f);
                break;
            }
            case Sensor.TYPE_GYROSCOPE: {
                if (v.length < 3) return;
                float g = magnitude(v[0], v[1], v[2]);
                gyroRad = ema(gyroRad, g, 0.14f);
                f[4] = ema(f[4], clamp(gyroRad / 4.5f, 0f, 1.4f), 0.18f);
                break;
            }
        }
    }

    synchronized void updateOrientation(float yawRad, float pitchRad, float rollRad) {
        yaw = yawRad;
        pitch = pitchRad;
        roll = rollRad;
        f[5] = ema(f[5], (float) Math.sin(yawRad), 0.12f);
        f[6] = ema(f[6], (float) Math.cos(yawRad), 0.12f);
        f[7] = ema(f[7], clamp(pitchRad / ((float) Math.PI / 2f), -1f, 1f), 0.12f);
        f[8] = ema(f[8], clamp(rollRad / (float) Math.PI, -1f, 1f), 0.12f);
    }

    synchronized void setAnchor() {
        System.arraycopy(f, 0, anchor, 0, N);
        hasAnchor = true;
    }

    synchronized boolean hasAnchor() {
        return hasAnchor;
    }

    synchronized void setMarker() {
        System.arraycopy(f, 0, marker, 0, N);
        hasMarker = true;
    }

    synchronized boolean hasMarker() {
        return hasMarker;
    }

    synchronized float[] currentMap() {
        if (!hasAnchor) return new float[]{0f, 0f};
        return project(f);
    }

    synchronized float[] markerMap() {
        if (!hasMarker || !hasAnchor) return new float[]{0f, 0f};
        return project(marker);
    }

    private float[] project(float[] state) {
        float[] d = new float[N];
        for (int i = 0; i < N; i++) {
            if (!active[i]) {
                d[i] = 0f;
            } else {
                d[i] = (state[i] - anchor[i]) / SCALE[i];
            }
        }

        float x = 0.95f * d[0] + 0.85f * d[1] - 0.55f * d[2]
                + 1.05f * d[3] + 0.72f * d[4] + 0.50f * d[5]
                - 0.32f * d[6] + 0.46f * d[7] - 0.28f * d[8];
        float y = -0.58f * d[0] + 0.92f * d[1] + 0.86f * d[2]
                + 0.58f * d[3] - 0.74f * d[4] - 0.26f * d[5]
                + 0.52f * d[6] + 0.80f * d[7] + 0.58f * d[8];

        return new float[]{(float) Math.tanh(x * 0.46f), (float) Math.tanh(y * 0.46f)};
    }

    synchronized float markerDistance() {
        if (!hasMarker) return Float.NaN;
        float sum = 0f;
        int count = 0;
        for (int i = 0; i < N; i++) {
            if (!active[i]) continue;
            float z = (f[i] - marker[i]) / SCALE[i];
            sum += z * z;
            count++;
        }
        return count == 0 ? Float.NaN : (float) Math.sqrt(sum / count);
    }

    synchronized String stateCode() {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < N; i++) {
            if (!active[i]) continue;
            int q = Math.round(f[i] * 127f);
            h ^= (q & 0xff);
            h *= 0x100000001b3L;
            h ^= (i * 31L + 17L);
            h *= 0x100000001b3L;
        }
        int a = (int) ((h >>> 32) & 0xffff);
        int b = (int) ((h >>> 16) & 0xffff);
        int c = (int) (h & 0xffff);
        return String.format(Locale.US, "SW-%04X-%04X-%04X", a, b, c);
    }

    synchronized String regionName() {
        if (!hasAnchor) return "CALIBRATING";
        float dl = active[0] ? (f[0] - anchor[0]) / SCALE[0] : 0f;
        float dm = active[1] ? (f[1] - anchor[1]) / SCALE[1] : 0f;
        float dp = active[2] ? (f[2] - anchor[2]) / SCALE[2] : 0f;
        float motion = active[3] ? (f[3] - anchor[3]) / SCALE[3] : 0f;
        float spin = active[4] ? (f[4] - anchor[4]) / SCALE[4] : 0f;

        float max = 0.48f;
        String name = "ANCHOR WATERS";
        if (-dl > max) { max = -dl; name = "NIGHT BASIN"; }
        if (dl > max) { max = dl; name = "LUMEN FIELDS"; }
        if (Math.abs(dm) > max) { max = Math.abs(dm); name = "MAGNETIC DUNES"; }
        if (Math.abs(dp) > max) { max = Math.abs(dp); name = dp > 0 ? "PRESSURE RIDGE" : "LOW-PRESSURE DEEP"; }
        if (motion > max) { max = motion; name = "KINETIC PLAINS"; }
        if (spin > max) { name = "SPINWARD"; }
        return name;
    }

    synchronized int activeSensorFamilies() {
        int n = 0;
        if (active[0]) n++;
        if (active[1]) n++;
        if (active[2]) n++;
        if (active[3]) n++;
        if (active[4]) n++;
        if (active[5]) n++;
        return n;
    }

    synchronized boolean isActive(int familyIndex) {
        switch (familyIndex) {
            case 0: return active[0];
            case 1: return active[1];
            case 2: return active[2];
            case 3: return active[3];
            case 4: return active[4];
            case 5: return active[5];
            default: return false;
        }
    }

    synchronized String valueLabel(int familyIndex) {
        switch (familyIndex) {
            case 0: return active[0] ? format(lightLux, 0) + " lx" : "—";
            case 1: return active[1] ? format(magneticUt, 1) + " µT" : "—";
            case 2: return active[2] ? format(pressureHpa, 1) + " hPa" : "—";
            case 3: return active[3] ? format(motionMs2, 2) + " m/s²" : "—";
            case 4: return active[4] ? format(gyroRad, 2) + " rad/s" : "—";
            case 5: return active[5] ? format((float) Math.toDegrees(yaw), 0) + "°" : "—";
            default: return "—";
        }
    }

    synchronized float movementStrength() {
        return clamp(f[3] + f[4] * 0.7f, 0f, 1f);
    }

    private static float magnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private static float ema(float old, float value, float alpha) {
        return old + alpha * (value - old);
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String format(float v, int decimals) {
        return String.format(Locale.US, "% ." + decimals + "f", v).trim();
    }
}
