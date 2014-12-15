package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

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