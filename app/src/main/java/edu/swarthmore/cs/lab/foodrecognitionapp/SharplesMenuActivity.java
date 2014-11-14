package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

import java.util.UUID;

/**
 * Created by rzevall1 on 11/14/14.
 */
public class SharplesMenuActivity extends Activity {
    private String TAG = "SharplesMenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            UUID foodPhotoId = (UUID)getIntent().getSerializableExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID);
            fragment = SharplesMenuFragment.newInstance();

            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();

        }


    }



}
