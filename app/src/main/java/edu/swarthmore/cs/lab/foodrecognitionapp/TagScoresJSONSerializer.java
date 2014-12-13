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

            gson = new Gson();
            // Build an array in JSON
            JSONArray array = new JSONArray();
            for (TagScore score : scores) {
                array.put(gson.toJson(score));
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

    }