package flycam.csce_483.muninn;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.VideoView;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

public class CameraActivity extends Activity {

    ImageButton fabCamera;
    ImageButton fabRecord;
    ImageButton fabStopRecord;

    private boolean mode; // true is camera mode, false is video mode
    private boolean recording;
    private boolean goProHero4; // false is hero2, true is hero4

    private VideoView videoView;

    private Switch whichGoPro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        connectToCamera();

        whichGoPro = (Switch) findViewById(R.id.goproSwitch);

        whichGoPro.setChecked(false);
        goProHero4 = false;

        whichGoPro.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    // The switch is enabled
                    Log.d("camera", "GoProHero4 is enabled");
                    goProHero4 = true;
                }
                else {
                    Log.d("camera", "GoProHero4 is disabled");
                    goProHero4 = false;
                }
            }
        });

        videoView = (VideoView) findViewById(R.id.liveVideo);
        String videoURL = "http://10.5.5.9:8080/live/amba.m3u8";
        Uri vidUri = Uri.parse(videoURL);

        Log.d("video","VIDEO URL: "+videoURL);
        videoView.setVideoURI(vidUri);

        final ImageView recordingStatus = (ImageView) findViewById(R.id.recordingStatus);
        fabCamera = (ImageButton) findViewById(R.id.cameraButtonFAB);
        fabRecord = (ImageButton) findViewById(R.id.recordButtonFAB);
        fabStopRecord = (ImageButton) findViewById(R.id.stopButtonFAB);

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
                    else
                        sendRequest("http://10.5.5.9:80/bacpac/SH?t=muninn483&p=%01");
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

                videoView.stopPlayback();
                recording = false;
                fabRecord.setVisibility(View.VISIBLE);
                fabStopRecord.setVisibility(View.INVISIBLE);
                recordingStatus.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void connectToCamera() {

        if(goProHero4){
            sendRequest("http://10.5.5.9/gp/gpControl/command/mode?p=0");
        }
        else
            sendRequest("http://10.5.5.9:80/bacpac/PW?t=muninn483&p=%01");
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
        Volley.newRequestQueue(this).add(stringRequest);
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
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
