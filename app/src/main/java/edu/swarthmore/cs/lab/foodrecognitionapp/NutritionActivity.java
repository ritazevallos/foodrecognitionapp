package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import java.util.UUID;

/**
 * Created by Jonah on 11/12/14.
 */
public class NutritionActivity extends Activity{

    private String TAG = "NutritionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            //UUID foodPhotoId = (UUID)getIntent().getSerializableExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID);
            fragment = NutritionFragment.newInstance();

            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();

        }


    }



}