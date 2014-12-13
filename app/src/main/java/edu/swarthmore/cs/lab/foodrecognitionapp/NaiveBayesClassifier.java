package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by ritazevallos on 12/13/14.
 */
public class NaiveBayesClassifier {
    private String TAG = "NaiveBayesClassifier";
    private List<Vector<Float>> vectorList;
    private Mat samples;
    private Mat centers;
    private Mat labels;
    private HashMap<Double, List<Vector<Double>>> mClusters;
    private HashMap<Double, String> mClusterTags;
    private HashMap<String, Integer> mTagCounts;
    private HashMap<Double, Map<String,Integer>> mClusterTagCounts;

    NaiveBayesClassifier(List<Vector<Float>> vectorList){
        this.vectorList = vectorList;
        mClusters = performClustering();
    }

    private void computeClusterTagCounts(){
        mClusterTagCounts = new HashMap<Double, Map<String,Integer>>();
        mTagCounts = new HashMap<String, Integer>();

        Iterator it = mClusterTags.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Double,String> pairs = (Map.Entry)it.next();
            Double clusterLabel = pairs.getKey();
            String clusterTag = pairs.getValue();

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

            it.remove();
        }
    }

    public String classify(List<Vector<Float>> featureVectorList){
        addVectorAndRecomputeClusters(featureVectorList);
        HashMap<Double, List<Vector<Double>>> imageClusters = getClusters();

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
            for (Double imageClusterLabel : imageClusters.keySet()) {
                int count_this_cluster_this_tag = mClusterTagCounts.get(imageClusterLabel).get(tag);
                Double probability_this_cluster_given_tag = count_this_cluster_this_tag / (smoothing_constant + mTagCounts.get(tag));
                probability_this_image_given_tag *= probability_this_cluster_given_tag;
            }
            Double probability_this_tag_given_image = probability_this_tag * probability_this_image_given_tag;
            if (probability_this_image_given_tag >= argmax) {
                argmax = probability_this_image_given_tag;
                maxTag = tag;
            }

            it.remove();
        }

        return maxTag;
    }

    public void addToModel(List<Vector<Float>> featureVectorList, String tag){
        HashMap<Double, List<Vector<Double>>> clusters = getClusters();

        mClusterTags = new HashMap<Double, String>();
        for (Double clusterLabel : clusters.keySet()){
            mClusterTags.put(clusterLabel, tag);
        }

        computeClusterTagCounts();

    }

    //region Clustering

    public void addVectorsWithoutRecomputingClusters(List<Vector<Float>> vectors){
        vectorList.addAll(vectors);
    }

    public void addVectorAndRecomputeClusters(List<Vector<Float>> vectors){
        vectorList.addAll(vectors);
        mClusters = performClustering();
        computeClusterTagCounts();
    }

    private HashMap<Double, List<Vector<Double>>> performClustering(){
        int width = vectorList.get(0).size();
        int height = vectorList.size();
        samples = new Mat(width, height, CvType.CV_32F);
        for (int i=0; i<width; i++){
            for (int j=0; j<height; j++){
                samples.put(i,j,vectorList.get(j).get(i));
            }
        }

        Log.d(TAG, "dumping samples matrix");
        samples.dump();

        int clusterCount = 15;
        labels = new Mat();
        int attempts = 5;
        centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER,100,0.1);

        Core.kmeans(samples, clusterCount, labels, criteria, attempts, Core.KMEANS_PP_CENTERS, centers );


        Log.d(TAG, "dumping centers matrix");
        centers.dump();
        Log.d(TAG, "dumping labels matrix");
        labels.dump();

        // labels will have one (single column) row for each feature vector (row) in samples

        HashMap<Double, List<Vector<Double>>> clusterDict = new HashMap<Double, List<Vector<Double>>>();
        for (int i = 0; i < labels.cols(); i++){
            Double clusterLabel = labels.get(i,0)[0]; //todo: I don't know what the [0] is for
            Vector<Double> featureVector = new Vector<Double>();
            for (int j=0; j<samples.cols(); j++){
                featureVector.add(samples.get(i,j)[0]); // what is the [0]? I just put it because it was returning an array
            }

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

    public HashMap<Double, List<Vector<Double>>> getClusters() {
        return mClusters;
    }

    public Mat getLabels() {
        return labels;
    }

    // endregion
}
