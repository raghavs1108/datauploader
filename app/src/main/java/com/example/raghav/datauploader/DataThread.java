package com.example.raghav.datauploader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DataThread extends Thread implements SensorEventListener, LocationListener{

    private final Context mContext;
    private final Activity mActivity;

    protected LocationManager locationManager;

    float[] gravity = new float[3];
    float[] gravity2 = new float[4];

    float[] linear = new float[4];
    float[] linear2 = new float[4];

    float[] accelero = new float[4];
    float[] gyro = new float[3];
    float[] magneto = new float[4];
    float[] magneto2 = new float[4];

    float[] rotation = new float[6];

    private float[] realAccValues = {0f ,0f ,0f ,0f};
    public float[] rotationMatrix = new float[16];
    public float[] invertedRotationMatrix = new float[16];
    public float[] inclinationMatrix = new float[16];

    public double [] location = new double[2];
    private SensorManager mSensorManager;

    private FileWriter writer;
    String imei;
    static String filename;
    private String column_order = "time,latitude,longitude,xAcc,yAcc,zAcc";
    public void run() {

        Log.d("","In run.");
        this.mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getDeviceId();
        filename = "/sdcard/sensordata.csv";
        File file = new File(filename);

        try {
            new RandomAccessFile(filename, "rw").setLength(0);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write(imei+ "\n");
                writer.write(column_order+"\n");
                writer.close();
            Log.d("", "New File.");
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }

//        File file = new File(filename);
//        long len = file.length();
//        if (len == 0){
//            try {
//                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//                writer.write(imei);
//                writer.close();
//            }
//            catch (IOException e) {
//                Log.e("Exception", "File write failed: " + e.toString());
//            }
//        }
//        long maxlen = 1024*1024*20; // 20 mb
//        if(len >= maxlen){
//            try {
//                new RandomAccessFile(filename, "rw").setLength(0);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        try {
            writer = new FileWriter(filename, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Looper.prepare();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this); // This error isn't worth a second look.
            initListeners();
            Looper.loop();
        } catch (Exception e) {
            //...
        }
    }

    public void kill() throws Exception {
        mSensorManager.unregisterListener(this);
        throw new Exception();
    }
    public void initListeners(){
        Log.d("", "initListeners");

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

        Log.d("", "Listeners Inintialised.");
    }
    @Override
    public void onLocationChanged(Location location) {
        this.location[0] = location.getLatitude();
        this.location[1] = location.getLongitude();
        TextView t = (TextView) mActivity.findViewById(R.id.textbox1);
        t.setText(this.location[0] + " "+ this.location[1]);
        Log.d("", "onLocationChanged");
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("","onStatusChanged");
    }
    @Override
    public void onProviderEnabled(String provider) {
        Log.d("", "onProviderEnabled");

    }
    @Override
    public void onProviderDisabled(String provider) {
        Log.d("", "onProviderDisabled");
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        mContext.startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d("","onSensorChanged");
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

        convert_coordinate();
        String time = getTimestamp();
        TextView text = (TextView) mActivity.findViewById(R.id.textbox1);
        text.setText(imei +"\n" + realAccValues[0] + "\n" + realAccValues[1] + "\n" + realAccValues[2] + "\n" + time + "\n" + location[0] + "\n" + location[1]);
        try {
            saveToFile(time, location[0], location[1], realAccValues[0], realAccValues[1], realAccValues[2]);
        }
        catch(IOException e){
            Log.d("","File Write failed.");
        }
    }
    private void convert_coordinate() {
//        Log.d("","convert Coordinate");
        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, magneto);
        android.opengl.Matrix.invertM(invertedRotationMatrix, 0, rotationMatrix, 0);
        android.opengl.Matrix.multiplyMV(realAccValues, 0, invertedRotationMatrix, 0, linear2, 0);

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public DataThread(Context mContext, Activity activity){
        this.mContext = mContext;
        this.mActivity = activity;
    }
    public String getTimestamp() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd:MM:yyyy HH:mm:ss.SSS");
        String strDate = sdf.format(c.getTime());
        return strDate;
    }

    private void saveToFile(String time, double latitude, double longitiude, double xAcc, double yAcc, double zAcc) throws IOException {
        Log.d("","SaveToFile.");
        writer.write(time+","+latitude+","+longitiude+","+xAcc+","+yAcc+","+zAcc+"\n");
    }
}