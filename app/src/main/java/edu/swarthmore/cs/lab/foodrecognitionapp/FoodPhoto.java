package edu.swarthmore.cs.lab.foodrecognitionapp;

import java.io.File;
import java.util.Date;
import java.util.UUID;

/**
 * Created by rzevall1 on 11/3/14.
 */
public class FoodPhoto {
    private String mTag;
    private File mFile;
    private UUID mId;
    private Date mDate;

    public FoodPhoto(){
        mId = UUID.randomUUID();
        mDate = new Date();
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        mTag = tag;
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
}
