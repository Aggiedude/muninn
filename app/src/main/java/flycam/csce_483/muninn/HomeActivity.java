package flycam.csce_483.muninn;

import android.app.Activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class HomeActivity extends Activity {

    final CharSequence[] modes = {"Hover", "Loop", "Follow-Me"};
    private int selectedMode;

    private boolean flightStatus = false; // 1 for in flight, 0 otherwise
    private boolean beaconStatus = false; // 1 for connected, 0 otherwise
    protected String currentView;

    private int hover_dist;
    private int loop_radius;
    private int follow_dist;

    private int temp_counter = 0;

    SharedPreferences sharedPreferences;

    // Location variables
    private LocationManager locationManager;
    MyLocationListener myLocationListener;

    protected Button launchLandButton;

    private DrawerLayout mDrawerLayout;
    private View drawerView;

    // Varying TextViews
    private TextView launchLandText;
    private TextView beacon_connection_text;

    // Input Number Fields
    private EditText hover_dist_input;
    private EditText loop_rad_input;
    private EditText follow_dist_input;

    // Fields for drone settings
    private int batteryLevel;
    private boolean isDroneConnected = false;
    private int droneMPH = 0;

    // Fields for Bluetooth
    private int REQUEST_ENABLE_BT = 1;
    private int REQUEST_ENABLE_GPS = 2;
    private ArrayList<BluetoothDevice> foundDevices;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> btArrayAdapter;
    private BluetoothDevice beacon = null;
    private BluetoothSocket btSocket;

    private Handler errHandler;

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
        drawerView = (View)findViewById(R.id.drawer);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {
                TextView dflight_stat = (TextView) findViewById(R.id.drone_flight_status_text);
                TextView dbattery_text = (TextView) findViewById(R.id.battery_level_text);
                TextView dmode = (TextView) findViewById(R.id.drone_status_flight_mode_text);

                ImageView drNotConnected = (ImageView) findViewById(R.id.drone_not_connected_symbol);
                ImageView drConnected = (ImageView) findViewById(R.id.drone_connected_symbol);

                dflight_stat.setText(launchLandText.getText().toString());
                dbattery_text.setText(""+batteryLevel);
                dmode.setText(modes[selectedMode]);

                if(isDroneConnected){
                    drNotConnected.setVisibility(View.INVISIBLE);
                    drConnected.setVisibility(View.VISIBLE);
                }
                else {
                    drNotConnected.setVisibility(View.VISIBLE);
                    drConnected.setVisibility(View.INVISIBLE);
                }
            }
        });

        ImageButton drawerBtn = (ImageButton) findViewById(R.id.drawerImageButton);
        drawerBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Opens the Drawer
                mDrawerLayout.openDrawer(drawerView);
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

        // Setting up Location Listeners
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        myLocationListener = new MyLocationListener();

        // Prompts user to connect to Muninn wifi and bluetooth manually
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Make sure that you have connected to Muninn's wi-fi and bluetooth! And also turn on your location services!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent,REQUEST_ENABLE_GPS);
                }
            }
        });
        builder.setCancelable(false).create().show();

        try {

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final Handler handler = new Handler();

        final class workerThread implements Runnable {

            private String btMsg;
            public workerThread(String msg) {
                btMsg = msg;
            }
            public void run() {
                if(!mBluetoothAdapter.isEnabled()){
                    Log.d("workerThread", "BT is not enabled for a settings worker thread!");
                    return;
                }
                sendBTMessage(btMsg);
                while(!Thread.currentThread().isInterrupted() && mBluetoothAdapter.isEnabled()) {
                    int bytesAvailable;
                    boolean workDone = false;

                    try {
                        Log.d("refreshThread", "attempting btConnection for receiving");
                        final InputStream mmInputStream;
                        mmInputStream = btSocket.getInputStream();
                        bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            Log.d("bt receive","bytes available:" + bytesAvailable);
                            byte[] readBuffer = new byte[1024];
                            mmInputStream.read(packetBytes);

                            for(int i=0;i<bytesAvailable;i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    Log.d("bt receive","String that was received in thread: " + data);

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
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Refresh error: !", Toast.LENGTH_SHORT).show();
                    }
                    catch (NullPointerException e) {
                        e.printStackTrace();
//                        Toast.makeText(getApplicationContext(), "Beacon error: Not connected!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        // Creates handler to refresh settings every 10 seconds
        final Handler settingsHandler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    if (null != beacon) {
                        Log.d("refreshHandler", "Attempting new refresh thread: " + temp_counter++);
                        if(mBluetoothAdapter.isEnabled()){
                            (new Thread(new workerThread("refresh:na"))).start();
                        }
                    }
                    settingsHandler.postDelayed(this, 10000);
            }
        };
        settingsHandler.postDelayed(runnable, 10000);

        final Handler gpsHandler = new Handler();

        Runnable gpsRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d("gpsRunnable", "Inside the gpsRunnable");
                if (selectedMode == 2) { // only need to send GPS every 3 seconds when in this mode.
                    Log.d("gpsRunnable", "Attempting to send GPS coordinates");
                    sendBTMessage(getGPSCoordinatesMessage());
                }
                gpsHandler.postDelayed(this, 5000);
            }
        };
        gpsHandler.postDelayed(gpsRunnable, 5000);

        errHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message){
                beacon_connection_text.setText(R.string.not_connected);
                Toast.makeText(getApplicationContext(), "Oops. Not connected to the beacon!", Toast.LENGTH_SHORT).show();
            }
        };

    }

    // Goes through the action of attempting to launch or land the device, sent via bluetooth
    public void launchLand(View view) {
        if(null == beacon){
            connectBeacon(view);
        }
        else {
            if (flightStatus) { // If in flight
                //send signals to land the drone
                sendBTMessage("launch_land:land");
                launchLandText.setText(R.string.landed);
                launchLandButton.setText(R.string.launch);
                flightStatus = !flightStatus;
            }
            else { // on the ground
                //send signals to launch the drone
                sendBTMessage("launch_land:launch");

                launchLandButton.setText(R.string.land);
                launchLandText.setText(R.string.in_flight);
                flightStatus = !flightStatus;
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
        sendBTMessage(generateSettingsMessage());

        setContentView(R.layout.activity_home);
    }


    private String generateSettingsMessage() {
        String message = "";
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
                        sendBTMessage("flight_mode:"+message+";"+getGPSCoordinatesMessage());
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

        // put paired names and addresses to the adapter
        for(BluetoothDevice device : foundDevices)
            btArrayAdapter.add(device.getName() + "\n" + device.getAddress());

        builder.setSingleChoiceItems(btArrayAdapter, foundDevices.indexOf(beacon), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Toast.makeText(getApplicationContext(), "Using " + foundDevices.get(which).getName() + " as the Muninn beacon", Toast.LENGTH_SHORT).show();
                beacon = foundDevices.get(which);
                beacon_connection_text.setText(R.string.connected);
            }
        });

        builder.create();
        builder.show();
    }

    private void sendBTMessage(String message) {
        Log.d("btMessage",message);

        if(!mBluetoothAdapter.isEnabled()){
            Toast.makeText(getApplicationContext(), "Beacon is not connected! Please hit the connect beacon button!", Toast.LENGTH_SHORT);
        }
        else {
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                //UUID uuid = beacon.getUuids()[0].getUuid();
                if (null == btSocket)
                    btSocket = beacon.createRfcommSocketToServiceRecord(uuid);

                mBluetoothAdapter.cancelDiscovery();
                Log.d("btSocket", "Connected to:" + btSocket.getRemoteDevice().getName());


                if (!btSocket.isConnected()) {
                    Log.d("btSocket", "Using this UUID to connect: " + uuid);
                    btSocket.connect();
                }

                Log.d("btSocket", "CONNECTED!!!");
                String mes = message;
                OutputStream out = btSocket.getOutputStream();
                out.write(mes.getBytes());
                Log.d("btSocket", "" + btSocket.isConnected());

            } catch (Exception e) {
                e.printStackTrace();
                Message m = errHandler.obtainMessage(0);
                m.sendToTarget();
            }
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

    private String getGPSCoordinatesMessage() {
        String gpsMessage = "GPS:";

        //send GPS information
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            if(myLocationListener.longitude == 0){ // doesn't have position from GPS provider
                try{
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if(loc.getLongitude() == 0){ // still doesn't have position
                        return gpsMessage + "ERR";
                    }
                    return gpsMessage + loc.getLatitude()+","+loc.getLongitude();
                }
                catch (SecurityException e) {
                    e.printStackTrace();
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "GPS cannot be found!", Toast.LENGTH_SHORT).show();
                    return gpsMessage + "ERR";
                }
            }
            else
                return gpsMessage + myLocationListener.latitude + "," + myLocationListener.longitude; // Good return
        }
        else{
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
        }

        return gpsMessage + "ERR";
    }

    private void parseMessage(String message) {
        // expecting "battery:70;mph:34;connected:true
        Log.d("receivedBTMessage",message);
        String[] first = message.split(";");

        for(String m : first) {
            String key = m.substring(0,m.indexOf(":"));
            String value = m.substring(m.indexOf(":")+1, m.length());

            switch(key) {
                case "battery" :
                    batteryLevel = Integer.parseInt(value);
                    break;
                case "mph" :
                    droneMPH = Integer.parseInt(value);
                    break;
                case "connected" :
                    if("true".equals(value)){
                        isDroneConnected = true;
                    }
                    else if("false".equals(value)){
                        isDroneConnected = false;
                    }
                    break;
                default :
                    Log.d("parseMessage","Bad Input Value: " + value);
            }
        }
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