package edu.swarthmore.cs.lab.foodrecognitionapp;

/**
 * Created by jschwar1 on 12/5/14.
 */
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends Activity {

    public static final int MEDIA_TYPE_IMAGE = 1;
    private Camera mCamera;
    private CameraPreview mPreview;
    public static final String EXTRA_URI = "edu.swarthmore.cs.lab.foodrecognitionapp";
    private Uri mUri;
    private static String TAG = "CameraActivity";
    private File mFile;
    private Bitmap mBitmap;

    public CameraActivity() {
        mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("TAG","Callback start");

                Bitmap mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                mUri = Uri.parse(getIntent().getStringExtra(EXTRA_URI));
                final File mFile = new File(mUri.getPath());

                try {
                    FileOutputStream fos = new FileOutputStream(mFile);
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    Log.d("Ok", mFile.getAbsolutePath());
                    fos.flush();
                    fos.close();
                    //File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Path");
                    MediaScannerConnection.scanFile(getApplicationContext(), new String[]{mFile.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {

                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            // TODO Auto-generated method stub
                        }
                    });
                    Log.d("TAG","DONE");
                } catch (FileNotFoundException e) {
                    Log.d("Test", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("Test", "Error accessing file: " + e.getMessage());
                }
                setResult(Activity.RESULT_OK);
                finish();
            }
        };
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        // Create an instance of Camera
        mCamera = getCameraInstance();

        setCameraDisplayOrientation(this, mCamera);


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.camera_preview);
        frameLayout.addView(mPreview);

        RelativeLayout relativeLayoutControls = (RelativeLayout) findViewById(R.id.controls_layout);
        relativeLayoutControls.bringToFront();

        RelativeLayout relativeLayoutSensorsData = (RelativeLayout) findViewById(R.id.sensors_data_layout);
        relativeLayoutSensorsData.bringToFront();

        Button captureButton = (Button) relativeLayoutControls.findViewById(R.id.takePictureButton);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Log.d("Take","Picture");
                        mCamera.takePicture(null, null, mPicture);
                        // mCamera.stopPreview();
                        // mCamera.startPreview();
                    }
                }
        );

    }

    private Camera.PictureCallback mPicture;

    public void logSensorData(View view)
    {
        Toast.makeText(getApplicationContext(), "Logged", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //releaseCamera();

    }
    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, android.hardware.Camera camera) {
        int cameraId = 0; // this is a hack - need to actually search for correct id if there are more than one
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }


}