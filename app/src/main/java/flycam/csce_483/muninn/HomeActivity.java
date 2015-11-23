package flycam.csce_483.muninn;

import android.Manifest;
import android.app.Activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class HomeActivity extends Activity {

    final CharSequence[] modes = {"Hover", "Loop", "Follow-Me"};

    private boolean flightStatus = false; // 1 for in flight, 0 otherwise
    private boolean beaconStatus = false; // 1 for connected, 0 otherwise
    protected String currentView;

    private int hover_dist;
    private int loop_radius;
    private int follow_dist;

    SharedPreferences sharedPreferences;
    private LocationManager locationManager;
    MyLocationListener myLocationListener;

    private int selectedMode;

    protected Button launchLandButton;

    private DrawerLayout mDrawerLayout;

    // Varying TextViews
    private TextView launchLandText;
    private TextView beacon_connection_text;

    // Input Number Fields
    private EditText hover_dist_input;
    private EditText loop_rad_input;
    private EditText follow_dist_input;

    // Fields for drone settings

    // Fields for Bluetooth
    private int REQUEST_ENABLE_BT = 1;
    private int REQUEST_DISCOVERABLE_BT = 2;
    private ArrayList<BluetoothDevice> foundDevices;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> btArrayAdapter;
    private BluetoothDevice beacon = null;
    private BluetoothSocket btSocket;

    final byte delimiter = 33;
    int readBufferPosition = 0;


    @Override
    public void setContentView(int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, null);
        //tag is needed for pressing back button to go back to splash screen
        currentView = (String) view.getTag();
        super.setContentView(view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();

        setContentView(R.layout.activity_home);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                invalidateOptionsMenu();
            }
        });


        // Setting TextViews
        launchLandText = (TextView) findViewById(R.id.launch_land_status);
        beacon_connection_text = (TextView) findViewById(R.id.beacon_status);

        // Setting Buttons
        launchLandButton = (Button) findViewById(R.id.button_launch_land);

        // Default values for auto flight distances
        hover_dist = 10;
        loop_radius = 25;
        follow_dist = 20;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myLocationListener = new MyLocationListener();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
        // Prompts user to connect to Muninn wifi and bluetooth manually
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Make sure that you have connected to Muninn's wi-fi and bluetooth! And also turn on your location services!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ;// purposely left open
            }
        });
        builder.create().show();

        // Creates handler to call the refreshSettings method every 10 seconds
        final Handler settingsHandler = new Handler();
        settingsHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshSettings();
            }
        }, 10000);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Handler handler = new Handler();

        final class workerThread implements Runnable {

            private String btMsg;
            public workerThread(String msg) {
                btMsg = msg;
            }
            public void run() {
                sendBTMessage(btMsg);
                while(!Thread.currentThread().isInterrupted()) {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {

                        final InputStream mmInputStream;
                        mmInputStream = btSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.d("bt receive","bytes available");
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for(int i=0;i<bytesAvailable;i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    //The variable data now contains our full command
                                    handler.post(new Runnable() {
                                        public void run() {
                                            //setTexts based upon data
                                            parseMessage(data);
                                        }
                                    });
                                    workDone = true;
                                    break;
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                            if (workDone == true){
                                btSocket.close();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Goes through the action of attempting to launch or land the device, sent via bluetooth
    public void launchLand(View view) {
        if(null == beacon){
            connectBeacon(view);
        }
        else {
            if (flightStatus) { // If in flight
                //send signals to land the drone
                //sendBTMessage("0launch_land:land");

                launchLandText.setText(R.string.landed);
                launchLandButton.setText(R.string.launch);
                flightStatus=!flightStatus;
            }
            else { // on the ground
                //send signals to launch the drone
                //sendBTMessage("0launch_land:launch");

                launchLandButton.setText(R.string.land);
                launchLandText.setText(R.string.in_flight);
                flightStatus=!flightStatus;
            }
        }
    }


    // Switches view to the application settings menu
    public void appSettings(View view){
        setContentView(R.layout.app_settings_menu);

        // Setting EditText Fields
        hover_dist_input = (EditText) findViewById(R.id.hover_dist_input);
        loop_rad_input = (EditText) findViewById(R.id.loop_rad_input);
        follow_dist_input = (EditText) findViewById(R.id.follow_dist_input);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        hover_dist = sharedPreferences.getInt("hover_dist", 10);
        loop_radius = sharedPreferences.getInt("loop_radius",25);
        follow_dist = sharedPreferences.getInt("follow_dist",20);

        // Sets the input as what the user has saved already
        hover_dist_input.setText(Integer.toString(hover_dist));
        loop_rad_input.setText(Integer.toString(loop_radius));
        follow_dist_input.setText(Integer.toString(follow_dist));

    }

    public void saveSettings(View view) {
        hover_dist = Integer.parseInt(hover_dist_input.getText().toString());
        loop_radius = Integer.parseInt(loop_rad_input.getText().toString());
        follow_dist = Integer.parseInt(follow_dist_input.getText().toString());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("hover_dist", hover_dist);
        editor.putInt("loop_radius", loop_radius);
        editor.putInt("follow_dist", follow_dist);
        editor.commit();

        // Get switch information

        Toast.makeText(getApplicationContext(), "Settings have been saved!", Toast.LENGTH_SHORT).show();
        //sendBTMessage(generateSettingsMessage());

        setContentView(R.layout.activity_home);
    }


    private String generateSettingsMessage() {
        String message = "1";

        message += "hover_distance:" + hover_dist + ";";
        message += "loop_radius:" + loop_radius + ";";
        message += "follow_distance:" + follow_dist;

        return message;
    }

    // Updates the current mode selection
    public void selectMode(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_mode)
                .setSingleChoiceItems(R.array.mode_select_array, selectedMode, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Muninn is now set to " + modes[which] + " mode!", Toast.LENGTH_SHORT).show();
                        selectedMode = which;
                        String message = "";
                        switch (selectedMode){
                            case 0 :
                                message+="hover";
                                break;
                            case 1 :
                                message+="loop";
                                break;
                            case 2 :
                                message+="follow";
                                break;
                            default:
                                message+="ERR";
                        }
                        //sendBTMessage("2flight_mode:"+message);
                    }
                });
        builder.create().show();
    }

    public void goToCamera(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    private void setupBT() {
        if(mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "This device does not support bluetooth. Please use this companion app with an appropriate device.", Toast.LENGTH_LONG).show();
        }
        else {
            requestBTOn();
        }
    }

    private void requestBTOn() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {
            showBTDevices();
        }
    }

    private void showBTDevices() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Choose Paired Bluetooth Device");

        // get paired devices
        foundDevices = new ArrayList<>(mBluetoothAdapter.getBondedDevices());
        btArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        // put it's one to the adapter
        for(BluetoothDevice device : foundDevices)
            btArrayAdapter.add(device.getName() + "\n" + device.getAddress());

        builder.setSingleChoiceItems(btArrayAdapter, foundDevices.indexOf(beacon), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "Paired with " + foundDevices.get(which), Toast.LENGTH_SHORT).show();
                beacon = foundDevices.get(which);
            }
        });

        builder.create();
        builder.show();
    }

    private void sendBTMessage(String message) {
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
        try {
            btSocket = beacon.createInsecureRfcommSocketToServiceRecord(uuid);

            if(!btSocket.isConnected()){
                btSocket.connect();
            }

            String mes = message;
            OutputStream out = btSocket.getOutputStream();
            out.write(mes.getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                showBTDevices();
            }
            else{
                Toast.makeText(this, "ooops. Something went wrong. Try Again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Goes through the process of connecting the phone to the beacon
    public void connectBeacon(View view) {

        if(null == beacon) {
            setupBT();
        }
        else {
            Toast.makeText(getApplicationContext(), "Beacon is already connected!", Toast.LENGTH_SHORT).show();
        }

    }

    // Will be used to refresh the connections, update drone status, etc.
    // Will connect to the beacon to retrieve the latest information
    private void refreshSettings() {
        Log.d("main", "Settings Refreshed");
        String gpsMessage = "";
        //send GPS information
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            gpsMessage+="3long:"+myLocationListener.longitude+";lat:"+myLocationListener.latitude;
            sendBTMessage(gpsMessage);
        }
        else{
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
        }

    }

    private void parseMessage(String message) {

    }

    @Override
    public void onBackPressed() {
        //Checks if the current screen is not the activity_main.xml layout
        if (!"main".equals(currentView)) {
            setContentView(R.layout.activity_home);
        }
        else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}