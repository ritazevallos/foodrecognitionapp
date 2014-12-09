package edu.swarthmore.cs.lab.foodrecognitionapp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import edu.swarthmore.cs.lab.foodrecognitionapp.R;

public class FromGalleryActivity extends Activity {

    private FoodPhotoStore mFoodPhotoStore;
    private String TAG = "FromGalleryActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFoodPhotoStore = FoodPhotoStore.get(this);
        setContentView(R.layout.main);

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        setContentView(R.layout.activity_fragment);

        UUID foodPhotoId = (UUID)getIntent().getSerializableExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {

            if (resultCode == RESULT_OK){

                FoodPhoto fp = mFoodPhotoStore.getFoodPhoto(foodPhotoId);
                Uri uri = data.getData();
                File file = new File(uri.getPath());
                Date date = new Date(file.lastModified());
                fp.setUri(uri);
                fp.setFile(file);
                fp.setDate(date);
                mFoodPhotoStore.addFoodPhoto(fp);

                Log.e(TAG, "Photo File in gallery activity: " + fp.getFile());

                fragment = PictureTakerFragment.newInstance(foodPhotoId);
                fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();


            }


        }

    }




}