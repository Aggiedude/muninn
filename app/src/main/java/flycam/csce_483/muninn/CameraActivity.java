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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.VideoView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CameraActivity extends Activity {

    ImageButton fabCamera;
    ImageButton fabRecord;
    ImageButton fabStopRecord;

    private boolean mode; // true is camera mode, false is video mode
    private boolean recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        connectToCamera();

        VideoView videoView = (VideoView) findViewById(R.id.liveVideo);

        String videoURL = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
        Uri vidUri = Uri.parse(videoURL);

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
                    String response = sendRequest("10.5.5.9/gp/gpControl/command/mode?p=1"); // switches to camera mode
                    Log.d("camera", response);
                }
                if (mode && !recording) {
                    Log.d("camera", "Made it through to shutter switch");
                    sendRequest("10.5.5.9/gp/gpControl/command/shutter?p=1");// send request to take picture on goPro
                }
            }
        });

        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode){
                    mode = false;
                    sendRequest("10.5.5.9/gp/gpControl/command/mode?p=0"); // switches to video mode
                }
                if (!mode && !recording) {
                    sendRequest("10.5.5.9/gp/gpControl/command/shutter?p=1");// send request to start recording on goPro
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
                sendRequest("10.5.5.9/gp/gpControl/command/shutter?p=0");// send request to stop recording on goPro
                recording = false;
                fabRecord.setVisibility(View.VISIBLE);
                fabStopRecord.setVisibility(View.INVISIBLE);
                recordingStatus.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void connectToCamera() {

        String URL = "10.5.5.9/gp/gpControl/command/mode?p=0";

        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(URL));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                String responseString = out.toString();
                out.close();
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
            }
        }
        catch(IOException e){
            Log.e("Camera", "Connection to camera failed");
        }

    }

    private String sendRequest(String s) {

        String response = "";
        try {
            URL url = new URL(s);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            response = readStream(in);
            urlConnection.disconnect();
        }
        catch(IOException e) {
            return "";
        }

        return response;
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
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
