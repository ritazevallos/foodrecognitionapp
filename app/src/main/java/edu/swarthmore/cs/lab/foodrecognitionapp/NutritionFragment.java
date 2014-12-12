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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import java.util.Date;

/**
 * Created by Jonah on 11/12/14.
 */
public class NutritionFragment extends Fragment {
        private static final String TAG = "NutritionFragment";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);


        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
            final View v = inflater.inflate(R.layout.activity_nutrition, parent, false);

            TextView titleView = (TextView) v.findViewById(R.id.title);
            titleView.setText("Title");

            GraphViewSeries graphSeries = new GraphViewSeries(new GraphView.GraphViewData[]{
                new GraphView.GraphViewData(1, 20.0d),
                new GraphView.GraphViewData(2, 30.0d),
                new GraphView.GraphViewData(3, 50.0d),
                new GraphView.GraphViewData(4, 20.0d),
            });

            GraphView graphView = new BarGraphView(
                    getActivity(), // context
                    "Nutrition breakdown" //heading
            );
            graphView.addSeries(graphSeries); // data
            graphView.setHorizontalLabels(new String[] {"Iron  ", "Protein  ", "Vitamin C  ", "Sugars  "});
            graphView.setVerticalLabels(new String[] {"0%", "25%", "50%", "100%"});
            graphView.setManualYMaxBound(100);

            LinearLayout layout = (LinearLayout) v.findViewById(R.id.graph_container);
            layout.addView(graphView);

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

            if(item.getItemId() == R.id.menu_item_add){
                Log.d(TAG, "Add Food Photo (from gallery)");
                newFoodPhoto();
            }

            if(item.getItemId() == R.id.menu_item_deleteAll) {
                Log.d(TAG, "Delete all (from picture taker)");
                Toast toast = Toast.makeText(getActivity(), "Delete things from gallery please", Toast.LENGTH_SHORT);
                toast.show();
            }

            if(item.getItemId() == R.id.menu_item_main_menu) {
                Log.d(TAG, "Go to main menu (from picture taker)");
                openMainMenu();
            }

            return true;
        }

        public void openGallery() {
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
            getActivity().finish();
        }

        public void openMainMenu(){
            Intent i = new Intent(getActivity(), MainMenuActivity.class);
            startActivity(i);
            getActivity().finish();
        }


        public static NutritionFragment newInstance() {
            NutritionFragment fragment = new NutritionFragment();

            return fragment;
        }

    }