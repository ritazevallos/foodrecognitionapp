package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;


public class PictureTaker extends Activity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri fileUri;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "PictureTaker";
    public static File _file;
    public static File _dir;
    public static Bitmap bitmap;
    public static ImageView _imageView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_taker);
//
//        if (IsThereAnAppToTakePictures())
//        {
            CreateDirectoryForPictures();
//        }

        Button button = (Button)findViewById(R.id.myButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clicked that button");
                TakeAPicture();
            }
        });
        _imageView = (ImageView)findViewById(R.id.imageView1);
        if (bitmap != null) {
            _imageView.setImageBitmap(bitmap);
            bitmap = null;
        }


//        // create Intent to take a picture and return control to the calling application
//        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//
//        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
//        i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
//
//        if (i.resolveActivity(getPackageManager()) != null) {
//            startActivityForResult(i, REQUEST_IMAGE_CAPTURE);
//        }
    }

    private void CreateDirectoryForPictures()
    {
        _dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "BOOKS");
        if (!_dir.exists())
        {
            _dir.mkdirs();
        }
    }

    private void TakeAPicture()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        _file = new File(_dir, String.format("foodPhoto_{0}.jpg", UUID.randomUUID()));

        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(_file));

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

        super.onActivityResult(requestCode,resultCode,data);

        // make it available in the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(_file);
        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);

        // display in ImageView. We will resize the bitmap to fit the display
        // Loading the full sized image will consume to much memory
        // and cause the application to crash.
        int height = (new DisplayMetrics()).heightPixels;
        int width = _imageView.getWidth() ;
        bitmap = LoadAndResizeBitmap(_file.getPath(), width, height);
    }

    public static Bitmap LoadAndResizeBitmap(String fileName, int width, int height)
    {
        // First we get the the dimensions of the file on disk
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, options);

        // Next we calculate the ratio that we need to resize the image by
        // in order to fit the requested dimensions.
        int outHeight = options.outHeight;
        int outWidth = options.outWidth;
        int inSampleSize = 1;

        if (outHeight > height || outWidth > width)
        {
            inSampleSize = outWidth > outHeight
                    ? outHeight / height
                    : outWidth / width;
        }

        // Now we will load the image and have BitmapFactory resize it for us.
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        Bitmap resizedBitmap = BitmapFactory.decodeFile(fileName, options);

        return resizedBitmap;
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
