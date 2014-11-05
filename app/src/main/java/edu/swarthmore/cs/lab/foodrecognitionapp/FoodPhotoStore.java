package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by hbown1 on 11/4/14.
 */

public class FoodPhotoStore {
    private static FoodPhotoStore sPhotoStore;
    private Context mAppContext;
    private ArrayList<FoodPhoto> mFoodPhotos;
    private static final String TAG = "FoodPhotoStore";
    private FoodPhotoIntentJSONSerializer mSerializer;

    public FoodPhotoStore(Context appContext) {
        mAppContext = appContext;

        mSerializer = new FoodPhotoIntentJSONSerializer(mAppContext, "foodPhotos.json");
        try {
            mFoodPhotos = mSerializer.loadFoodPhotos();
        } catch (Exception e) {
            mFoodPhotos = new ArrayList<FoodPhoto>();
            Log.e(TAG, "Error loading Food Photos: ", e);

        }
    }

    public ArrayList getFoodPhotos(){
        return mFoodPhotos;
    }

    public FoodPhoto getFoodPhoto(UUID id){
        for (FoodPhoto photo : mFoodPhotos) {
            // will loop through all items in ArrayListVar
            // each time through the loop elementVar (which is of type ElementType) will be an item in the list
            if (photo.getId().equals(id)) {
                return photo;
            }
        }

        FoodPhoto emptyPhoto = new FoodPhoto();
        return emptyPhoto;

    }

    public static FoodPhotoStore get(Context c) {
        if (sPhotoStore == null) {
            sPhotoStore = new FoodPhotoStore(c.getApplicationContext());
        }
        return sPhotoStore;
    }

    public void addFoodPhoto(FoodPhoto fp) {
        mFoodPhotos.add(fp);
    }


    public boolean saveFoodPhotos() {
        try {
            mSerializer.saveFoodPhotos(mFoodPhotos);
            Log.d(TAG, "Food photos saved to file.");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error saving Food Photos: ", e);
            return false;
        }
    }

}