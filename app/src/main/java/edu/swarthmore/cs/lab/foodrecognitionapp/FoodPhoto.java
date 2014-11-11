package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by rzevall1 on 11/3/14.
 */
public class FoodPhoto {
    private ArrayList<String> mTags;
    private File mFile;
    private UUID mId;
    private Date mDate;

    private static final String JSON_ID = "id";
    private static final String JSON_TAGS = "tags";
    private static final String JSON_DATE = "date";
    private static final String JSON_FILE_URI = "file";

    public FoodPhoto(){
        mId = UUID.randomUUID();
        mDate = new Date();
        mTags = new ArrayList<String>();
    }

    public ArrayList<String> getTags() {
        return mTags;
    }

    public void setTags(ArrayList<String> tags) {
        mTags = tags;
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
        json.put(JSON_TAGS, mTags.get(0)); //TODO: just putting the first string
        json.put(JSON_DATE, mDate.getTime());
        json.put(JSON_FILE_URI, Uri.fromFile(mFile).toString());//todo: no idea if this is the right way to do this
        return json;
    }

    // given the JSON of the foodPhoto, this is a constructor
    public FoodPhoto(JSONObject json) throws JSONException {
        mId = UUID.fromString(json.getString(JSON_ID));
        mTags = new ArrayList<String>();
        String string_tags = json.getString(JSON_TAGS);
        mTags.add(string_tags);
        mDate = new Date(json.getLong(JSON_DATE));
        mFile = new File(json.getString(JSON_FILE_URI));

    }
}
