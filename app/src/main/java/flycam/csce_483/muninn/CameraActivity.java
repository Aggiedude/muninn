package flycam.csce_483.muninn;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;
import android.widget.VideoView;
import android.widget.MediaController;

public class CameraActivity extends Activity {

    ImageButton fabCamera;
    ImageButton fabRecord;
    ImageButton fabStopRecord;

    boolean recording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        VideoView videoView = (VideoView) findViewById(R.id.liveVideo);

        String videoURL = "";
        Uri vidUri = Uri.parse(videoURL);

        videoView.setVideoURI(vidUri);


        fabCamera = (ImageButton) findViewById(R.id.cameraButtonFAB);
        fabRecord = (ImageButton) findViewById(R.id.recordButtonFAB);
        fabStopRecord = (ImageButton) findViewById(R.id.stopButtonFAB);

        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ;// send request to take picture on goPro
            }
        });

        fabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ;// send request to start recording on goPro
            }
        });

        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ;// send request to stop recording on goPro
            }
        });
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
