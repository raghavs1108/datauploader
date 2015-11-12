package com.example.raghav.datauploader;
import android.app.ProgressDialog;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.example.raghav.datauploader.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

class AsyncHttpPost extends AsyncTask<String, Void, Void> {

    NetworkInfo net;
    File file = null;

    MainActivity uActivity;

    HttpURLConnection connection = null;
    DataOutputStream outputStream = null;
    DataInputStream inputStream = null;

    String folderPath;
    String arrayOfFiles[];
    File root;
    File allFiles;

    String urlServer = "https://pothole-detector-server.herokuapp.com/"; //"http://192.168.0.104:8080/";
    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary =  "*****";

    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 100*1024*1024; // 100 MB

    URL url;
    String filename = null;

    public AsyncHttpPost(String filename) {
        this.filename = filename;
        this.file = new File(filename);
    }


    @Override
    protected void onPreExecute() {


        Log.d(" UploadGpsData", "onPreRequest");


    }

    @Override
    protected Void doInBackground(String... params) {

        Log.d(" UploadGpsData","doInBackground");

            Log.d("File Name", filename);
            //File filename = new File(arrayOfFiles[i].toString());
            try {
                url = new URL(urlServer);
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                connection = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Enable POST method
            try {
                connection.setRequestMethod("POST");
            } catch (ProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
            try{

                FileInputStream fileInputStream = new FileInputStream(filename);
                try {
                    outputStream = new DataOutputStream( connection.getOutputStream() );
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + filename +"\"" + lineEnd);
                outputStream.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0){
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                //int serverResponseCode = connection.getResponseCode();
                //String serverResponseMessage = connection.getResponseMessage();

                // Responses from the server (code and message)
                //serverResponseCode = connection.getResponseCode();
                //serverResponseMessage = connection.getResponseMessage();

                fileInputStream.close();
                outputStream.flush();
                outputStream.close();
                Log.d("File Name", "closed the output stream.");
            } catch(Exception e){
                e.printStackTrace();
            }


        return null;
    }

    protected void onPostExecute(Void result) {

        Log.d(" UploadGpsData","onPost");
        Log.d(" UploadGpsData", "upload acheived.");

    }
}