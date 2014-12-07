package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rzevall1 on 11/13/14.
 */
public class SharplesMenu {
    private static SharplesMenu sSharplesMenu;
    private ArrayList<String> breakfastMenu;
    private ArrayList<String> lunchMenu;
    private ArrayList<String> dinnerMenu;
    private ArrayList<String> alwaysMenu;

    private Date breakfastStart;
    private Date breakfastEnd;
    private Date lunchStart;
    private Date lunchEnd;
    private Date dinnerStart;
    private Date dinnerEnd;
    private Date menuDate;

    private String mFileName = "https://secure.swarthmore.edu/dash/";

    private String TAG = "edu.swarthmore.cs.lab.foodrecognitionapp.SharplesMenu";

    public SharplesMenu(Context c) {

        Document dash;
        ArrayList<ArrayList<Date>> mealHours = new ArrayList<ArrayList<Date>>();
        ArrayList<ArrayList<String>> menus = new ArrayList<ArrayList<String>>();
        menuDate = new Date();

        try {

            Log.d(TAG, "INITIALIZING ASYNC THREAD");
            Connection connection = Jsoup.connect(mFileName);
            Log.d(TAG, "AFTER CALL TO JSOUP.CONNECT");

            dash = connection.get();
            Log.d(TAG, "AFTER CALL TO CONNECTION.GET()");
            //TODO: WHY IS THIS BREAKING HERE WITHOUT EVEN AT LEAST GIVING AN ERROR MESSAGE


            Element diningDiv = dash.getElementById("dining");
            //Log.d(TAG, "AFTER GET DINING DIV: "+diningDiv.toString());
            Elements mealDivs = diningDiv.getElementsByClass("dining-meal");
            // this returns 5 meals: Sharples breakfast, lunch, dinner, Essie Mae's lunch, dinner

            for (int i = 0; i < 3; i++) { // iterating only through Sharples meals
                Element mealDiv = mealDivs.get(i);
                String hoursString = mealDiv.getElementsByTag("strong").first().nextSibling().toString();
                ArrayList<Date> startAndEndHours = parseHours(hoursString);
                mealHours.add(startAndEndHours);

                Element menuDiv = mealDiv.getElementsByClass("dining-menu").get(0);
                ArrayList<String> menuLines = new ArrayList<String>();
                for (Node child : menuDiv.childNodes()) {
                    if (child instanceof TextNode) {
                        menuLines.add(child.toString());
                    }
                }
                Log.d(TAG, "AFTER MEAL " + i + ": " + mealDiv.toString());

                ArrayList<String> menu = parseMenu(menuLines);
                menus.add(menu);
            }

            Log.d(TAG, "after for loop");

            breakfastStart = mealHours.get(0).get(0);
            breakfastEnd = mealHours.get(0).get(1);
            lunchStart = mealHours.get(1).get(0);
            lunchEnd = mealHours.get(1).get(1);
            dinnerStart = mealHours.get(2).get(0);
            dinnerEnd = mealHours.get(2).get(1);

            breakfastMenu = menus.get(0);
            lunchMenu = menus.get(1);
            dinnerMenu = menus.get(2);


            // we'd have to hand-type in the always menu (salad bar and whaterver)
            alwaysMenu = new ArrayList<String>();
            alwaysMenu.add("Salad");

            // should probably throw an error if the times overlap for some reason?

            Log.d(TAG, "end of SharplesMenu constructor");
        } catch (Exception e) {
            Log.d(TAG, "exception in constructor: " + e);
        }
    }

    public static SharplesMenu get(Context c ) {
        return get(c,false);
    }


    public static SharplesMenu get(Context c, boolean newDay ) {
        if (sSharplesMenu == null || newDay) {
            sSharplesMenu = new SharplesMenu(c.getApplicationContext());
        }
        return sSharplesMenu;
    }

    public boolean isNewDay(Date otherDate){
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return !(fmt.format(otherDate).equals(fmt.format(menuDate)));
    }

    private ArrayList<Date> parseHours(String hoursString){
        ArrayList<Date> hours = new ArrayList<Date>();

        String patternString = "[0-9][0-9]?:[0-9][0-9][ap]m";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(hoursString);

        DateFormat formatter = new SimpleDateFormat("hh:mma");

        int count = 0;
        while (matcher.find()){
            count++;
            int start_i = matcher.start();
            int end_i = matcher.end();
            String timeString = hoursString.substring(start_i,end_i);
            try {
                Date timeDate = formatter.parse(timeString);
                hours.add(timeDate);
            } catch (Exception e){
                Log.d(TAG, "parseHours: caught exception " + e);
                // todo: take care of what we return if we get any of these exceptions
            }
        }

        if (count != 2){
            //todo: error handling
            Log.d(TAG, "In parseHours - more/less than 2 times in the startend string");
        }

        return hours;
    }

    private ArrayList<String> parseMenu(ArrayList<String> menuLines){
        ArrayList<String> menu = new ArrayList<String>();

        for (String line : menuLines){
            String[] splitCommas = line.split(",");
            for (String piece : splitCommas) {
                if (piece.contains(":")){
                    String[] type_and_food = piece.split(":");
                    menu.add(type_and_food[1].trim()); // don't add the "Vegetarian: " part of it
                    //todo: strip "(vegan)" from things
                }
                else {
                    menu.add(piece.trim());
                }
            }
        }

        return menu;
    }

    public ArrayList<String> getMenu(Date rightNow){
        //todo: eventually after figuring out nutrition database interaction, don't want to just be passing around strings

        ArrayList<String> menu = new ArrayList<String>();

        if (!(rightNow.after(breakfastStart) || rightNow.after(breakfastEnd))){
            // it's breakfast right now!
            menu.addAll(breakfastMenu);
            menu.addAll(alwaysMenu);
        }
        else if (!(rightNow.after(lunchStart) || rightNow.after(lunchEnd))){
            // it's lunch right now!
            menu.addAll(lunchMenu);
            menu.addAll(alwaysMenu);
        }
        else if (!(rightNow.after(dinnerStart) || rightNow.after(dinnerEnd))){
            // it's dinner right now!
            menu.addAll(dinnerMenu);
            menu.addAll(alwaysMenu);
        }
        else {
            // take care of this case
            menu.addAll(lunchMenu); // todo: switch this back to alwaysMenu
        }

        return menu;
    }

    public ArrayList<String> getBreakfastMenu() {
        return breakfastMenu;
    }

    public void setBreakfastMenu(ArrayList<String> breakfastMenu) {
        this.breakfastMenu = breakfastMenu;
    }

    public ArrayList<String> getLunchMenu() {
        return lunchMenu;
    }

    public void setLunchMenu(ArrayList<String> lunchMenu) {
        this.lunchMenu = lunchMenu;
    }

    public ArrayList<String> getDinnerMenu() {
        return dinnerMenu;
    }

    public void setDinnerMenu(ArrayList<String> dinnerMenu) {
        this.dinnerMenu = dinnerMenu;
    }

    public ArrayList<String> getAlwaysMenu() {
        return alwaysMenu;
    }

    public void setAlwaysMenu(ArrayList<String> alwaysMenu) {
        this.alwaysMenu = alwaysMenu;
    }

    public Date getBreakfastStart() {
        return breakfastStart;
    }

    public void setBreakfastStart(Date breakfastStart) {
        this.breakfastStart = breakfastStart;
    }

    public Date getBreakfastEnd() {
        return breakfastEnd;
    }

    public void setBreakfastEnd(Date breakfastEnd) {
        this.breakfastEnd = breakfastEnd;
    }

    public Date getLunchStart() {
        return lunchStart;
    }

    public void setLunchStart(Date lunchStart) {
        this.lunchStart = lunchStart;
    }

    public Date getLunchEnd() {
        return lunchEnd;
    }

    public void setLunchEnd(Date lunchEnd) {
        this.lunchEnd = lunchEnd;
    }

    public Date getDinnerStart() {
        return dinnerStart;
    }

    public void setDinnerStart(Date dinnerStart) {
        this.dinnerStart = dinnerStart;
    }

    public Date getDinnerEnd() {
        return dinnerEnd;
    }

    public void setDinnerEnd(Date dinnerEnd) {
        this.dinnerEnd = dinnerEnd;
    }
}
