package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by hbown1 on 11/5/14.
 */
public class MainMenuFragment extends Fragment{

    private static final String TAG = "MainMenuFragment";
    private FoodPhotoStore mFoodPhotoStore;

    /* onCreate */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.main_menu_title);

        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        ArrayList mFoodPhotos = mFoodPhotoStore.getFoodPhotos();

    }


    /* onCreateView
     *  Handles buttons in main menu */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.main_menu, parent, false);

        Button addPhotoButton = (Button)v.findViewById(R.id.add_food_photo);
        addPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FoodPhoto fp = new FoodPhoto();
                //mFoodPhotoStore.addFoodPhoto(fp);
                Intent i = new Intent(getActivity(), PictureTakerActivity.class);
                i.putExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID, fp.getId());
                startActivity(i);
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
                Intent i = new Intent(getActivity(), SharplesMenuActivity.class);
                startActivity(i);
            }
        });

        return v;
    }

    /* onCreateOptionsMenu */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.picture_taker, menu);

    }

    /* onOptionItemSelected */
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.menu_item_gallery) {
            Log.d(TAG, "Go to Gallery selected");

            openGallery();
        }

        if(item.getItemId() == R.id.menu_item_add){
            Log.d(TAG, "Add Food Photo selected");

            newFoodPhoto();
        }

        return true;
    }

    /* openGallery
     *  Starts new photo list activity */
    public void openGallery(){
        Intent i = new Intent(getActivity(), FoodPhotoListActivity.class);
        startActivity(i);
    }

    /* newFoodPhoto
     * Creates new food photo object */
    public void newFoodPhoto(){
        FoodPhoto fp = new FoodPhoto();
        //mFoodPhotoStore.addFoodPhoto(fp);
        Intent i = new Intent(getActivity(), PictureTakerActivity.class);
        i.putExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID, fp.getId());
        startActivity(i);
    }

}
