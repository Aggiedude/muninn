package flycam.csce_483.muninn;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.os.Handler;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;

public class CameraActivity extends Activity {

    ImageButton fabCamera;
    ImageButton fabRecord;
    ImageButton fabStopRecord;

    private boolean mode; // true is camera mode, false is video mode
    private boolean recording;
    private boolean goProHero4; // false is hero2, true is hero4

    private ImageView imageView;

    private Switch whichGoPro;

    private int currentGoProNumber = 0;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        RequestQueue queue = MySingleton.getInstance(this.getApplicationContext()).getRequestQueue();
        imageView = (ImageView) findViewById(R.id.livePhoto);

        whichGoPro = (Switch) findViewById(R.id.goproSwitch);

        whichGoPro.setChecked(true);
        goProHero4 = true;
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        currentGoProNumber = sharedPreferences.getInt("currentGoProNumber", 0);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Log.d("wifi", "Connected to: " + wifiManager.getConnectionInfo().getSSID());

        String wifiSSID = wifiManager.getConnectionInfo().getSSID();

        if(!wifiSSID.equals("\"Muninn\"")){
            // Prompts user to connect to Muninn wifi and bluetooth manually
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("You are not connected to the correct wifi! Once connected, you will be able to access camera functionality!\n*HINT* Be sure your GoPro's network id is \"Muninn\".");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ; // purposefully left blank
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            builder.setCancelable(false).create().show();
        }
        else {
            connectToCamera();
        }


        Log.d("camera", "Made it through, current goProNumber is: " + currentGoProNumber);

        whichGoPro.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The switch is enabled
                    Log.d("camera", "GoProHero4 is enabled");
                    goProHero4 = true;

                } else {
                    Log.d("camera", "GoProHero4 is disabled");
                    goProHero4 = false;
                }
                connectToCamera();
            }
        });

        final ImageView recordingStatus = (ImageView) findViewById(R.id.recordingStatus);
        fabCamera = (ImageButton) findViewById(R.id.cameraButtonFAB);
        fabRecord = (ImageButton) findViewById(R.id.recordButtonFAB);
        fabStopRecord = (ImageButton) findViewById(R.id.stopButtonFAB);

        final Handler handler = new Handler();

        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("camera", "mode = " + mode);
                Log.d("camera", "recording = " + recording);

                if (!mode && !recording) {
                    mode = true;
                    Log.d("camera", "Made it through to mode switch");

                    // switches to camera mode
                    if(goProHero4) {
                        sendRequest("http://10.5.5.9/gp/gpControl/command/mode?p=1");
                    }
                    else {
                        sendRequest("http://10.5.5.9:80/camera/CM?t=muninn483&p=%01");
                    }
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (mode && !recording) {
                    Log.d("camera", "Made it through to shutter switch");



                    // send request to take picture on goPro
                    if(goProHero4){
                        sendRequest("http://10.5.5.9/gp/gpControl/command/shutter?p=1");
                    }
                    else {
                        sendRequest("http://10.5.5.9:80/bacpac/SH?t=muninn483&p=%01");
                    }
                    incrementGoProNumber();

                    String photoNum = String.format("%04d", currentGoProNumber);
                    Log.d("photoNum", "Current photo Num: " + photoNum);

                    String url = "";
                    if(goProHero4){
                        url = "http://10.5.5.9/videos/DCIM/100GOPRO/GOPR"+photoNum+".JPG";
                    }
                    else {
                        url = "http://10.5.5.9:8080/videos/DCIM/100GOPRO/GOPR"+photoNum+".JPG";
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ImageRequest request = new ImageRequest(url,
                            new Response.Listener<Bitmap>() {
                                @Override
                                public void onResponse(Bitmap bitmap) {
                                    imageView.setImageBitmap(bitmap);
                                    Log.d("camera","take a picture, set a picture works!");
                                }
                            }, 0, 0, null,
                            new Response.ErrorListener() {
                                public void onErrorResponse(VolleyError error) {
                                    error.printStackTrace();
                                    Log.d("camera", "take a picture, set a picture DOESN'T works!");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(CameraActivity.this, "Camera is unable to grab Live Picture!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                    // Access the RequestQueue through your singleton class.
                    MySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
                }
            }
        });

        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode){
                    mode = false;

                    // switches to video mode
                    if(goProHero4){
                        sendRequest("http://10.5.5.9/gp/gpControl/command/mode?p=0");
                    }
                    else {
                        sendRequest("http://10.5.5.9:80/camera/CM?t=muninn483&p=%00");
                    }

                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                if (!mode && !recording) {
                    // send request to start recording on goPro

                    if(goProHero4){
                        sendRequest("http://10.5.5.9/gp/gpControl/command/shutter?p=1");
                    }
                    else {
                        sendRequest("http://10.5.5.9:80/bacpac/SH?t=muninn483&p=%01");
                    }

                    recording = true;
                    fabRecord.setVisibility(View.INVISIBLE);
                    fabStopRecord.setVisibility(View.VISIBLE);
                    recordingStatus.setVisibility(View.VISIBLE);
                }
            }
        });

        fabStopRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // send request to stop recording on goPro
                if(goProHero4){
                    sendRequest("http://10.5.5.9/gp/gpControl/command/shutter?p=0");
                }
                else {
                    sendRequest("http://10.5.5.9:80/bacpac/SH?t=muninn483&p=%00");
                }

                incrementGoProNumber();

                recording = false;
                fabRecord.setVisibility(View.VISIBLE);
                fabStopRecord.setVisibility(View.INVISIBLE);
                recordingStatus.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void connectToCamera() {

        if(goProHero4){
            sendRequest("http://10.5.5.9/gp/gpControl/command/mode?p=1");
            mode = true;
        }
        else
            sendRequest("http://10.5.5.9:80/camera/CM?t=muninn483&p=%01");
    }

    private void incrementGoProNumber() {

        currentGoProNumber++;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("currentGoProNumber", currentGoProNumber);
        editor.commit();
    }

   private void sendRequest(String s) {
        String url = s;

        // Request a string response
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        // Result handling
                        Log.d("camera","SUCCESS");
                        Log.d("camera", response);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                // Error handling
                Log.d("camera","Something went wrong!");
                error.printStackTrace();

            }
        });

        // Add the request to the queue
        MySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putBoolean("MyBoolean", true);
        savedInstanceState.putDouble("myDouble", 1.9);
        savedInstanceState.putInt("MyInt", 1);
        savedInstanceState.putString("MyString", "Welcome back to Android");
        // etc.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
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
