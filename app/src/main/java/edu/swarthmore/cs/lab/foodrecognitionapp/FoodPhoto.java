package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Point;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import junit.framework.TestCase;

/**
 * Created by rzevall1 on 11/3/14.
 */
public class FoodPhoto {
    private ArrayList<FoodPhotoTag> mTags;
    private File mFile;
    private UUID mId;
    private Date mDate;
    private Uri mUri;
    private Boolean mFromGallery;
    private Gson gson;
    private JSONObject json;

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

    public String cleanedFilePath(){
        String path = mFile.getAbsolutePath();
        int start = path.indexOf("/storage");
        path = path.substring(start, path.length());
        return path;
    }

    public void setTags(ArrayList<FoodPhotoTag> tags) {
        mTags = tags;
    }

//    public void setOneTag(String foodName, Point ll, Point ur, int i){
//        FoodPhotoTag tag = new FoodPhotoTag(foodName, ll, ur);
//        try{
//            mTags.set(i,tag);
//        } catch (Exception e){
//            Log.d(TAG, "broke trying to set a tag for invalid index; "+e);
//        }
//    }

    public void setOneTag(String foodName, Point ll, Point ur, int guessScore){
        FoodPhotoTag tag = new FoodPhotoTag(foodName, ll, ur, guessScore);
        mTags.add(tag);
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
        String path = cleanedFilePath();
        mFile = new File(path);
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

    public Uri getUri() { return Uri.fromFile(this.mFile); }

    public void setUri(Uri uri) { this.mUri = uri; }

    @Override
    public String toString() {
        if(mFile!=null) {
            return mFile.toString();
        } else {
            return "No file found";
        }
    }


    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_ID, mId.toString());


        gson = new Gson();
        String jsonTags = gson.toJson(mTags);
        Log.d(TAG, "Tags saved: " + mTags.get(1).getFoodName());

        json.put(JSON_TAGS, jsonTags);
        //json.put(JSON_TAGS, mTags.get(0).getFoodName()); //only saving the first tag as a string


        json.put(JSON_DATE, mDate.getTime());
        json.put(JSON_FILE_URI, Uri.fromFile(mFile).toString());//todo: no idea if this is the right way to do this
        return json;
    }

    // given the JSON of the foodPhoto, this is a constructor
    public FoodPhoto(JSONObject json) throws JSONException {
        mId = UUID.fromString(json.getString(JSON_ID));
        mTags = new ArrayList<FoodPhotoTag>();
        if(gson == null){ gson = new Gson();  }

        Type type = new TypeToken<ArrayList<FoodPhotoTag>>(){}.getType();
        Object obj = json.get(JSON_TAGS);
        String string = obj.toString();
        mTags = gson.fromJson(string, type);


        //String string_tags = json.getString(JSON_TAGS); // currently just a single string
        //FoodPhotoTag new_tag = new FoodPhotoTag(string_tags, new Point(), new Point());
        //mTags.add(new_tag); // temporary losing all data except the string of the first tag
        mDate = new Date(json.getLong(JSON_DATE));
        mFile = new File(json.getString(JSON_FILE_URI));

    }

    public class FoodPhotoTag {
        private String mFoodName;
        private Point ll;
        private Point ur;
        private int guessScore;

        private FoodPhotoTag(String foodName, Point ll, Point ur) {
            mFoodName = foodName;
            this.ll = ll;
            this.ur = ur;
            this.guessScore = 0;
        }

        private FoodPhotoTag(String foodName, Point ll, Point ur, int guessScore){
            mFoodName = foodName;
            this.ll = ll;
            this.ur = ur;
            this.guessScore = guessScore;
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

        public int getGuessScore() { return guessScore;  }

        public void setGuessScore(int guessScore) {  this.guessScore = guessScore;}

    }
}
