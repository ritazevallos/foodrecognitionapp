package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
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
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        try {
            FoodPhotoAdapter la = (FoodPhotoAdapter) getListAdapter();
            FoodPhoto fp = (FoodPhoto) la.getItem(position);
            Intent i = new Intent(getActivity(), PictureTakerActivity.class);
            i.putExtra(PictureTakerFragment.EXTRA_FOODPHOTO_ID, fp.getId());
            startActivity(i);
        } catch (Exception e){
            Log.d(TAG, "List Item click failed. Error = " + e);
        }

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
            ArrayList<FoodPhoto.FoodPhotoTag> tags = fp.getTags();
            // todo: catch if no tag
            String all_tags = "";
            for (int i=0; i<tags.size(); i++){
                if(i == tags.size()-1){
                    all_tags += tags.get(i).getFoodName();
                }else {
                    all_tags += tags.get(i).getFoodName() + ", ";
                }
            }
            tagsTextView.setText(all_tags);

            ImageView imageView = (ImageView) convertView.findViewById(R.id.list_image);
            File file = fp.getFile();
            String path = file.getAbsolutePath();
            int start = path.indexOf("/storage");
            path = path.substring(start, path.length());
            
            //Uri contentUri = Uri.parse(path);
            //Uri contentUri = Uri.fromFile(fp.getFile());

            Log.d(TAG, "File: " + file.toString());
            Log.d(TAG, "Absolute path: " + path);


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            imageView.setImageBitmap(bitmap);


            //Bitmap bitmap;
            try {
                //bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), contentUri);
                //imageView.setImageBitmap(bitmap);
                bitmap = BitmapFactory.decodeFile(path, options);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e){
                Log.e(TAG, "ERROR: " + e.getMessage());
            }
//            } catch (FileNotFoundException e1) {
//                // TODO Auto-generated catch block
//                Log.d(TAG, "FILE NOT FOUND ~~~~~~????");
//                e1.printStackTrace();
//            } catch (IOException e1) {
//                // TODO Auto-generated catch block
//                e1.printStackTrace();
//            }

            return convertView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ((FoodPhotoAdapter)getListAdapter()).notifyDataSetChanged();
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
            Log.d(TAG, "Go to Gallery (from gallery)");
            openGallery();
        }

        if(item.getItemId() == R.id.menu_item_add){
            Log.d(TAG, "Add Food Photo (from gallery)");
            newFoodPhoto();
        }

        if(item.getItemId() == R.id.menu_item_deleteAll){
            Log.d(TAG, "Delete all (from gallery)");
            mFoodPhotoStore.deleteAllPhotos();
            openGallery();
        }
        if(item.getItemId() == R.id.menu_item_main_menu) {
            Log.d(TAG, "Go to main menu (from picture taker)");
            openMainMenu();
        }

        return true;
    }

    /* openGallery
 *  Starts new photo list activity */
    public void openGallery(){
        Intent i = new Intent(getActivity(), FoodPhotoListActivity.class);
        startActivity(i);
        getActivity().finish();
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
}
