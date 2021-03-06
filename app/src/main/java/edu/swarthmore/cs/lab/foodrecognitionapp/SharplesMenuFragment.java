package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;

/**
 * Created by rzevall1 on 11/14/14.
 */
public class SharplesMenuFragment extends Fragment {
    private static final String TAG = "SharplesMenuFragment";
    private TextView mBreakfastMenuView;
    private TextView mLunchMenuView;
    private TextView mDinnerMenuView;
    private SharplesMenu mSharplesMenu;
    private boolean menuIsLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        AsyncSharplesGetter dashScraper = new AsyncSharplesGetter();
        Log.d(TAG, "before execute");
        dashScraper.execute("go!");
        Log.d(TAG, "after execute");
        // todo: asynctask crashes when screen is rotated
        // https://androidresearch.wordpress.com/2013/05/10/dealing-with-asynctask-and-screen-orientation/


        Log.d(TAG, "SharplesMenuFragment: end of onCreate");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.activity_sharples_menu, parent, false);

        mBreakfastMenuView = (TextView) v.findViewById(R.id.breakfast_menu);
        mLunchMenuView = (TextView) v.findViewById(R.id.lunch_menu);
        mDinnerMenuView = (TextView) v.findViewById(R.id.dinner_menu);

        if (menuIsLoaded){
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


    private class AsyncSharplesGetter extends AsyncTask<String, Integer, String> {
        //todo: what are the string, int, string in the constructor?

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //mBreakfastMenuView.setText("loading menu");
            // show progress bar
        }

        @Override
        protected String doInBackground(String... params) {

            // this will get the existing sharples menu, or create one if it doesn't exist
            mSharplesMenu = SharplesMenu.get(getActivity());
            if (mSharplesMenu.isNewDay(new Date())){
                mSharplesMenu = SharplesMenu.get(getActivity(), true);
                // todo: i haven't tested whether the new day thing works
            }


            return "All done!";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // update progress bar
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // refresh view
            menuIsLoaded = true;

            mBreakfastMenuView.setText(mSharplesMenu.getBreakfastMenu().toString());
            mLunchMenuView.setText(mSharplesMenu.getLunchMenu().toString());
            mDinnerMenuView.setText(mSharplesMenu.getDinnerMenu().toString());
            // todo: dismiss progress bar

            Log.d(TAG, "in onPostExecute");
        }
    }
}