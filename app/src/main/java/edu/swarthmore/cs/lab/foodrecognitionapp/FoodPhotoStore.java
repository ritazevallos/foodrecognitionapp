package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.content.Context;
import android.renderscript.Matrix2f;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by hbown1 on 11/4/14.
 */

public class FoodPhotoStore {
    private static FoodPhotoStore sPhotoStore;
    private Context mAppContext;
    private ArrayList<FoodPhoto> mFoodPhotos;
    private NutritionGrid mNutritionGrid;
    private static final String TAG = "FoodPhotoStore";
    private FoodPhotoIntentJSONSerializer mSerializer;

    public FoodPhotoStore(Context appContext) {
        mAppContext = appContext;
        //mNutritionGrid = new NutritionGrid();
        mSerializer = new FoodPhotoIntentJSONSerializer(mAppContext, "foodPhotos.json");
        try {
            mFoodPhotos = mSerializer.loadFoodPhotos();
        } catch (Exception e) {
            mFoodPhotos = new ArrayList<FoodPhoto>();
            Log.e(TAG, "Error loading Food Photos: ", e);

        }
    }

    protected class NutritionGrid {
        private List<List<Double>> mGrid;
        private Map<String, Integer> mNutrientIndices;
        private Map<Date, Integer> mTimeStampIndices;

        public NutritionGrid(){
            mNutrientIndices = new HashMap<String, Integer>();
            List<String> defaultNutrients = new ArrayList<String>(Arrays.asList(
                    "CARBOHYDRATES",
                    "PROTEIN",
                    "FAT",
                    "VITAMIN_C",
                    "VITAMIN_D",
                    "POTASSIUM",
                    "CALCIUM"
            ));
            //todo: populate these column headings from our database
            mTimeStampIndices = new HashMap<Date, Integer>();

            for (int i=0; i < defaultNutrients.size(); i++){
                mNutrientIndices.put(defaultNutrients.get(i), i);
                mGrid.add(new ArrayList<Double>());
            }
        }

        public void addFood(String foodName) throws Exception{
            Map<String, Double> nutritionInfo = new HashMap<String, Double>(){{
                put("CARBOHYDRATES", 20.0);
                put("PROTEIN", 19.7);
            }};
            // todo: load map of nutrition info from database given food name

            for (Map.Entry<String,Integer> entry : mNutrientIndices.entrySet()){
                String nutrient = entry.getKey();
                Integer nutrientIndex = entry.getValue();

                try{
                    double grams = nutritionInfo.get(nutrient);
                    List<Double> nutrientRow = mGrid.get(nutrientIndex);
                    int timestampIndex = nutrientRow.size();
                    nutrientRow.add(grams);
                    mTimeStampIndices.put(new Date(), timestampIndex);

                } catch(Exception e){
                    throw new Exception("Could not find nutrient "+nutrient+" in database for food"+ foodName +": "+e);
                }
            }
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
                Log.d(TAG, "returning existing photo with id" + id.toString());
                return photo;
            }
        }

        FoodPhoto emptyPhoto = new FoodPhoto();
        Log.d(TAG, "returning new photo");
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

    public void deleteAllPhotos(){
        boolean deleted = mSerializer.deleteJsonFile();
        Log.d(TAG, "Json file deleted = " + deleted);
        mFoodPhotos = new ArrayList<FoodPhoto>();
    }

}