package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * Created by ritazevallos on 12/13/14.
 */
public class TagScore {
    String correctTag;
    String firstTagGuess;
    String secondTagGuess;
    String thirdTagGuess;
    boolean correctlyClassifiedFirstGuess;
    boolean correctlyClassified;
    int guessesTaken; // 1, 2, 3, 4 (did not guess)

    private static final String JSON_CORRECT = "CORRECT";
    private static final String JSON_FIRST = "FIRST";
    private static final String JSON_SECOND = "SECOND";
    private static final String JSON_THIRD = "THIRD";
    private static final String JSON_CORRECTLY_FIRST = "CORRECTLY_FIRST";
    private static final String JSON_CORRECTLY = "CORRECTLY";
    private static final String JSON_GUESSES_TAKEN = "GUESSES_TAKEN";


    // given the JSON of the foodPhoto, this is a constructor
    public TagScore(JSONObject json) throws JSONException {
        correctTag = json.getString(JSON_CORRECT);
        firstTagGuess = json.getString(JSON_FIRST);
        secondTagGuess = json.getString(JSON_SECOND);
        thirdTagGuess = json.getString(JSON_THIRD);
        correctlyClassifiedFirstGuess = json.getBoolean(JSON_CORRECTLY_FIRST);
        correctlyClassified = json.getBoolean(JSON_CORRECTLY);
        guessesTaken = json.getInt(JSON_GUESSES_TAKEN);

    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(JSON_CORRECT, correctTag);
        json.put(JSON_FIRST, firstTagGuess);
        json.put(JSON_SECOND,secondTagGuess);
        json.put(JSON_THIRD, thirdTagGuess);
        json.put(JSON_CORRECTLY_FIRST, correctlyClassifiedFirstGuess);
        json.put(JSON_CORRECTLY, correctlyClassified);
        json.put(JSON_GUESSES_TAKEN, guessesTaken);

        return json;
    }

    TagScore(String correct, String first, String second, String third) {
        this.correctTag = correct;
        this.firstTagGuess = first;
        this.secondTagGuess = second;
        this.thirdTagGuess = third;
        this.correctlyClassifiedFirstGuess = false;
        this.correctlyClassified = false;
        if (correct == first){
            this.correctlyClassifiedFirstGuess = true;
            this.correctlyClassified = true;
            this.guessesTaken = 1;
        } else if (correct == second){
            this.correctlyClassified = true;
            this.guessesTaken = 2;
        } else if (correct == third){
            this.correctlyClassified = true;
            this.guessesTaken = 3;
        } else {
            this.guessesTaken = 4;
        }
    }

    public String getCorrectTag() {
        return correctTag;
    }

    public String getFirstTagGuess() {
        return firstTagGuess;
    }

    public String getSecondTagGuess() {
        return secondTagGuess;
    }

    public String getThirdTagGuess() {
        return thirdTagGuess;
    }

    public boolean isCorrectlyClassifiedFirstGuess() {
        return correctlyClassifiedFirstGuess;
    }

    public boolean isCorrectlyClassified() {
        return correctlyClassified;
    }

    public int getGuessesTaken() {
        return guessesTaken;
    }

    @Override
    public String toString() {
        return "TagScore{" +
                "correctTag='" + correctTag + '\'' +
                ", firstTagGuess='" + firstTagGuess + '\'' +
                ", secondTagGuess='" + secondTagGuess + '\'' +
                ", thirdTagGuess='" + thirdTagGuess + '\'' +
                ", correctlyClassifiedFirstGuess=" + correctlyClassifiedFirstGuess +
                ", correctlyClassified=" + correctlyClassified +
                ", guessesTaken=" + guessesTaken +
                '}';
    }
}