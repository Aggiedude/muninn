package flycam.csce_483.muninn;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonWriter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class HomeActivity extends Activity {

    private boolean flightStatus = false; // 1 for in flight, 0 otherwise
    private boolean beaconStatus = false; // 1 for connected, 0 otherwise
    protected String currentView;

    protected Button launchLandButton;

    // Varying TextViews
    private TextView launchLandText;
    private TextView beacon_connection_text;

    @Override
    public void setContentView(int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, null);
        //tag is needed for pressing back button to go back to splash screen
        currentView = (String)view.getTag();
        super.setContentView(view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();

        setContentView(R.layout.activity_home);

        launchLandText = (TextView) findViewById(R.id.launch_land_status);
        beacon_connection_text = (TextView) findViewById(R.id.beacon_status);

        // Creates handler to call the refreshSettings method every 10 seconds
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshSettings();
            }
        }, 10000);

    }



    // Goes through the action of attempting to launch or land the device, sent via bluetooth
    private void launchLand(View view) {

        if(beaconStatus) {
            if(flightStatus) { // If in flight
                //send signals to land the drone
                launchLandButton.setText(R.string.landing);
                launchLandButton.setClickable(false);
                launchLandText.setText(R.string.landing);
            }
            else { // on the ground
                //send signals to launch the drone
                launchLandButton.setText(R.string.land);
                launchLandText.setText(R.string.in_flight);
            }
        }
        else
            connectBeacon();

    }

    // Switches view to the application settings menu
    public void appSettings(View view){
        getActionBar().show();
        setContentView(R.layout.app_settings_menu);
    }

    // Updates the current mode selection
    public void selectMode(View view) {

    }

    public void goToCamera(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }



    // Goes through the process of connecting the phone to the beacon
    private void connectBeacon() {



        // Code to connect to beacon



        //Creates pop-up letting user know the whether or not the beacon was successfully connected
        Toast toast = Toast.makeText(HomeActivity.this, "Beacon NOT connected!", Toast.LENGTH_SHORT);
        if(beaconStatus){
            toast = Toast.makeText(HomeActivity.this, "Beacon connected!", Toast.LENGTH_SHORT);
        }
        toast.setGravity(Gravity.BOTTOM, 0, 200);
        toast.show();
    }

    // Will be used to refresh the connections, update drone status, etc.
    // Will connect to the beacon to retrieve the latest information
    private void refreshSettings() {
        Log.d("main", "Settings Refreshed");
        //retrieve information

        if(!flightStatus /*&& still in flight via signals sent: still in the process of landing*/) {
            launchLandText.setText(R.string.landing);
            launchLandButton.setText(R.string.landing);
        }
        else if(!flightStatus /*&& landed via signals*/) {
            launchLandText.setText(R.string.landed);
            launchLandButton.setText(R.string.launch);
            launchLandButton.setClickable(true);
        }

    }

    private void generateJSON() {
       //Json Writer object
        OutputStream out = new OutputStream() {
            @Override
            public void write(int oneByte) throws IOException {

            }
        }
    }

    @Override
    public void onBackPressed() {
        //Checks if the current screen is not the activity_main.xml layout
        if (!currentView.equals("main")) {
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