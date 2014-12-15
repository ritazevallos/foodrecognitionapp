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
    private Context mContext;
    private List<Vector<Float>> mVectorList;
    private HashMap<Integer, String> mVectorListTagMap;
    List<Vector<Float>> mCenters; // vectors that stand for each of the clusters; to be compared against during nearest neighbors classification
    private List<Double> mClusterLabels; // list of cluster labels; for iteration
    private HashMap<Double, List<Vector<Double>>> mClusters; // list of the vectors that make up the clusters
    private HashMap<Double, String> mClusterTags; // labeling of clusters by tags (clusters are repeated)
    private HashMap<String, Integer> mTagCounts; // the total tag counts, for the priors
    private HashMap<Double, Map<String,Integer>> mClusterTagCounts; // the tag counts for each cluster
    private NaiveBayesClassifierJSONSerializer mSerializer;
    private int numVectorsSinceLastRecomputeModel = 0;
    private int numVectorsSinceLastRecomputeClustering = 0; // in case we want to recompute every x amount of times

    /* NaiveBayesClassifier usage
        - NaiveBayesClassifier.get(Context context);
            get the current instance of the classifier, or create one for the first time

        ##### To build training model ######

        For all training instances, call
            - void addToVectorList(List<Vector<Float>> featureVectorList, String tag);
                Adds to the list and the list-tag mapping without recomputing any clustering
                Use this when we're building the original model from training data
        Then after we've gotten enough training data, call
            - Void recomputeClusteringAndModel();
                Recomputes clustering and counting from all the tagged vectors that are
                currently stored in mVectorList
        Now ready to classify using the model!

        ##### To classify a single novel feature vector ######

            - String classify(List<Vector<Float>> featureVectorList);
                Pass in a list of feature vectors, each of which corresponds to a chunk of the image
                Returns a tag

        ##### To add a single vector to the training model ######

            - void addToModel(List<Vector<Float>> featureVectorList, String tag);
                Updates mVectorList, mVectorListTagMap, mClusterTags
            - Void recomputeModel();
                Recomputes the model but doesn't recompute clustering

        ##### JSON serializer #####

        There's also some JSON serializer things at the bottom which I haven't tested at all or thought
        very long about what needs to be stored
        I haven't tested any of this actually

        Rita 12-13-14
     */

    NaiveBayesClassifier(Context context){
        mContext = context;
        mVectorListTagMap = new HashMap<Integer, String>();
        mVectorList = new ArrayList<Vector<Float>>();
        mSerializer = new NaiveBayesClassifierJSONSerializer(context, "NaiveBayesClassifier.json");
        try {
            mSerializer.loadClassifier(); // this will either load the existing classifier,
                                          // or if the file doesn't exist, compute clusters
                                          // and the hashmaps from mVectorList
        } catch (JSONException e) {
            Log.e(TAG, "Error loading classifier from serializer"+e);
            performClustering();
        } catch (IOException e) {
            Log.e(TAG, "Error loading classifier from serializer"+e);
            performClustering();
        }
    }

    public static NaiveBayesClassifier get(Context context) {
        if (sBayesClassifier == null) {
            sBayesClassifier = new NaiveBayesClassifier(context);
        }

        return sBayesClassifier;
    }

    /*
        addToVectorList - adds each feature vector into the feature vector store,
                          and associates with each the tag (stored in mVectorListTagMap)
     */
    public void addToVectorList(List<Vector<Float>> featureVectorList, String tag){
        int first_index = mVectorList.size();
        int last_index = first_index + featureVectorList.size() - 1;

        mVectorList.addAll(featureVectorList);

        for (int i=first_index; i<= last_index; i++){
            mVectorListTagMap.put(i,tag);
        }

    }

    /* addToModel - updates mVectorList, mVectorListTagMap, and mClusterTags - ready to recomputeModel

     */
    public void addToModel(List<Vector<Float>> featureVectorList, String tag){
        // update mVectorList and mVectorListTagMap
        addToVectorList(featureVectorList, tag);

        // update mClusterTags
        HashMap<Double, Integer> clusterCounts = clusterNovelFeatureVectorList(featureVectorList);
        Iterator it = clusterCounts.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Double, Integer> pairs = (Map.Entry) it.next();
            Double cluster = pairs.getKey();
            Integer countClusters = pairs.getValue();

            for (int i=0; i<countClusters; i++){
                mClusterTags.put(cluster, tag);
            }
            it.remove();
        }

    }

    public ArrayList<String> classify(List<Vector<Float>> featureVectorList){

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

        ArrayList<Double> top_three_probabilities = new ArrayList<Double>();
        ArrayList<String> top_three_tags = new ArrayList<String>();
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
            for (int i=0; i<3; i++) {
                if (i+1 > top_three_probabilities.size()){
                    top_three_probabilities.add(probability_this_tag_given_image);
                    top_three_tags.add(tag);
                }
                else {
                    double argmax = top_three_probabilities.get(i);
                    //todo: will this actually get the max three
                    if (probability_this_tag_given_image >= argmax) {
                        top_three_probabilities.set(i,probability_this_tag_given_image);
                        top_three_tags.add(tag);
                    }
                }
            }

            it.remove();
        }

        return top_three_tags;
    }

    public void recomputeClusteringAndModel(){
        performClustering();
        numVectorsSinceLastRecomputeClustering = 0;
        recomputeModel();
    }

    /*
        From changes in mClusterTags, recomputes mClusterTagCounts and mTagCounts
     */
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

    /* incrementClusterTagCountAndTagCount - for the given clusterLabel and tag,
        adds a count of 1 in mClusterTagCounts. Also adds a count of 1 in mTagCounts
        for the given tag
     */
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

    /* performClustering - performs k-means clustering on all the feature vectors in mVectorList.
        Also updates mClusterTags, which is essentially the same this as mVectorListTagMap, except
        each vector key is replaced with a cluster key

        Updated data structures:
        ArrayList<Vector<Float>> mCenters
        ArrayList<Double> mClusterLabels
        HashMap<Double, List<Vector<Double>>> mClusters
        HashMap<Double, String>() mClusterTags
     */
    private void performClustering(){
        int width = mVectorList.get(0).size();
        int height = mVectorList.size();
        Mat samples = new Mat(width, height, CvType.CV_32F);
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
        mClusterTags = new HashMap<Double, String>();

        mClusters = new HashMap<Double, List<Vector<Double>>>();
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
            if (mClusters.containsKey(clusterLabel)){
                clusterVectorList = mClusters.get(clusterLabel);
            } else{
                clusterVectorList = new ArrayList<Vector<Double>>();
            }
            clusterVectorList.add(featureVector);
            mClusters.put(clusterLabel,clusterVectorList);

            // for each feature vector, associate the cluster with a tag
            mClusterTags.put(clusterLabel, mVectorListTagMap.get(i)); // todo: make sure this is right
        }
    }

    public int getNumVectorsSinceLastRecomputeModel() {
        return numVectorsSinceLastRecomputeModel;
    }

    public int getNumVectorsSinceLastRecomputeClustering() {
        return numVectorsSinceLastRecomputeClustering;
    }

    /* this is what Android Studio's "generate" spit out... i'm dubious whether those dicts will look nice
     */
    @Override
    public String toString() {
        return "NaiveBayesClassifier{" +
                "mVectorList=" + mVectorList +
                ", mVectorListTagMap=" + mVectorListTagMap +
                ", mCenters=" + mCenters +
                ", mClusterLabels=" + mClusterLabels +
                ", mClusters=" + mClusters +
                ", mClusterTags=" + mClusterTags +
                ", mTagCounts=" + mTagCounts +
                ", mClusterTagCounts=" + mClusterTagCounts +
                ", numVectorsSinceLastRecomputeClustering=" + numVectorsSinceLastRecomputeClustering +
                ", numVectorsSinceLastRecomputeModel=" + numVectorsSinceLastRecomputeModel +
                '}';
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
                    performClustering();
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
