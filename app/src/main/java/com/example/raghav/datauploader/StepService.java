package com.example.raghav.datauploader;

/**
 * Created by raghav on 29/8/15.
 */

import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

public class StepService extends Service implements SensorEventListener, LocationListener {
    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();

    protected LocationManager locationManager;
    protected LocationListener locationListener;

    public SensorManager mSensorManager = null;

    int step_avg_filter_size = 40;
    float step_z_acc_previous = 0, step_z_acc_present = 0, step_x_acc_previous = 0, step_x_acc_present = 0, step_y_acc_previous = 0, step_y_acc_present = 0;

    //----------------------to store sensor values-----------------------------
    float[] linear = new float[4];
    float[] linear2 = new float[4];
    float[] accelero = new float[4];
    float[] gyro = new float[3];
    float[] magneto = new float[4];
    float[] magneto2 = new float[4];
    float[] gravity = new float[3];
    float[] gravity2 = new float[4];
    float[] rotation = new float[6];
    private float[] realAccValues = new float[4];
    public float[] rotationMatrix = new float[16];
    public float[] invertedRotationMatrix = new float[16];
    public float[] inclinationMatrix = new float[16];
    float ang = 0;

    int lagger = 0;
    //------------------------step detector-----------------------
    float step_detector_count = 0;
    float[] step_values_before_step_detected = {0, 0, 0, 0, 0};
    float[] step_values_after_step_detected = {0, 0, 0, 0, 0};

    //----------------for step_lengths---------------
    float step_l1 = 0;
    float step_length = 0;

    private float angle = 0f;
    private boolean prevStepDetected = false;
    private boolean currentStepDetected = false;
    private int step_count = 0;
    private int bufferStepCount = 0;
    private int maxDataCount = 100;
    private String[] dataBuffer = new String[maxDataCount];
    private int counter = 1;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "in onCreate");


        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        initListeners();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "in onDestroy");
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
                gravity = event.values;
                gravity2[0] = event.values[0];
                gravity2[1] = event.values[1];
                gravity2[2] = event.values[2];
                gravity2[3] = 0;
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyro = event.values;
                break;
            case Sensor.TYPE_ORIENTATION:
                ang = event.values[0];
                //   my_layout.setRotation(-ang);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneto = event.values;
                magneto2[0] = event.values[0];
                magneto2[1] = event.values[1];
                magneto2[2] = event.values[2];
                magneto2[3] = 0;

                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                rotation[0] = event.values[0];
                rotation[1] = event.values[1];
                rotation[2] = event.values[2];
                rotation[3] = event.values[3];
                break;

            case Sensor.TYPE_ACCELEROMETER:
                accelero = event.values;
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:
                linear2[0] = event.values[0];
                linear2[1] = event.values[1];
                linear2[2] = event.values[2];
                linear2[3] = 0;
                linear = event.values;
        }
        localization();                 // localization_using_inertial _sensor_data
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not implemented. Goes unused.
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("", "Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
        Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        gpsOptionsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(gpsOptionsIntent);

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }



    private void localization()
    {
        convert_coordinate();// converting phone coordinate system to real world system

        setAllData(step_count + 1, step_l1, /*step_detection_info.step_ang*/ 0, realAccValues[0], realAccValues[1], realAccValues[2], linear2[0], linear2[1], linear2[2],
         /*step_detection.step_min1*/ 0, /*step_detection.step_min2*/ 0, /*step_detection.step_max1*/ 0);
    }


    private void convert_coordinate() {
        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, magneto);
        android.opengl.Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0);
        android.opengl.Matrix.multiplyMV(realAccValues, 0, invertedRotationMatrix, 0, linear2, 0);

    }

    public void initListeners(){

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);


        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);


    }


    /*public void postData(String[] dataToSend) {

        String ipaddr = "10.182.0.111";
        String port = "8080";
        String res = "";

        HashMap<String, String> data = new HashMap<String, String>();
        for(int i = 0;i < dataToSend.length; i++){
            data.put("stepdata" + i, dataToSend[i]);
        }
        AsyncHttpPost asyncHttpPost = new AsyncHttpPost(data);
        asyncHttpPost.execute("http://"+ipaddr + ":" +port + "/data");
    }*/

    public String getTimestamp() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss.SSS");
        String strDate = sdf.format(c.getTime());
        return strDate;
    }

    private void setAllData (int steps, float d, float ang, float xAcc, float yAcc,   float zAcc, float phoneXAcc, float phoneYAcc, float phoneZacc, float min1, float max1, float min2) {

        String value1 = "step="+String.valueOf(steps)+"&";
        value1 += "stepLength=" + String.valueOf(step_length)+"&";
        value1 += "stepAngle=" + String.valueOf(ang)+"&";
        value1 += "xAcc="+ String.valueOf(xAcc)+"&";
        value1 += "yAcc="+ String.valueOf(yAcc)+"&";
        value1 += "zAcc="+ String.valueOf(zAcc)+"&";
        value1 += "phoneXAcc="+ String.valueOf(phoneXAcc)+"&";
        value1 += "phoneYAcc="+ String.valueOf(phoneYAcc)+"&";
        value1 += "phoneZAcc="+ String.valueOf(phoneZacc)+"&";
        value1 += "min1="+ String.valueOf(min1)+"&";
        value1 += "min2="+ String.valueOf(min2)+"&";
        value1 += "max1="+ String.valueOf(max1)+"&";
        value1 += "time="+ getTimestamp();

        if(counter <= maxDataCount ){
            dataBuffer[counter-1] = value1;
            counter++;
        }
        else{
//            postData(dataBuffer);
            counter = 1;

        }
    }


    public class MyBinder extends Binder {
        StepService getService() {
            return StepService.this;
        }
    }
}
