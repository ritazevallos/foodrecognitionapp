package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ritazevallos on 12/15/14.
 */
public class TagScoreStore {
    public static TagScoreStore sTagScoreStore;
    private Context mAppContext;
    private ArrayList<TagScore> mTagScores;
    private static final String TAG = "TagScoreStore";
    private TagScoresJSONSerializer mSerializer;
    HashMap<String,ArrayList<TagScore>> scoresSortedByTag;

    public TagScoreStore(Context appContext) {
        scoresSortedByTag = new HashMap<String,ArrayList<TagScore>>();
        mAppContext = appContext;
        mSerializer = new TagScoresJSONSerializer(mAppContext, "tagScores.json");
        try {
            mTagScores = mSerializer.loadTagScores();
        } catch (Exception e) {
            mTagScores = new ArrayList<TagScore>();
            Log.e(TAG, "Error loading Food Photos: ", e);

        }
    }

    public ArrayList getTagScores(){
        return mTagScores;
    }

    public static TagScoreStore get(Context c) {
        if (sTagScoreStore == null) {
            sTagScoreStore = new TagScoreStore(c.getApplicationContext());
        }
        return sTagScoreStore;
    }

    public void addTagScore(TagScore tagScore) {
        mTagScores.add(tagScore);
        ArrayList<TagScore> scoresWithThisTag = scoresSortedByTag.get(tagScore.correctTag);
        if (scoresWithThisTag == null) {
            scoresWithThisTag = new ArrayList<TagScore>();
            scoresSortedByTag.put(tagScore.correctTag, scoresWithThisTag);
        }
        scoresWithThisTag.add(tagScore);
    }

    public boolean saveTagScores() {
        try {
            mSerializer.saveTagScores(mTagScores);
            Log.d(TAG, "Tag Scores saved to file.");
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Error saving TagScores: ", e);
            return false;
        }
    }

    public void deleteAllScores(){
        boolean deleted = mSerializer.deleteJsonFile();
        Log.d(TAG, "Json file deleted = " + deleted);
        mTagScores = new ArrayList<TagScore>();
    }

    @Override
    public String toString() {
        return "Total stats: "+averageStats(mTagScores)+detailString();
    }

    public String detailString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String tag : scoresSortedByTag.keySet()){
            stringBuilder.append("\n\n"+tag+"stats:\n");
            stringBuilder.append(averageStats(scoresSortedByTag.get(tag)));
        }
        return stringBuilder.toString();
    }

    private String averageStats(ArrayList<TagScore> tagScores){
        int num_scores = 0;
        int amount_correct = 0;
        int amount_first_correct = 0;
        int total_score = 0;

        for (TagScore tagScore : tagScores){
            num_scores += 1;
            amount_correct += (tagScore.correctlyClassified) ? 1 : 0;
            amount_first_correct += (tagScore.correctlyClassifiedFirstGuess) ? 1 : 0;
            total_score += tagScore.guessesTaken;
        }

        double percent_correct = (100.0*amount_correct) / num_scores;
        double percent_first_correct = (100.0*amount_first_correct) / num_scores;
        double normalized_score = total_score / (num_scores * 1.0);

        return "\n PERCENT CORRECT: " + percent_correct +
                "\nPERCENT CORRECT ON FIRST TRY= " + percent_first_correct +
                "\nNORMALIZED SCORE = " + normalized_score;
    }
}
