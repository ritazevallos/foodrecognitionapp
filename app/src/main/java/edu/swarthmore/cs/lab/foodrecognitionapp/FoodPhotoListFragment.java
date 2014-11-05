package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.ListFragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by hbown1 on 11/4/14.
 */
public class FoodPhotoListFragment  extends ListFragment {

    private static final String TAG = "FoodPhotoListFragment";
    private FoodPhotoStore mFoodPhotoStore;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.food_photo_title);

        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        ArrayList mFoodPhotos = mFoodPhotoStore.getFoodPhotos();

        FoodPhotoAdapter adapter = new FoodPhotoAdapter(mFoodPhotos);
        setListAdapter(adapter);


    }



    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FoodPhotoAdapter la = (FoodPhotoAdapter)getListAdapter();
        FoodPhoto fp = (FoodPhoto)la.getItem(position);
        Intent i = new Intent(getActivity(), PictureTakerFragment.class);
        i.putExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID, fp.getId());
        startActivity(i);


    }

    private class FoodPhotoAdapter extends ArrayAdapter<FoodPhoto> {
        public FoodPhotoAdapter(ArrayList<FoodPhoto> foodPhotos) {
            super(getActivity(), 0, foodPhotos);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // if we weren't given a view, inflate one
            if (null == convertView) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.list_item_food_photo, null);
            }

            // configure the view for this FoodPhoto
            FoodPhoto fp = getItem(position);

            TextView dateTextView =
                    (TextView) convertView.findViewById(R.id.food_photo_list_dateTextView);
            dateTextView.setText(fp.getDate().toString());

            TextView tagsTextView =
                    (TextView) convertView.findViewById(R.id.food_photo_list_tagsTextView);
            ArrayList<String> tags = fp.getTags();
            // todo: catch if no tag
            tagsTextView.setText(tags.get(0));

            ImageView imageView = (ImageView) convertView.findViewById(R.id.list_image);
            Uri contentUri = Uri.fromFile(fp.getFile());
            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), contentUri);
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            return convertView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((FoodPhotoAdapter)getListAdapter()).notifyDataSetChanged();
    }

}
