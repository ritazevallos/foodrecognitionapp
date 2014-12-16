package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by ritazevallos on 12/13/14.
 */
public class TagScoresJSONSerializer {
        private Context mContext;
        private String mFilename;
        private Gson gson;
        private String TAG = "TagScoresJSONSerializer";

        public TagScoresJSONSerializer(Context c, String f) {
            mContext = c;
            mFilename = f;
        }

        public void saveTagScores(ArrayList<TagScore> scores)
                throws JSONException, IOException {

            // Build an array in JSON
            JSONArray array = new JSONArray();
            for (TagScore score : scores) {
                array.put(score.toJSON());
            }

            // Write the file to disk
            Writer writer = null;
            try {
                // todo.. i don't know if we have to load first, if its just replacing this file
                OutputStream out = mContext.openFileOutput(mFilename, Context.MODE_PRIVATE);
                writer = new OutputStreamWriter(out);
                writer.write(array.toString());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        public boolean deleteJsonFile() {
            return mContext.deleteFile(mFilename);

        }


        public ArrayList<TagScore> loadTagScores() throws IOException, JSONException {
            ArrayList<TagScore> tagScores = new ArrayList<TagScore>();
            BufferedReader reader = null;
            try {
                // check if the file exists (i.e. we have any FoodPhotos to load)
                File file = mContext.getFileStreamPath(mFilename);
                if(file.exists()) {
                    // Open and read the file into StringBuilder
                    InputStream in = mContext.openFileInput(mFilename);
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder jsonString = new StringBuilder();
                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        jsonString.append(line);
                    }
                    // Parse the JSON
                    JSONArray array = (JSONArray) new JSONTokener(jsonString.toString()).nextValue();
                    // Build the array from JSONObjects
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jsonObject = array.getJSONObject(i);
                        TagScore tagScore = new TagScore(jsonObject);
                        tagScores.add(tagScore);
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "got an error!" + e);
                // We can ignore this one as it can happen when starting with no list
            } catch (Exception e){
                Log.e(TAG, "got an error that isn't FileNotFoundException!" + e);
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
            return tagScores;
        }

    }