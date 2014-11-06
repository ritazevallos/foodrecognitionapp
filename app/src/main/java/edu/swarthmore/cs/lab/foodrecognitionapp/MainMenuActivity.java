package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;


/**
 * Created by hbown1 on 11/5/14.
 */
public class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getFragmentManager();

        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);

        if (fragment == null) {
            fragment = new MainMenuFragment();
            fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();

        }


    }



}