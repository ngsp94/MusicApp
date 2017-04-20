package com.example.link.opencvtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Play extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG="MainActivity";

    private static Vector<InstrumentData> instruments;

    private SensorManager mSensorManager;
    private SensorEventListener mSensorListener;
    private PlayWave play = new PlayWave();

    JavaCameraView javaCameraView;
    Mat mRgba, imgGrey, imgCanny;
    int[] heights = new int[5];
    int baseFreq = 440;
    int sensitivity = 5;
    int limit = 3;
    float dist = 0;
    int num_data = 0;
    String currInst = "Piano";
    int prev_dist = 0;

    private static final float[] notes = {
            55.00f,
            61.74f,
            65.41f,
            73.42f,
            82.41f,
            87.31f,
            98.00f,
            110.00f,
            123.47f,
            130.81f,
            146.83f,
            164.81f,
            174.61f,
            196.00f,
            220.00f,
            246.94f,
            261.63f, // Middle C
            293.66f,
            329.63f,
            349.23f,
            392.00f,
            440.00f,
            493.88f,
            523.25f,
            587.33f,
            659.26f,
            698.46f,
            783.99f,
            880.00f,
            987.77f,
            1046.5f,
    };

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;

                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        instruments = new Vector<InstrumentData>();
        num_data = getIntent().getExtras().getInt("NUM_DATA");
        for(int i = 0; i < num_data; i++) {
            String type = (String) getIntent().getExtras().get("TYPE_" + i);
            int sensitivity = getIntent().getExtras().getInt("SENSITIVITY_" + i);
            float[] orientation = getIntent().getExtras().getFloatArray("ORIENTATION_" + i);
            InstrumentData newData = new InstrumentData(type, sensitivity, orientation);
            instruments.add(newData);
        }

        javaCameraView = (JavaCameraView)findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

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
                    float x = values[0];
                    float y = values[1];
                    float z = values[2];

                    // set instrument & sensitivity

                    if (z < -5) {
                        play.stop();
                        prev_dist = 0;
                    }
                    else {
                        for (int i = 0; i < num_data; i++) {
                            float[] orient = instruments.get(i).orientation;
                            if (Math.abs(orient[0] - x) < limit &&
                                    Math.abs(orient[1] - y) < limit &&
                                    Math.abs(orient[2] - z) < limit) {
                                currInst = instruments.get(i).instrumentType;
                                sensitivity = instruments.get(i).sensitivity;
                            }
                        }
                    }

                }
                System.gc();
            }
        };

        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        play.setWave(baseFreq, currInst);

    }


    @Override
    protected void onPause() {
        super.onPause();
        play.stop();
        prev_dist = 0;

        if (javaCameraView!=null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        play.stop();
        prev_dist = 0;
        if (javaCameraView!=null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "Successfully loaded");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.d(TAG, "Not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        imgGrey = new Mat(height, width, CvType.CV_8UC1);
        imgCanny = new Mat(height, width, CvType.CV_8UC1);

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        imgCanny.release();
        imgCanny.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        // greyscale
        Imgproc.cvtColor(mRgba, imgGrey, Imgproc.COLOR_RGBA2GRAY);
        List<Mat> channels = new ArrayList<Mat>();
        Core.split(mRgba, channels);

        // blurimage
        Imgproc.GaussianBlur(imgGrey, imgGrey, new Size(5, 5), 0);
        // detect edges
        Imgproc.Canny(imgGrey, imgCanny, 85, 255);

        // find contours
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imgCanny, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // draw contours
        Rect rect = null;
        double maxArea = 0;
        for (int i=0; i<contours.size(); i++) {
            if (Imgproc.contourArea(contours.get(i)) > maxArea) {
                rect = Imgproc.boundingRect(contours.get(i));
                maxArea = Imgproc.contourArea(contours.get(i));
            }

        }

        if (rect != null) {
            final int height = rect.height;
            Core.rectangle(imgCanny, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 0, 0), 3);

            // set height to average of past 4 + current
            int sum = 0;
            for (int i=1; i<heights.length; i++) {
                sum = sum + i;
                heights[i] = heights[i-1];

            }

            heights[0] = height;
            final int aveHeight = (sum + height) / 5;

            runOnUiThread( new Runnable() {

                @Override
                public void run() {

                    TextView t = (TextView)findViewById(R.id.Instrument);

                    if (aveHeight < 10) {
                        play.stop();
                        prev_dist = 0;
                    }

                    dist = 100.0f*sensitivity/aveHeight;

                    t.setText(currInst);

                    Log.d(TAG, "Dist " + dist);

                    if ((int)dist < notes.length && (int)dist != prev_dist) {
                        play.stop();
                        play.setWave((int) (notes[(int) dist]), currInst);
                        play.start();

                        prev_dist = (int) dist;

                    }


                }

            });

        } else {
            play.stop();
            prev_dist = 0;
        }

        System.gc();
        return imgCanny;

    }
}