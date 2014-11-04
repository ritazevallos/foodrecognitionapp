package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;


public class PictureTaker extends Activity {
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "PictureTaker";
    public static File mFile;
    public static File mDir;
    public static Bitmap bitmap;
    public static ImageView _imageView;
    private EditText mTagField;
    private FoodPhoto mFoodPhoto;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_taker);

        // we are not making sure there is an app to take pictures
        CreateDirectoryForPictures();

        mFoodPhoto = new FoodPhoto();

        Button button = (Button)findViewById(R.id.myButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TakeAPicture();
            }
        });

        mTagField = (EditText)findViewById(R.id.pictureTag);
        mTagField.setTag(mFoodPhoto.getTag());

        mTagField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String msg = s.toString();
                Log.d(TAG, msg);
                mFoodPhoto.setTag(msg);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        _imageView = (ImageView)findViewById(R.id.imageView1);
        if (bitmap != null) {
            _imageView.setImageBitmap(bitmap);
            bitmap = null;
        }

    }

    private void CreateDirectoryForPictures()
    {
        mDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "BOOKS");
        if (!mDir.exists())
        {
            mDir.mkdirs();
        }
    }

    private void TakeAPicture()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        mFile = new File(mDir, String.format("foodPhoto_"+ UUID.randomUUID() + ".jpg"));
        Log.d(TAG, "file path: " + mFile.toString());
        mFoodPhoto.setFile(mFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));

        startActivityForResult(intent, 0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.picture_taker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "in onActivityResult");

        super.onActivityResult(requestCode,resultCode,data);

        // make it available in the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(mFile);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);

        // display in ImageView. We will resize the bitmap to fit the display
        // Loading the full sized image will consume to much memory
        // and cause the application to crash.
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), contentUri);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        _imageView.setImageBitmap(bitmap);
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Food");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Food", "failed to create directory");
                return null;
            } else {
                Log.d("Food", "successfully created directory");
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }
}
