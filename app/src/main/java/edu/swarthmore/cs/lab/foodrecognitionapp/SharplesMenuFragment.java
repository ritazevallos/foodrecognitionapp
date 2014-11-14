package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by rzevall1 on 11/14/14.
 */
public class SharplesMenuFragment extends Fragment {
    private static final String TAG = "SharplesMenuFragment";
    private SharplesMenu mSharplesMenu;
    private TextView mBreakfastMenuView;
    private TextView mLunchMenuView;
    private TextView mDinnerMenuView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mSharplesMenu = new SharplesMenu(getActivity());

        Log.d(TAG, "SharplesMenuFragment: end of onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.activity_sharples_menu, parent, false);

        mBreakfastMenuView = (TextView) v.findViewById(R.id.breakfast_menu);
        mLunchMenuView = (TextView) v.findViewById(R.id.lunch_menu);
        mDinnerMenuView = (TextView) v.findViewById(R.id.dinner_menu);

        if (mSharplesMenu.getLoaded()){
            mBreakfastMenuView.setText(mSharplesMenu.getBreakfastMenu().toString());
            mLunchMenuView.setText(mSharplesMenu.getLunchMenu().toString());
            mDinnerMenuView.setText(mSharplesMenu.getDinnerMenu().toString());
        } else {
            Log.d(TAG, "Sharples menu failed to load");
        }

        return v;


    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.picture_taker, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_gallery) {
            Log.d(TAG, "In OptionsItemSelected");

            openGallery();
        }

        return true;
    }

    public void openGallery() {
        Intent i = new Intent(getActivity(), FoodPhotoListActivity.class);
        startActivity(i);
    }


    public static SharplesMenuFragment newInstance() {
        SharplesMenuFragment fragment = new SharplesMenuFragment();

        return fragment;
    }
}