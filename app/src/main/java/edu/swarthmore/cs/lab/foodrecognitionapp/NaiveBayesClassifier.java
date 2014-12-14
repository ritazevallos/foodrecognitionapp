package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by ritazevallos on 12/13/14.
 */
public class NaiveBayesClassifier {
    private static NaiveBayesClassifier sBayesClassifier;
    private String TAG = "NaiveBayesClassifier";
    private List<Vector<Float>> mVectorList;
    private Mat samples;
    List<Vector<Float>> mCenters;
    private Context mContext;
    private List<Double> mClusterLabels;
    private HashMap<Double, List<Vector<Double>>> mClusters;
    private HashMap<Double, String> mClusterTags;
    private HashMap<String, Integer> mTagCounts; // the total tag counts, for the priors
    private HashMap<Double, Map<String,Integer>> mClusterTagCounts;
    private NaiveBayesClassifierJSONSerializer mSerializer;
    private int numVectorsSinceLastRecomputeModel = 0;
    private int numVectorsSinceLastRecomputeClustering = 0;

    /* NaiveBayesClassifier usage
        - NaiveBayesClassifier.get(Context context);
            get the current instance of the classifier, or create one for the first time
        - String classify(List<Vector<Float>> featureVectorList);
            Pass in a list of feature vectors, each of which corresponds to a chunk of the image
            Returns a tag
        - void addToModel(List<Vector<Float>> featureVectorList, String tag);
            Performs clustering on the image, and updates mCluster tags, which has a cluster-tag entry
            for each cluster in the input image
            Also adds the vector to the vector list
        - Void recomputeModel();
            Recomputes the model but doesn't recompute clustering, so if we input a large amount of
            training data, we should call the below function.
            todo: There is a better way to do this - just add the new data to the counts
        - Void recomputeClusteringAndModel();
            Recomputes clustering and counting from all the tagged vectors that are currently stored in mVectorList
        There's also some JSON serializer things at the bottom which I haven't tested at all or thought
        very long about what needs to be stored
        I haven't tested any of this actually

        Rita 12-13-14
     */

    NaiveBayesClassifier(Context context){
        mContext = context;
        mSerializer = new NaiveBayesClassifierJSONSerializer(context, "NaiveBayesClassifier.json");
        try {
            mSerializer.loadClassifier(); // this will either load the existing classifier,
                                          // or if the file doesn't exist, compute clusters
                                          // and the hashmaps from mVectorList
        } catch (JSONException e) {
            Log.e(TAG, "Error loading classifier from serializer"+e);
            mClusters = performClustering();
        } catch (IOException e) {
            Log.e(TAG, "Error loading classifier from serializer"+e);
            mClusters = performClustering();
        }
    }

    public static NaiveBayesClassifier get(Context context) {
        if (sBayesClassifier == null) {
            sBayesClassifier = new NaiveBayesClassifier(context);
        }

        return sBayesClassifier;
    }

    public String classify(List<Vector<Float>> featureVectorList){

        HashMap<Double, Integer> clusterCounts = clusterNovelFeatureVectorList(featureVectorList);

        /*
        the "feature" in the Naive bayes would here be "put into this cluster"
        the "feature vector" is "the sections of the image were put into these clusters"

        best_tag = arg_max (over all tags) P(tag|image)
                 = arg_max (over all tags) P(tag)P(word|tag)
                 = arg_max (over all tags) P(tag) PRODUCT (over all clusters w_i in this image) w_i|tag

         P(tag) = count (clusters labeled with tag)/ count (the amount of clusters total)
         P(w_i|tag) = count (w_i, tag) / sum over all clusters w_j, count(w_j, tag)
         */

        double argmax = 0.0;
        String maxTag = "default tag";
        double smoothing_constant = 0.1;

        Iterator it = mTagCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> pairs = (Map.Entry) it.next();
            String tag = pairs.getKey();
            Integer countClustersLabeledWithThisTag = pairs.getValue();

            Double probability_this_tag = countClustersLabeledWithThisTag / (smoothing_constant + mClusters.size());
            Double probability_this_image_given_tag = 1.0;

            // iterate through all of the chunks in the image, cluster by cluster
            for (Double imageClusterLabel : clusterCounts.keySet()) {

                int count_this_cluster_this_tag = mClusterTagCounts.get(imageClusterLabel).get(tag);
                Double probability_this_cluster_given_tag = count_this_cluster_this_tag / (smoothing_constant + mTagCounts.get(tag));
                probability_this_image_given_tag *= probability_this_cluster_given_tag;

                // weight by the amount of chunks in the image that are in this cluster
                probability_this_image_given_tag *= clusterCounts.get(imageClusterLabel);
            }
            Double probability_this_tag_given_image = probability_this_tag * probability_this_image_given_tag;
            if (probability_this_tag_given_image >= argmax) {
                argmax = probability_this_tag_given_image;
                maxTag = tag;
            }

            it.remove();
        }

        return maxTag;
    }

    public void addToModel(List<Vector<Float>> featureVectorList, String tag){

        mVectorList.addAll(featureVectorList); // todo: this is not necessary and take up space; remove as sure as we know clustering is working properly

        HashMap<Double, Integer> clusterCounts = clusterNovelFeatureVectorList(featureVectorList);
        Iterator it = clusterCounts.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Double, Integer> pairs = (Map.Entry)it.next();
            Double clusterLabel = pairs.getKey();
            Integer clusterCount = pairs.getValue();
            for (int i=0; i<clusterCount; i++) {
                mClusterTags.put(clusterLabel, tag);
            }
            it.remove();
        }
        numVectorsSinceLastRecomputeModel += featureVectorList.size();
        numVectorsSinceLastRecomputeClustering += featureVectorList.size();
    }

    public void recomputeClusteringAndModel(){
        mClusters = performClustering();
        numVectorsSinceLastRecomputeClustering = 0;
        recomputeModel();
    }

    private void incrementClusterTagCountAndTagCount(Double clusterLabel, String clusterTag) {
        Integer clusterTagCount;
        Integer tagCount;
        if (mClusterTagCounts.containsKey(clusterLabel)) {
            if (mClusterTagCounts.get(clusterLabel).containsKey(clusterTag)) {
                clusterTagCount = mClusterTagCounts.get(clusterLabel).get(clusterTag) + 1;
                tagCount = mTagCounts.get(clusterTag) + 1;
            } else {
                clusterTagCount = 1;
                Map<String,Integer> tagCounts = new HashMap<String, Integer>();
                mClusterTagCounts.put(clusterLabel, tagCounts);
                tagCount = 1;
            }
        } else{
            clusterTagCount = 1;
            Map<String,Integer> tagCounts = new HashMap<String, Integer>();
            mClusterTagCounts.put(clusterLabel, tagCounts);
            tagCount = 1;
        }
        mClusterTagCounts.get(clusterLabel).put(clusterTag,clusterTagCount);
        mTagCounts.put(clusterTag, tagCount);
    }

    public void recomputeModel(){
        mClusterTagCounts = new HashMap<Double, Map<String,Integer>>();
        mTagCounts = new HashMap<String, Integer>();

        Iterator it = mClusterTags.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Double,String> pairs = (Map.Entry)it.next();
            Double clusterLabel = pairs.getKey();
            String clusterTag = pairs.getValue();

            incrementClusterTagCountAndTagCount(clusterLabel, clusterTag);

            it.remove();
        }
        numVectorsSinceLastRecomputeModel = 0;
    }

    private float euclideanDistance(Vector<Float> first, Vector<Float> second) throws Exception{
        float sum_of_squares = 0.0f;
        if (first.size() != second.size()){
            throw new Exception("Attempted to compute euclidean distance of two vectors of incompatible lengths: "+first.toString()+ "; "+second.toString());
        }
        for (int i=0; i< first.size(); i++){
            float ed = Math.abs(first.get(i)-second.get(i));
            sum_of_squares += ed*ed;
        }
        float distance = (float) Math.sqrt(sum_of_squares);
        return distance;
    }

    private Double nearestNeighbor(Vector<Float> featureVector){
        Double nearestClusterLabel = mClusterLabels.get(0);
        Float minDistance = Float.MAX_VALUE;

        for (int i=0; i<mClusterLabels.size(); i++) {
            try {
                Float distance = euclideanDistance(featureVector, mCenters.get(i));
                if (distance < minDistance){
                    minDistance = distance;
                    nearestClusterLabel = mClusterLabels.get(i);
                }
            } catch(Exception e){
                Log.d(TAG, "Error computing euclideanDistance between featureVector " + featureVector.toString() + " and cluster center "+i+":" + e);
            }
        }

        return nearestClusterLabel;
    }

    private HashMap<Double, Integer> clusterNovelFeatureVectorList(List<Vector<Float>> featureVectorList){
        HashMap<Double, Integer> clusterCounts = new HashMap<Double, Integer>();
        for (Vector<Float> featureVector : featureVectorList){
            Double clusterLabel = nearestNeighbor(featureVector);
            int clusterCount;
            if (clusterCounts.containsKey(clusterLabel)){
                clusterCount = clusterCounts.get(clusterLabel) + 1;
            } else { clusterCount = 1; }
            clusterCounts.put(clusterLabel, clusterCount);
        }

        return clusterCounts;
    }


    //region Clustering

    private HashMap<Double, List<Vector<Double>>> performClustering(){
        int width = mVectorList.get(0).size();
        int height = mVectorList.size();
        samples = new Mat(width, height, CvType.CV_32F);
        for (int i=0; i<width; i++){
            for (int j=0; j<height; j++){
                samples.put(i, j, mVectorList.get(j).get(i));
            }
        }

        Log.d(TAG, "dumping samples matrix");
        samples.dump();

        int clusterCount = 15;
        Mat labels = new Mat();
        int attempts = 5;
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER,100,0.1);

        Core.kmeans(samples, clusterCount, labels, criteria, attempts, Core.KMEANS_PP_CENTERS, centers );


        Log.d(TAG, "dumping centers matrix");
        centers.dump();
        Log.d(TAG, "dumping labels matrix");
        labels.dump();

        // labels will have one (single column) row for each feature vector (row) in samples

        mCenters = new ArrayList<Vector<Float>>() ;
        mClusterLabels = new ArrayList<Double>();

        HashMap<Double, List<Vector<Double>>> clusterDict = new HashMap<Double, List<Vector<Double>>>();
        for (int i = 0; i < labels.cols(); i++){
            Double clusterLabel = labels.get(i,0)[0]; //todo: I don't know what the [0] is for
            mClusterLabels.add(clusterLabel);

            Vector<Double> featureVector = new Vector<Double>();
            Vector<Float> centerVector = new Vector<Float>();

            for (int j=0; j<samples.cols(); j++){
                featureVector.add(samples.get(i,j)[0]); // what is the [0]? I just put it because it was returning an array
                centerVector.add((float)centers.get(i,j)[0]); // todo: I don't know if I'm iterating through centers properly
            }
            mCenters.add(centerVector);

            List<Vector<Double>> clusterVectorList;
            if (clusterDict.containsKey(clusterLabel)){
                clusterVectorList = clusterDict.get(clusterLabel);
            } else{
                clusterVectorList = new ArrayList<Vector<Double>>();
            }
            clusterVectorList.add(featureVector);
            clusterDict.put(clusterLabel,clusterVectorList);
        }

        // clusters is a list of clusters, each of which is a list of feature vectors in that cluster
        ArrayList<List<Vector<Double>>> clusters = new ArrayList<List<Vector<Double>>>(clusterDict.values());

        //instead I'm returning the dict
        return clusterDict;
    }

    public int getNumVectorsSinceLastRecomputeModel() {
        return numVectorsSinceLastRecomputeModel;
    }

    public int getNumVectorsSinceLastRecomputeClustering() {
        return numVectorsSinceLastRecomputeClustering;
    }

    // endregion

    private class NaiveBayesClassifierJSONSerializer {

        private Context mContext;
        private String mFilename;
        private Gson gson;
        private String TAG = "NaiveBayesClassifierJSONSerializer";

        public NaiveBayesClassifierJSONSerializer(Context c, String f) {
            mContext = c;
            mFilename = f;
        }

        public void saveClassifier(HashMap<Double, List<Vector<Double>>> clusters,
                                   HashMap<Double, String> clusterTags,
                                   HashMap<String, Integer> tagCounts,
                                   HashMap<Double, Map<String, Integer>> clusterTagCounts)
                throws JSONException, IOException {


            // Build an array in JSON
            JSONArray array = new JSONArray();

            gson = new Gson();
            array.put(gson.toJson(clusters));
            array.put(gson.toJson(clusterTags));
            array.put(gson.toJson(tagCounts));
            array.put(gson.toJson(clusterTagCounts));

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


        public void loadClassifier() throws IOException, JSONException {

            BufferedReader reader = null;
            try {
                // check if the file exists (i.e. we have any FoodPhotos to load)
                File file = mContext.getFileStreamPath(mFilename);
                if (file.exists()) {
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

                    JSONObject jsonObjectClusters = array.getJSONObject(0);
                    JSONObject jsonObjectClusterTags = array.getJSONObject(0);
                    JSONObject jsonObjectTagCounts = array.getJSONObject(0);
                    JSONObject jsonObjectClusterTagCounts = array.getJSONObject(0);
                    if (gson == null) {
                        gson = new Gson();
                    }

                    Type typeClusters = new TypeToken<HashMap<Double, List<Vector<Double>>>>() {
                    }.getType();
                    Type typeClusterTags = new TypeToken<HashMap<Double, String>>() {
                    }.getType();
                    Type typeTagCounts = new TypeToken<HashMap<String, Integer>>() {
                    }.getType();
                    Type typeClusterTagCounts = new TypeToken<HashMap<Double, Map<String, Integer>>>() {
                    }.getType();
                    String stringClusters = jsonObjectClusters.toString();
                    String stringClusterTags = jsonObjectClusterTags.toString();
                    String stringObjectTagCounts = jsonObjectTagCounts.toString();
                    String stringClusterTagCounts = jsonObjectClusterTagCounts.toString();
                    mClusters = gson.fromJson(stringClusters, typeClusters);
                    mClusterTags = gson.fromJson(stringClusterTags, typeClusterTags);
                    mTagCounts = gson.fromJson(stringObjectTagCounts, typeTagCounts);
                    mClusterTagCounts = gson.fromJson(stringClusterTagCounts, typeClusterTagCounts);

                } else {
                    mClusters = performClustering();
                    recomputeModel();
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "got an error!" + e);
                // We can ignore this one as it can happen when starting with no list
            } catch (Exception e) {
                Log.e(TAG, "got an error that isn't FileNotFoundException!" + e);
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }
}
