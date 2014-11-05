package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;


public class PictureTakerFragment extends Fragment {
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "PictureTakerFragment";
    public static File mFile;
    public static File mDir;
    public static Bitmap bitmap;
    public static ImageView _imageView;
    private EditText mTagField;
    private FoodPhoto mFoodPhoto;
    private FoodPhotoStore mFoodPhotoStore;
    public static final String EXTRA_FOODPHOTO_ID =
            "edu.swarthmore.cs.lab.foodrecognitionapp.foodphoto_id";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // we are not making sure there is an app to take pictures
        setHasOptionsMenu(true);
        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        UUID foodPhotoId = (UUID)getArguments().getSerializable(EXTRA_FOODPHOTO_ID);

        CreateDirectoryForPictures();
        mFoodPhoto = mFoodPhotoStore.getFoodPhoto(foodPhotoId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.activity_picture_taker, parent, false);

        Button cameraButton = (Button)v.findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TakeAPicture();
            }
        });

        Button saveButton = (Button)v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTagField.toString().isEmpty()){
                    Context context = getActivity();
                    Toast toast = Toast.makeText(context, "Remember to tag your photo", Toast.LENGTH_SHORT);
                    toast.show();
                } else{
                    mFoodPhotoStore.addFoodPhoto(mFoodPhoto);
                    FoodPhotoStore.get(getActivity()).saveFoodPhotos();
                    openGallery();
                }
            }
        });

        mTagField = (EditText)v.findViewById(R.id.pictureTag);
        mTagField.setTag(mFoodPhoto.getTags());

        mTagField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String msg = s.toString();
                ArrayList<String> tags = new ArrayList<String>();
                tags.add(msg);
                Log.d(TAG, msg);
                // TODO: setTags will be addTag
                mFoodPhoto.setTags(tags);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        _imageView = (ImageView)v.findViewById(R.id.imageView1);
        if (bitmap != null) {
            _imageView.setImageBitmap(bitmap);
            bitmap = null;
        }
        return v;

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "in onActivityResult");

        super.onActivityResult(requestCode,resultCode,data);

        // make it available in the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(mFile);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);

        // display in ImageView. We will resize the bitmap to fit the display
        // Loading the full sized image will consume to much memory
        // and cause the application to crash.
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), contentUri);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        _imageView.setImageBitmap(bitmap);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.picture_taker, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.menu_item_gallery) {
            Log.d(TAG, "In OptionsItemSelected");

            openGallery();
        }

        return true;
    }

    public void openGallery(){
        Intent i = new Intent(getActivity(), FoodPhotoListActivity.class);
        startActivity(i);
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

    public static PictureTakerFragment newInstance(UUID healthID) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FOODPHOTO_ID, healthID);

        PictureTakerFragment fragment = new PictureTakerFragment();
        fragment.setArguments(args);

        return fragment;
    }


    // we want to only save it once we click the save button
//    @Override
//    public void onPause() {
//        super.onPause();
//        FoodPhotoStore.get(getActivity()).saveFoodPhotos();
//    }



}
