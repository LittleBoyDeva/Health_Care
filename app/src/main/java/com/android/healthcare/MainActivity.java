package com.android.healthcare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {



    LocationManager lm;
    public WifiManager mWifiManager;
    ServerSocket serverSocket;

    String[] PERMISSIONS = new String[]{  //creating an array of permissions.
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    Button button;

    String URL = "https://api.thingspeak.com/update?api_key=0DFD2MDEODZ10ZN3&field1=";





    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);


        lm = (LocationManager) this.getApplicationContext().getSystemService(LOCATION_SERVICE);
        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(WIFI_SERVICE);

        if (hasPermissions(this, PERMISSIONS)){ //checking permission is granted or not.
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, 1);
        }

        button = findViewById(R.id.button);
        button.setOnClickListener(view -> scan());
        


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    try {
                        URL url = new URL(URL+7);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        try {
                            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                            Log.e("TAG", String.valueOf(in.read()));
                        } finally {
                            urlConnection.disconnect();
                        }
                    }catch (Exception e){
                        Log.e("TAG", e +" LINE COMMAND");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();



        Log.e("TAG","-----------------start---------------------------------");

        new  WiFiSocketTask("localhost",9600).execute();
        

        Log.e("TAG","--------------------end------------------------------");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    public static boolean hasPermissions(Context context, String... permissions) { //request permission.
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void scan() {

        if (locationGPS()) {
            alertDialogForGPSP2P();

        } else {
            if (mWifiManager.isWifiEnabled()) {
                //DO NEEDS
                Log.e("TAG","CLICKED");
            } else {
                alertDialogForWifiP2P();
            }
        }

    }


    private void alertDialogForGPSP2P() {
        // notify user
        new AlertDialog.Builder(this)
                .setTitle("GPS not enabled")
                .setMessage(R.string.gps_request_for_p2p)
                .setPositiveButton("allow", (paramDialogInterface, paramInt) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("deny", null)
                .show();

    }

    private boolean locationGPS() {

        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e("TAG","error while checking GPS");
        }

        return !gps_enabled;
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void alertDialogForWifiP2P() {
        // notify user
        new AlertDialog.Builder(this)
                .setTitle("Wifi not enabled")
                .setMessage(R.string.wifi_request_for_p2p)
                .setPositiveButton("allow", (paramDialogInterface, paramInt) -> {
                    try {
                        Intent panelIntent = new Intent(android.provider.Settings.Panel.ACTION_WIFI);
                        startActivity(panelIntent);

                    } catch (Exception e) {
                        Log.e("TAG",e.toString());

                        try {
                            Intent panelIntent = new Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
                            startActivity(panelIntent);
                        } catch (Exception ignored) {
                            Log.e("TAG","Turn on WIFI manually");
                        }

                    }
                })
                .setNegativeButton("deny", null)
                .show();

    }



        /**
     * AsyncTask that connects to a remote host over WiFi and reads/writes the connection
     * using a socket. The read loop of the AsyncTask happens in a separate thread, so the
     * main UI thread is not blocked. However, the AsyncTask has a way of sending data back
     * to the UI thread. Under the hood, it is using Threads and Handlers.
     */
    @SuppressLint("StaticFieldLeak")
    public class WiFiSocketTask extends AsyncTask<Void, String, Void> {

        // Location of the remote host
        String address;
        int port;

        // Special messages denoting connection status
        private static final String PING_MSG = "SOCKET_PING";
        private static final String CONNECTED_MSG = "SOCKET_CONNECTED";
        private static final String DISCONNECTED_MSG = "SOCKET_DISCONNECTED";

        Socket socket = null;
        BufferedReader inStream = null;
        OutputStream outStream = null;


        // Constructor
        WiFiSocketTask(String address, int port) {
            this.address = address;
            this.port = port;
        }

        /**
         * Main method of AsyncTask, opens a socket and continuously reads from it
         */
        @Override
        protected Void doInBackground(Void... arg) {

            Log.e("TAG","------------------before execution --------------------------------");

            try {

                serverSocket = new ServerSocket(9600);

                Log.e("TAG","waiting to client");

                runOnUiThread(() -> Toast.makeText(getApplicationContext(),"waiting to client connect",Toast.LENGTH_LONG).show());

                socket = serverSocket.accept();

                runOnUiThread(() -> Toast.makeText(getApplicationContext(),"CONNECTED",Toast.LENGTH_LONG).show());

                if (socket.isConnected()){
                    Log.e("TAG","CONNECTED");
                }else {
                    Log.e("TAG","NOT CONNECTED");
                }

                /*PrintWriter output = new PrintWriter(socket.getOutputStream());
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));*/


            } catch (Exception e) {
                e.printStackTrace();
                Log.e("TAG", "Error in socket thread!");

                runOnUiThread(() -> Toast.makeText(getApplicationContext(),"Error in socket thread!",Toast.LENGTH_LONG).show());

            }

            // Send a disconnect message
            publishProgress(DISCONNECTED_MSG);

            // Once disconnected, try to close the streams
            try {
                if (socket != null) socket.close();
                if (inStream != null) inStream.close();
                if (outStream != null) outStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.e("TAG","------------------after execution --------------------------------");

            return null;
        }

        /**
         * This function runs in the UI thread but receives data from the
         * doInBackground() function running in a separate thread when
         * publishProgress() is called.
         */
        @Override
        protected void onProgressUpdate(String... values) {

            String msg = values[0];
            if(msg == null) return;



            super.onProgressUpdate(values);
        }

    }

}
