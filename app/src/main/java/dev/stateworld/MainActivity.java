package dev.stateworld;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private final StateModel state = new StateModel();
    private StateWorldView view;
    private final List<Sensor> sensors = new ArrayList<>();
    private final float[] rotationMatrix = new float[9];
    private final float[] orientation = new float[3];
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(0xFFF7F6F2);
        window.setNavigationBarColor(0xFFF7F6F2);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        discover(Sensor.TYPE_LIGHT);
        discover(Sensor.TYPE_MAGNETIC_FIELD);
        discover(Sensor.TYPE_PRESSURE);
        discover(Sensor.TYPE_ACCELEROMETER);
        discover(Sensor.TYPE_GYROSCOPE);
        discover(Sensor.TYPE_ROTATION_VECTOR);

        view = new StateWorldView(this, state);
        setContentView(view);

        handler.postDelayed(() -> {
            if (!state.hasAnchor()) {
                state.setAnchor();
                view.clearTrail();
                view.invalidate();
            }
        }, 1600L);
    }

    private void discover(int type) {
        Sensor sensor = sensorManager.getDefaultSensor(type);
        state.setSensorAvailable(type, sensor != null);
        if (sensor != null) sensors.add(sensor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Sensor sensor : sensors) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ROTATION_VECTOR) {
            try {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientation);
                state.updateOrientation(orientation[0], orientation[1], orientation[2]);
            } catch (RuntimeException ignored) {
            }
        } else {
            state.update(type, event.values);
        }
        if (view != null) view.postInvalidateOnAnimation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
