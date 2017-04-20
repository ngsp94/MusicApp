package com.example.link.opencvtest;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Vector;

public class Settings extends AppCompatActivity {

    private Spinner instrumentFilter;
    private SeekBar sensitivityBar;
    private int id;
    private float x;
    private float y;
    private float z;

    private float finalX;
    private float finalY;
    private float finalZ;

    private int limit = 3; // buffer for orientation

    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        id = getIntent().getExtras().getInt("ID");
        instrumentFilter = (Spinner) findViewById(R.id.instrument_filter);
        sensitivityBar = (SeekBar) findViewById(R.id.sensitivity_bar);

        if(getIntent().getExtras().getString("STATUS").equals("UPDATE")) {
            String[] instrumentList = getResources().getStringArray(R.array.instrument_list);
            String instrumentChoice = getIntent().getExtras().getString("INSTRUMENT_CHOICE");
            int sensitivity = getIntent().getExtras().getInt("SENSITIVITY_VALUE");
            finalX = getIntent().getExtras().getFloat("ORIENTATION_X");
            finalY = getIntent().getExtras().getFloat("ORIENTATION_Y");
            finalZ = getIntent().getExtras().getFloat("ORIENTATION_Z");

            TextView t = (TextView)findViewById(R.id.orientation_description);
            String text = "x: " + Float.toString(finalX) + "\ny: " + Float.toString(finalY) + "\nz: " + Float.toString(finalZ) + "\n";
            t.setText(text);
            instrumentFilter.setSelection(Arrays.asList(instrumentList).indexOf(instrumentChoice));
            sensitivityBar.setProgress(sensitivity);
        }

        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float[] values = event.values;
                    x = values[0];
                    y = values[1];
                    z = values[2];

                }
            }
        };

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void setOrientation(View v) {

        int num_data = getIntent().getExtras().getInt("NUM_DATA");
        boolean isClash = false;
        for(int i = 0; i < num_data; i++) {
            float[] orien = getIntent().getExtras().getFloatArray("ORIENTATION_" + i);
            if ((Math.abs(orien[0] - x) < limit &&
                    Math.abs(orien[1] - y) < limit &&
                    Math.abs(orien[2] - z) < limit)) {
                isClash = true;
                break;
            }
        }

        TextView t = (TextView)findViewById(R.id.orientation_description);

        if (isClash) {
            t.setText("Orientation clashes with existing orientation. Please choose something else!");
        } else {

            finalX = x;
            finalY = y;
            finalZ = z;
            String text = "x: " + Float.toString(finalX) + "\ny: " + Float.toString(finalY) + "\nz: " + Float.toString(finalZ) + "\n";
            t.setText(text);
        }
    }

    public void onCancelButton(View v) {
        finish();
    }

    public void onSaveButton(View v) {
        String instrument = instrumentFilter.getSelectedItem().toString();
        int sensitivity = sensitivityBar.getProgress();

        if(finalX == 0 && finalY == 0 & finalZ == 0) {
            TextView t = (TextView)findViewById(R.id.orientation_description);
            String text = "Please set the orientation";
            t.setText(text);
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("ID", id);
        resultIntent.putExtra("INSTRUMENT_CHOICE", instrument);
        resultIntent.putExtra("SENSITIVITY_VALUE", sensitivity);
        resultIntent.putExtra("ORIENTATION_X", finalX);
        resultIntent.putExtra("ORIENTATION_Y", finalY);
        resultIntent.putExtra("ORIENTATION_Z", finalZ);
        setResult(AppCompatActivity.RESULT_OK, resultIntent);
        finish();
    }
}
