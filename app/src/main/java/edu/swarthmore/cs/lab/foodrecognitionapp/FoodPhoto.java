package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by rzevall1 on 11/3/14.
 */
public class FoodPhoto {
    private ArrayList<FoodPhotoTag> mTags;
    private File mFile;
    private UUID mId;
    private Date mDate;

    private static final String JSON_ID = "id";
    private static final String JSON_TAGS = "tags";
    private static final String JSON_DATE = "date";
    private static final String JSON_FILE_URI = "file";

    private String TAG = "FoodPhoto";

    public FoodPhoto(){
        mId = UUID.randomUUID();
        mDate = new Date();
        mTags = new ArrayList<FoodPhotoTag>();
    }

    public ArrayList<FoodPhotoTag> getTags() {
        return mTags;
    }

    public void setTags(ArrayList<FoodPhotoTag> tags) {
        mTags = tags;
    }

    public void setOneTag(String foodName, Point ll, Point ur, int i){
        FoodPhotoTag tag = new FoodPhotoTag(foodName, ll, ur);
        try{
            mTags.add(i,tag);
        } catch (Exception e){
            Log.d(TAG, "broke trying to set a tag for invalid index; "+e);
        }
    }

    public void setOneTag(String foodName, Point ll, Point ur){
        FoodPhotoTag tag = new FoodPhotoTag(foodName, ll, ur);
        mTags.add(tag);
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID id) {
        mId = id;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    @Override
    public String toString() { return mFile.toString();}

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_ID, mId.toString());
        json.put(JSON_TAGS, mTags.get(0).getFoodName()); //TODO: only saving the first tag as a string
        json.put(JSON_DATE, mDate.getTime());
        json.put(JSON_FILE_URI, Uri.fromFile(mFile).toString());//todo: no idea if this is the right way to do this
        return json;
    }

    // given the JSON of the foodPhoto, this is a constructor
    public FoodPhoto(JSONObject json) throws JSONException {
        mId = UUID.fromString(json.getString(JSON_ID));
        mTags = new ArrayList<FoodPhotoTag>();
        String string_tags = json.getString(JSON_TAGS); // currently just a single string
        FoodPhotoTag new_tag = new FoodPhotoTag(string_tags, new Point(), new Point());
        mTags.add(new_tag); // temporary losing all data except the string of the first tag
        mDate = new Date(json.getLong(JSON_DATE));
        mFile = new File(json.getString(JSON_FILE_URI));

    }

    public class FoodPhotoTag {
        private String mFoodName;
        private Point ll;
        private Point ur;

        private FoodPhotoTag(String foodName, Point ll, Point ur) {
            mFoodName = foodName;
            this.ll = ll;
            this.ur = ur;
        }
        public Point getLl() {
            return ll;
        }

        public void setLl(Point ll) {
            this.ll = ll;
        }

        public Point getUr() {
            return ur;
        }

        public void setUr(Point ur) {
            this.ur = ur;
        }

        public String getFoodName() {
            return mFoodName;
        }

        public void setFoodName(String foodName) {
            mFoodName = foodName;
        }

    }
}
