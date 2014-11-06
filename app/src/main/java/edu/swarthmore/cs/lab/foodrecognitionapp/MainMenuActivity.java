package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import java.util.UUID;

/**
 * Created by hbown1 on 11/5/14.
 */
public class MainMenuActivity extends Activity {
    private String TAG = "PictureTakerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            UUID foodPhotoId = (UUID)getIntent().getSerializableExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID);
            fragment = PictureTakerFragment.newInstance(foodPhotoId);
            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();

        }


    }



}