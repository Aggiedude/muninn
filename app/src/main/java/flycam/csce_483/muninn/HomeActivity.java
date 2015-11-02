package flycam.csce_483.muninn;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private boolean flightStatus = false; // 1 for in flight, 0 otherwise
    private boolean beaconStatus = false; // 1 for connected, 0 otherwise
    protected String currentView;

    // Varying TextViews
    private TextView launch_land_text;
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

        launch_land_text = (TextView) findViewById(R.id.launch_land_status);
        beacon_connection_text = (TextView) findViewById(R.id.beacon_status);
    }



    // Goes through the action of attempting to launch or land the device
    private void launchLand(View view) {
        Button launchLandButton = (Button) findViewById(R.id.button_launch_land);

        // code goes here in order to connect

    }

    // Switches view to the application settings menu
    public void appSettings(View view){
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
    private void refreshSettings() {

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