package com.android.healthcare;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {



    LocationManager lm;
    public WifiManager mWifiManager;
    ServerSocket serverSocket;
    private PrintWriter output;
    private BufferedReader input;

    Button button;
    // Tag for logging
    private final String TAG = getClass().getSimpleName();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.app_name);


        lm = (LocationManager) this.getApplicationContext().getSystemService(LOCATION_SERVICE);
        mWifiManager = (WifiManager) this.getApplicationContext().getSystemService(WIFI_SERVICE);


        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scan();
            }
        });

        WiFiSocketTask wifi= new  WiFiSocketTask("localhost",9600);

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


    public void scan() {

        if (locationGPS()) {
            alertDialogForGPSP2P();

        } else {
            if (mWifiManager.isWifiEnabled()) {
                //DO NEEDS
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

            try {

                serverSocket = new ServerSocket(9600);

                socket = serverSocket.accept();
                if (socket.isConnected()){
                    Toast.makeText(getApplicationContext(),"CONNECTED",Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getApplicationContext(),"NOT CONNECTED",Toast.LENGTH_LONG).show();
                }
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Error in socket thread!");
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