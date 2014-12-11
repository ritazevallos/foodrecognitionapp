package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by jschwar1 on 12/11/14.
 */
public class MenuFood {

    private String mFoodName;
    private ArrayList<Integer> mColor1;
    private ArrayList<Integer> mColor2;


    public MenuFood(String foodName, ArrayList<Integer> color1, ArrayList<Integer> color2){
        mFoodName = foodName;
        mColor1 = color1;
        mColor2 = color2;
    }

    public MenuFood(String foodName){
        mFoodName = foodName;
        mColor1 = new ArrayList<Integer>();
        mColor2 = new ArrayList<Integer>();
    }


    public String getFoodName() { return mFoodName; }

    public void setFoodName(String foodName) { mFoodName = foodName; }

    public ArrayList<Integer> getColor1() { return mColor1;  }

    public void setColor1(ArrayList<Integer> color1) {  mColor1 = color1;  }

    public void setColor1(int R, int G, int B){
        ArrayList<Integer> color1 = new ArrayList<Integer>();
        color1.add(R);
        color1.add(G);
        color1.add(B);
        mColor1 = color1;

    }

    public ArrayList<Integer> getColor2() { return mColor2;  }

    public void setColor2(ArrayList<Integer> color2) {  mColor2 = color2;   }

    public void setColor2(int R, int G, int B){
        ArrayList<Integer> color2 = new ArrayList<Integer>();
        color2.add(R);
        color2.add(G);
        color2.add(B);
        mColor1 = color2;

    }


}
