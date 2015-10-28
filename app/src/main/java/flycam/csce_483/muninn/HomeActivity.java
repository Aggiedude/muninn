package flycam.csce_483.muninn;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.Button;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private boolean flightStatus = false; // 1 for in flight, 0 otherwise
    private boolean beaconStatus = false; // 1 for connected, 0 otherwise

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();

        setContentView(R.layout.activity_home);

    }



    // Goes through the action of attempting to launch or land the device
    private void launchLand(View view) {
        Button launchLandButton = (Button) findViewById(R.id.button_launch_land);

        // code goes here in order to connect

    }

    // Switches view to the beacon settings menu
    public void beaconSettings(View view){
        setContentView(R.layout.beacon_settings_menu);
    }

    // Switches view to the application settings menu
    public void appSettings(View view){
        setContentView(R.layout.app_settings_menu);
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


}