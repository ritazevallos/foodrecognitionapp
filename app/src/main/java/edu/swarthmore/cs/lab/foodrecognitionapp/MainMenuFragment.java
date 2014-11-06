package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by hbown1 on 11/5/14.
 */
public class MainMenuFragment extends Fragment{

    private static final String TAG = "MainMenuFragment";
    private FoodPhotoStore mFoodPhotoStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.main_menu_title);

        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        ArrayList mFoodPhotos = mFoodPhotoStore.getFoodPhotos();

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.activity_main_menu, parent, false);

        Button addPhotoButton = (Button)v.findViewById(R.id.add_food_photo);
        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: make new food photo
            }
        });

        Button nutritionButton = (Button)v.findViewById(R.id.view_nutrition_history);
        nutritionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: hook up nutrition history
            }
        });

        Button sharplesMenuButton = (Button)v.findViewById(R.id.todays_menu);
        sharplesMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Hook up sharples menu
            }
        });

        return v;
    }


}
