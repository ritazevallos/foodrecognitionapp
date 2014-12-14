package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

public class PictureTakerFragment extends Fragment{

    //region Variable initialization, onCreate, newInstance

    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "PictureTakerFragment";
    public static File mFile;
    List <MatOfPoint> mContours;
    public static File mDir;
    public static Bitmap bitmap;
    public static Bitmap mapbit;
    public static ImageView mImageView;
    private AutoCompleteTextView mTagField;
    private ArrayList<AutoCompleteTextView> mTagFields;
    private ArrayList<MenuFood> mLunchFoods;
    private ArrayList<MenuFood> mDinnerFoods;
    private ArrayList<ArrayList<Integer>> mLunchColors;
    private ArrayList<ArrayList<Integer>> mDinnerColors;
    private FoodPhoto mFoodPhoto;
    private FoodPhotoStore mFoodPhotoStore;
    private NaiveBayesClassifier mBayesClassifier;
    private FoodPhoto.FoodPhotoTag mFoodPhotoTag;
    private Button retakePhotoButton;
    public boolean beforePhotoTaken;
    private SharplesMenu mSharplesMenu;
    private boolean menuIsLoaded = false;
    private LinearLayout mSegmentsContainer;
    public static final String EXTRA_FOODPHOTO_ID =
            "edu.swarthmore.cs.lab.foodrecognitionapp.foodphoto_id";
    private ArrayList<ImageView> mSegmentImageViews;
    private boolean taggingUnSegmentedBit = false;
    private ArrayList<Rect> ROIs;
    private TextView mNumberOfSegmentsTextView;
    private LinearLayout mTagSuggestionsLayout;
    private LinearLayout mNotFoodLayout;
    private RelativeLayout mTagContainer;
    private Boolean isFood;
    private boolean canContinue = false;
    private int count;
    private float guessScore = 0;
    private String inputTag;
    private ArrayList<Mat> segmentMats;
    private ArrayList<TagScore> mScores;
    private TagScoresJSONSerializer mSerializer;

    // SOME DEVELOPER SETTINGS
    private boolean viewMaskAndMaskedImage = false;
    private boolean viewSegments = true;
    private boolean using_emulator = false; // make this true if you don't want it to break when opening up camera
    private boolean loggingTrainingData = true;
    private boolean recomputeModelIfNeeded = true;
    private boolean recomputeClusteringAndModelIfNeeded = true;
    private int maxInputBeforeRecomputingModel = 5;
    private int maxInputBeforeRecomputingClusteringAndModel = 15;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // todo: we are not making sure there is an app to take pictures
        setHasOptionsMenu(true);
        beforePhotoTaken = true;
        mSerializer = new TagScoresJSONSerializer(getActivity(), "tagScores.json");
        //store these views for accessing later
        mTagFields = new ArrayList<AutoCompleteTextView>();
        mSegmentImageViews = new ArrayList<ImageView>();
        mScores = new ArrayList<TagScore>();

        // Either create a new food photo, or get the one with the id added to the intent
        CreateDirectoryForPictures();
        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        UUID foodPhotoId = (UUID)getArguments().getSerializable(EXTRA_FOODPHOTO_ID);
        mFoodPhoto = mFoodPhotoStore.getFoodPhoto(foodPhotoId);

        //Populate mSharplesMenu, populates FOOD_GUESSES, and sets menuIsLoaded to true once done
        AsyncSharplesGetter dashScraper = new AsyncSharplesGetter();
        dashScraper.execute("go!");

        if (mFoodPhoto.getFile() != null){
            // load image from file
            Uri contentUri = Uri.fromFile(new File(mFoodPhoto.cleanedFilePath()));
            loadBitmapFromUri(contentUri);
        }
        else if (!using_emulator) {
            TakeAPicture(); // launches camera intent
        }
    }

    public static PictureTakerFragment newInstance(UUID healthID) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FOODPHOTO_ID, healthID);

        PictureTakerFragment fragment = new PictureTakerFragment();
        fragment.setArguments(args);

        return fragment;
    }

    //endregion

    //region Data tracking

    private void saveTrainingData(){
        for (int i=0; i<segmentMats.size(); i++){
            Mat segment = segmentMats.get(i);
            String tag = mFoodPhoto.getTags().get(i).getFoodName();

            Vector feature_vector = new Vector();

            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stddev = new MatOfDouble();
            Core.meanStdDev(segment, mean, stddev);
            double[] meanArray = mean.toArray();
            double[] stdDevArray = stddev.toArray();

            feature_vector.add(meanArray[0]);
            feature_vector.add(meanArray[1]);
            feature_vector.add(meanArray[2]);
            feature_vector.add(stdDevArray[0]);
            feature_vector.add(stdDevArray[1]);
            feature_vector.add(stdDevArray[2]);

            // stick the feature vector into the Naive Bayes

            // either gets the current classifier or makes a new one
            mBayesClassifier = NaiveBayesClassifier.get(getActivity());
            mBayesClassifier.addToModel(feature_vector, tag);
            if (recomputeClusteringAndModelIfNeeded && mBayesClassifier.getNumVectorsSinceLastRecomputeClustering() > maxInputBeforeRecomputingClusteringAndModel){
                Log.d(TAG, "RECOMPUTING NAIVE BAYES CLUSTERING AND MODEL");
                mBayesClassifier.recomputeClusteringAndModel();
            } else if (recomputeModelIfNeeded && mBayesClassifier.getNumVectorsSinceLastRecomputeModel() > maxInputBeforeRecomputingModel) {
                Log.d(TAG, "RECOMPUTING NAIVE BAYES MODEL");
                mBayesClassifier.recomputeModel();
            }

            // todo: should probably also save the image file in case we want to extract more features later, so save the file and store the uri somewhere


        }
    }

    private void saveAccuracyData(){
        //todo, using mScores
        try {
            mSerializer.saveTagScores(mScores);
        } catch (JSONException e){
            Log.d(TAG, "Error in saveAccuracyData calling mSerializer.saveTagScores()"+e);
        } catch (IOException e){
            Log.d(TAG, "Error in saveAccuracyData calling mSerializer.saveTagScores()"+e);
        }
    }

    //endregion

    //region Initializing the the menu and UI, including listeners for buttons

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        final View v = inflater.inflate(R.layout.activity_picture_taker, parent, false);

        mSegmentsContainer = (LinearLayout)v.findViewById(R.id.segmentsContainerLayout);
        if (!viewSegments) {
            mSegmentsContainer.setVisibility(View.GONE);
        }

        mTagSuggestionsLayout = (LinearLayout)v.findViewById(R.id.tag_buttons_layout);
        mNotFoodLayout = (LinearLayout)v.findViewById(R.id.notFoodLayout);
        mTagContainer = (RelativeLayout)v.findViewById(R.id.tagContainerLayout);

        retakePhotoButton = (Button)v.findViewById(R.id.retake_photo_button);
        if(beforePhotoTaken) {
            retakePhotoButton.setVisibility(View.GONE);
        }
        retakePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TakeAPicture();
            }
        });

        Button loadPhoto = (Button)v.findViewById(R.id.loadPhoto);
        loadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoadPicture();
            }
        });

        Button saveButton = (Button)v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "tag says: " + mFoodPhoto.getTags());
                if (mFoodPhoto.getTags().isEmpty()) {
                    showToast("Remember to tag your photo");
                }
                else if (mFoodPhoto.getTags().size() < ROIs.size()){
                    int tags_left = ROIs.size()-mFoodPhoto.getTags().size();
                    showToast("You still have "+ tags_left + "segments left to tag!");
                }
                else{
                    mFoodPhotoStore.addFoodPhoto(mFoodPhoto);
                    FoodPhotoStore.get(getActivity()).saveFoodPhotos();
                    if (loggingTrainingData) {
                        saveTrainingData();
                    }
                    saveAccuracyData();
                    Log.d(TAG, "Average guess score: " + String.valueOf(guessScore/mFoodPhoto.getTags().size()));
                    openNutrition();
                }
            }
        });

        mImageView = (ImageView)v.findViewById(R.id.imageView1);
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
            bitmap = null;
        }
        mImageView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                // at the moment, taggingUnSegmentedBit is always false. We may change
                // this if we want to allow users to tag unsegmented bits
                if ((taggingUnSegmentedBit) && (event.getAction() == MotionEvent.ACTION_DOWN)) {
                    Log.d(TAG, "mImageView.onTouchListener");
                    Point ll = new Point(event.getX() - 20, event.getY() - 20);
                    Point ur = new Point(event.getX() + 20, event.getY() + 20);
                    //todo:

                    retakePhotoButton.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });

        mNumberOfSegmentsTextView = (TextView)v.findViewById(R.id.number_of_segments);

        return v;

    }

    private void showToast(String text){
        Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    //endregion

    //region Tagging UI things

    private void commenceTagging(){

        displayTaggingUI();
    }

    private void displayTaggingUI(){
        //todo: if ROIs is empty, error out
        mNumberOfSegmentsTextView.setText("Found "+ROIs.size()+" foods on your plate!");

        count = 0;
        tagSegment(0);
        count++;

        Button nextSegmentButton = (Button) mNotFoodLayout.findViewById(R.id.next_segment);
//
//        // changed this listener from nextSegmentButton to the three guessButtons
//        final Button firstButtonSuggestion = (Button) mTagSuggestionsLayout.findViewById(R.id.first_tag_suggestion);
//        final Button secondButtonSuggestion = (Button) mTagSuggestionsLayout.findViewById(R.id.second_tag_suggestion);
//        final Button thirdButtonSuggestion = (Button) mTagSuggestionsLayout.findViewById(R.id.third_tag_suggestion);

        ArrayList<Button> buttons = new ArrayList<Button>();
//        buttons.add(firstButtonSuggestion);
//        buttons.add(secondButtonSuggestion);
//        buttons.add(thirdButtonSuggestion);
        buttons.add(nextSegmentButton);
        for (Button button: buttons){
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToNextSegment();
                }
            });
        }
    }

    public void goToNextSegment(){
        if(canContinue) {
            if(count<ROIs.size()){
                tagSegment(count);
                mNumberOfSegmentsTextView.setText(ROIs.size()-count + " foods left to tag!");
                count++;
            } else {
                showToast("All segments have been tagged");
                canContinue = true;
            }

            canContinue = false;
        } else {
            showToast("Tag this segment, or hit 'Not Food'");
        }
    }

    public void highlightCurrentROI(Point ll, Point ur, int segmentNum){
        Bitmap.Config config = bitmap.getConfig();
        mapbit = bitmap.copy(config, true);
        Mat imageMat = new Mat(mapbit.getWidth(), mapbit.getHeight(), CvType.CV_8UC4); // fc means floating point matrices
        Utils.bitmapToMat(mapbit, imageMat);
        // highlight the segment we're talking about with a rectangle
        Scalar colorLine = new Scalar(0, 255, 0, 255);

        //commented out to do contour instead of rectangle
        //Core.rectangle(imageMat, ll, ur, colorLine, 15);

        Imgproc.drawContours(imageMat, mContours, segmentNum, colorLine, 15);
        Utils.matToBitmap(imageMat, mapbit);

        mImageView.setImageBitmap(mapbit);
    }

    public void tagSegment(int segmentNum){

        Rect rect = ROIs.get(segmentNum);
        final Point ll = new Point(rect.x, rect.y);
        final Point ur = new Point(rect.x + rect.width, rect.y + rect.height);

        //todo: use the submat (with the area outside the contour zeroed out) for the classification)
        //Mat subMat = imageMat.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);

        isFood = true;

        highlightCurrentROI(ll,ur, segmentNum);

        final ArrayList<String> suggestions = getTagSuggestions(1, 2, 3);

        final ArrayList<Button> suggestionButtons = new ArrayList<Button>(){{
            add((Button) mTagSuggestionsLayout.findViewById(R.id.first_tag_suggestion));
            add((Button) mTagSuggestionsLayout.findViewById(R.id.second_tag_suggestion));
            add((Button) mTagSuggestionsLayout.findViewById(R.id.third_tag_suggestion));
        }};
        mTagField = (AutoCompleteTextView)mTagContainer.findViewById(R.id.pictureTag);
        mTagField.setEnabled(true);

        for (int index=0; index<suggestionButtons.size(); index++){
            final int i = index;
            Button button = suggestionButtons.get(i);
            button.setText(suggestions.get(i));
            button.setEnabled(true);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String selectedTag = suggestions.get(i);
                    TagScore tagScore = new TagScore(selectedTag, suggestions.get(0), suggestions.get(1), suggestions.get(2));
                    mScores.add(tagScore);

                    mFoodPhoto.setOneTag(selectedTag, ll, ur, i+1);
                    canContinue = true;
                    guessScore+= i+1;
                    for (int j=0; j<suggestionButtons.size(); j++){
                        if (j!=i){
                            suggestionButtons.get(i).setEnabled(false);
                        }
                    }
                    mTagField.setEnabled(false);
                }
            });
        }

        Button notFoodButton = (Button) mNotFoodLayout.findViewById(R.id.not_food_button);
        notFoodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFood = false;
                canContinue = true;
            }
        });

        mTagField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String selectedTag = s.toString();

                TagScore tagScore = new TagScore(selectedTag, suggestions.get(0),suggestions.get(1),suggestions.get(2));
                mScores.add(tagScore);

                inputTag = s.toString();
                for (Button button: suggestionButtons) {
                    button.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }


        });

        Button doneButton = (Button) mTagContainer.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inputTag != null) {
                    mFoodPhoto.setOneTag(inputTag, ll, ur, 4);
                    canContinue = true;
                    guessScore += 4;
                    inputTag = null;
                    showToast("Custom tag added");
                } else {
                    showToast("Type in your custom tag");

                }
            }
        });


        if (!isFood) {
            ROIs.remove(segmentNum);
        }


    }

    //endregion

    //region Getting the picture from the camera or gallery, including all the file IO

    private void CreateDirectoryForPictures()
    {
        mDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "BOOKS");
        if (!mDir.exists())
        {
            mDir.mkdirs();
        }
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Food");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Food", "failed to create directory");
                return null;
            } else {
                Log.d("Food", "successfully created directory");
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void LoadPicture(){

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    private void TakeAPicture(){

        Intent intent = new Intent(getActivity(), CameraActivity.class);

        mFile = new File(mDir, String.format("foodPhoto_"+ UUID.randomUUID() + ".jpg"));
        Log.d(TAG, "file path: " + mFile.toString());
        mFoodPhoto.setFile(mFile);

        intent.putExtra(CameraActivity.EXTRA_URI, Uri.fromFile(mFile).toString());

        beforePhotoTaken = false;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "in onActivityResult");

        super.onActivityResult(requestCode,resultCode,data);

        Uri contentUri;
        if(data == null){
            contentUri = Uri.fromFile(mFile);
        } else {
            contentUri = data.getData();
        }

        loadBitmapFromUri(contentUri);

        cropPhoto();

        mImageView.setImageBitmap(bitmap);
        retakePhotoButton.setVisibility(View.VISIBLE);

        segmentImage();

        cropSegmentsToContours();

        commenceTagging();

    }


    private void loadBitmapFromUri(Uri uri){
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    //endregion

    // region Classification

    public void cropSegmentsToContours(){

        ArrayList<Mat> croppedSegmentMats = new ArrayList<Mat>();

        for (int i=0; i<segmentMats.size(); i++){
            Mat segmentMat = segmentMats.get(i);

            Mat maskedImg = new Mat(segmentMat.size(), segmentMat.type());
            maskedImg.setTo(new Scalar(102,51,153));

            Mat mask = new Mat(segmentMat.size(), segmentMat.type());
            Imgproc.cvtColor(segmentMat, mask, Imgproc.COLOR_RGBA2GRAY, 4);
            mask.setTo(new Scalar(0, 0, 0));
            Imgproc.drawContours(mask, mContours, i, new Scalar(255, 255, 255), -200);
            segmentMat.copyTo(maskedImg, mask);

            Bitmap maskedImgBitmap = Bitmap.createBitmap(maskedImg.cols(), maskedImg.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(maskedImg, maskedImgBitmap);
            croppedSegmentMats.add(maskedImg);
            ImageView maskedImgView = new ImageView(getActivity());
            maskedImgView.setImageBitmap(maskedImgBitmap);
            mSegmentsContainer.addView(maskedImgView);

            // todo: store the cropped image in a file ex: //Highgui.imwrite(mFile.toString(),submat);

        }

        segmentMats = croppedSegmentMats;
    }

    public ArrayList<String> getTagSuggestions(int R, int G, int B){
        // todo: get suggestions from classifier, given foodMat
        ArrayList<String> suggestions = new ArrayList<String>();
        //mLunchFoods = new ArrayList<MenuFood>();
        //mDinnerFoods = new ArrayList<MenuFood>();
        ArrayList<String> foodItems = new ArrayList<String>();//mSharplesMenu.getMenu(new Date());
        //parseLunchMenu();
//        parseDinnerMenu();
//        ArrayList<Integer> comp = new ArrayList<Integer>(mDinnerFoods.size()*2);
//
//        for(int i =0; i<mDinnerFoods.size(); i++){
//            MenuFood food = mDinnerFoods.get(i);
//            ArrayList<Integer> color1 = food.getColor1();
//            ArrayList<Integer> color2 = food.getColor2();
//            int compNum = Math.abs(R-color1.get(0)) + Math.abs(G - color1.get(1)) + Math.abs(B-color1.get(2));
//            int compNum2 = Math.abs(R-color2.get(0)) + Math.abs(G-color2.get(1)) + Math.abs(B-color2.get(2));
//            comp.add(compNum);
//            comp.add(compNum2);
//        }
//
//        ArrayList<Integer> smallestComps = new ArrayList<Integer>(Arrays.asList(1000,1000,1000));
//        ArrayList<Integer> indices = new ArrayList<Integer>(Arrays.asList(0,0,0));
//        for(int j = 0; j<comp.size();j++){
//
//            if(comp.get(j)<smallestComps.get(0)){
//                smallestComps.set(2, smallestComps.get(1));
//                smallestComps.set(1, smallestComps.get(0));
//                smallestComps.set(0, comp.get(j));
//                indices.set(2, indices.get(1));
//                indices.set(1, indices.get(0));
//                indices.set(0, j);
//            } else if (comp.get(j)<smallestComps.get(1)){
//                smallestComps.set(2, smallestComps.get(1));
//                smallestComps.set(1, comp.get(j));
//                indices.set(2, indices.get(1));
//                indices.set(1, j);
//            } else if (comp.get(j)<smallestComps.get(2)){
//                smallestComps.set(2, comp.get(j));
//                indices.set(2, j);
//            }
//        }
//        for(int k = 0; k<3; k++){
//            int index = (indices.get(k)/2);
//            suggestions.add(mDinnerFoods.get(index).getFoodName());
//        }

        foodItems.add("Vegetable Medley");
        foodItems.add("Cheese Ravioli with Marinara");
        foodItems.add("Falafel Bar with Hummus");

//        //todo: if we don't do machine learning in time, get a random three from this meal period from sharples menu
        while(suggestions.size()<3) {
            String newSug = foodItems.get(randInt(0, foodItems.size() - 1));
            //guess food here

            if(suggestions.contains(newSug)){
                continue;
            }
            suggestions.add(newSug);
        }

        return suggestions;
    }

//    public void parseLunchMenu(){
//        int colorCount = 0;
//        parseLunchColors();
//        ArrayList<String> foodItems = mSharplesMenu.getMenu(new Date());
//        for(int i = 0; i<foodItems.size(); i++){
//            MenuFood food = new MenuFood(foodItems.get(i), mLunchColors.get(colorCount), mLunchColors.get(colorCount+1));
//            mLunchFoods.add(food);
//            colorCount+=2;
//        }
//    }
//
//    public void parseDinnerMenu(){
//        int colorCount = 0;
//        parseDinnerColors();
//        //ArrayList<String> foodItems = mSharplesMenu.getMenu(new Date());
//        ArrayList<String> foodItems = mSharplesMenu.getDinnerMenu();
//        for(int i = 0; i<foodItems.size(); i++){
//            String foodName = foodItems.get(i);
//            ArrayList<Integer> color1 = mDinnerColors.get(colorCount);
//            ArrayList<Integer> color2 = mDinnerColors.get(colorCount+1);
//            MenuFood food = new MenuFood(foodItems.get(i), mDinnerColors.get(colorCount), mDinnerColors.get(colorCount+1));
//            mDinnerFoods.add(food);
//            colorCount+=2;
//        }
//    }
//
//    public void parseLunchColors(){
//        mLunchColors = new ArrayList<ArrayList<Integer>>();
//        ArrayList<Integer> color = new ArrayList<Integer>(Arrays.asList(175,113,55));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(183,110,38));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(198,109,33));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(193,115,29));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(222,75,28));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(205,78,25));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(147,117,50));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(135,94,24));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(187,166,97));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(173,149,80));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(224,169,107));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(208,153,87));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(175,127,67));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(202,91,47));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(157,94,29));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(146,93,19));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(131,56,4));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(175,94,43));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(213,209,176));
//        mLunchColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(224,191,156));
//        mLunchColors.add(color);
//
//    }
//
//    public void parseDinnerColors(){
//        mDinnerColors = new ArrayList<ArrayList<Integer>>();
//
//        ArrayList<Integer> color = new ArrayList<Integer>(Arrays.asList(92,63,39));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(96,67,65));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(128,89,79));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(154,92,44));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(86,121,43));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(105,92,64));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(210,144,27));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(201,133,60));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(88,55,41));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(90,59,34));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(121,90,21));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(120,93,48));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(93,60,25));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(121,72,15));
//        mDinnerColors.add(color);
//
//        color = new ArrayList<Integer>(Arrays.asList(247,215,109));
//        mDinnerColors.add(color);
//        color = new ArrayList<Integer>(Arrays.asList(130,78,63));
//        mDinnerColors.add(color);
//    }

    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    //endregion

    //region Segmentation

    public void cropPhoto(){
        Mat imageMat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4); // fc means floating point matrices
        Utils.bitmapToMat(bitmap, imageMat);

        Mat maskedImg = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4);
        maskedImg.setTo(new Scalar(102,51,153));

        Mat mask = new Mat(imageMat.size(), imageMat.type());
        Imgproc.cvtColor(imageMat, mask, Imgproc.COLOR_RGBA2GRAY, 4);

        mask.setTo(new Scalar(0, 0, 0));

        //Draw ellipse
        int plateRadius = 800;
        Point center = new Point(bitmap.getWidth()/2, bitmap.getHeight()/2);
        Rect rect = new Rect(new Point(center.x - plateRadius, center.y - plateRadius), new Point(center.x + plateRadius, center.y + plateRadius));
        Core.circle(mask, center, plateRadius, new Scalar(255,255,255), -200);

        imageMat.copyTo(maskedImg, mask);
        Mat submat = maskedImg.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
        bitmap = Bitmap.createBitmap(submat.cols(), submat.rows(), Bitmap.Config.ARGB_8888);
//        Mat newmat = new Mat(submat.size(), submat.type());
//        Imgproc.cvtColor(submat, newmat, Imgproc.COLOR_RGBA2BGRA, 4);
        Utils.matToBitmap(submat, bitmap);


        // store the cropped image in mFile

        Highgui.imwrite(mFile.toString(),submat);

    }

    public void segmentImage(){
            Mat imageMat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4); // fc means floating point matrices

            Utils.bitmapToMat(bitmap, imageMat);
            Mat mask = new Mat(imageMat.size(), imageMat.type());

            Imgproc.cvtColor(imageMat, mask, Imgproc.COLOR_RGBA2GRAY, 4); // or should we use COLOR_BayerRG2GRAY? COLOR_BGR2GRAY
            //mask.create(mask.rows(), mask.cols(), CvType.CV_8UC4);
            Imgproc.GaussianBlur(mask, mask, new Size(3, 3), 0);
            Imgproc.threshold(mask, mask, 0, 255, Imgproc.THRESH_OTSU);

            if (viewMaskAndMaskedImage) {
                Bitmap maskBitmap = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mask, maskBitmap);
                ImageView maskView = new ImageView(getActivity());
                maskView.setImageBitmap(maskBitmap);
                TextView maskText = new TextView(getActivity());
                maskText.setText("mask");
                mSegmentsContainer.addView(maskText);
                mSegmentsContainer.addView(maskView);
            }

            Mat maskedImg = new Mat(imageMat.size(), imageMat.type());
            maskedImg.setTo(new Scalar(0,0,0));
            imageMat.copyTo(maskedImg, mask);

            if (viewMaskAndMaskedImage) {
                Bitmap maskedImgBitmap = Bitmap.createBitmap(maskedImg.cols(), maskedImg.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(maskedImg, maskedImgBitmap);
                ImageView maskedImgView = new ImageView(getActivity());
                maskedImgView.setImageBitmap(maskedImgBitmap);
                TextView maskedText = new TextView(getActivity());
                maskedText.setText("masked");
                mSegmentsContainer.addView(maskedText);
                mSegmentsContainer.addView(maskedImgView);
            }

            List <MatOfPoint> contours = new ArrayList<MatOfPoint>();
            ROIs = new ArrayList<Rect>();
            segmentMats = new ArrayList<Mat>();

            Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            Scalar colorLine = new Scalar(180, 37, 216, 255); // PURPLE the last value is necessary so that it's not transparent

            int minSize = 500; // we can figure out what this should be using the tray size: they should position the camera
            //so's the tray fills the camera screen

            mContours = new ArrayList<MatOfPoint>();
            List <Point> centers = new ArrayList<Point>();

            for (int i = 0; i < contours.size(); i++) {
                // draw contour in green
                if (Imgproc.contourArea(contours.get(i)) > minSize) {
                    Rect rect = Imgproc.boundingRect(contours.get(i));
                    if(rect.width>250||rect.height>250) {
                        mContours.add(contours.get(i));
                        ROIs.add(rect);

                        Mat subMat = imageMat.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
                        segmentMats.add(subMat);

                        ImageView segmentImageView = new ImageView(getActivity());

                        Bitmap segmentBitmap = Bitmap.createBitmap(subMat.cols(), subMat.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(subMat, segmentBitmap);
                        segmentImageView.setImageBitmap(segmentBitmap);
                    }
                }
            }

            // the image with contours
            Mat imageWithContoursAndRectangles = new Mat(imageMat.size(), imageMat.type());
            imageMat.copyTo(imageWithContoursAndRectangles);

            // draw contours and rectangles
            for (int i=0; i< mContours.size(); i++){
                // switch around the commented section to draw contours instead of rectangles
                Imgproc.drawContours ( imageWithContoursAndRectangles, mContours, i, colorLine, 15);
                Rect rect = ROIs.get(i);
                //Core.rectangle(imageWithContoursAndRectangles, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), colorLine, 15);
            }

            bitmap = Bitmap.createBitmap(imageWithContoursAndRectangles.cols(), imageWithContoursAndRectangles.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(imageWithContoursAndRectangles, bitmap);
            mImageView.setImageBitmap(bitmap);

    }

    //endregion

    //region The App Options Menu

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.picture_taker, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.menu_item_gallery) {
            Log.d(TAG, "Go to gallery (from picture taker)");
            openGallery();
        }
        if(item.getItemId() == R.id.menu_item_add) {
            Log.d(TAG, "Add new photo (from picture taker)");
            showToast("Save your current photo first!");

        }
        if(item.getItemId() == R.id.menu_item_main_menu) {
            Log.d(TAG, "Go to main menu (from picture taker)");
            openMainMenu();
        }
        if(item.getItemId() == R.id.menu_item_deleteAll) {
            Log.d(TAG, "Delete all (from picture taker)");
            deletePhotosAfterConfirmation();

        }

        return true;
    }

    private void deletePhotosAfterConfirmation(){
        //todo: this alert isn't appearing
        new AlertDialog.Builder(getActivity())
                .setTitle("Delete all photos")
                .setMessage("Do you really want to delete all the photos?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        mFoodPhotoStore.deleteAllPhotos();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    public void openGallery(){
        Intent i = new Intent(getActivity(), FoodPhotoListActivity.class);
        startActivity(i);
        //getActivity().finish();
    }

    public void openNutrition(){
        //ideally this will do all the nutrition api stuff...
        Intent i = new Intent(getActivity(), NutritionActivity.class);
        startActivity(i);
    }

    public void openMainMenu(){
        Intent i = new Intent(getActivity(), MainMenuActivity.class);
        startActivity(i);
        //getActivity().finish();
    }

    //endregion

    //region SharplesMenu asynchronous loader

    private class AsyncSharplesGetter extends AsyncTask<String, Integer, String> {
        //todo: what are the string, int, string in the constructor?

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "in AsyncSharplesGetter.onPreExecute()");
            super.onPreExecute();
            // todo: show progress bar
        }

        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, "in AsyncSharplesGetter.doInBackground()");
            // this will get the existing sharples menu, or create one if it doesn't exist
            mSharplesMenu = SharplesMenu.get(getActivity());
            if (mSharplesMenu.isNewDay(new Date())){
                mSharplesMenu = SharplesMenu.get(getActivity(), true);
                // todo: i haven't tested whether the new day thing works
            }


            return "All done!";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // update progress bar
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG, "in onPostExecute");
            if (mSharplesMenu.isLoaded()) {
                menuIsLoaded = true;
                mTagFields.add(mTagField);
                attachGuessesToTagFields(mTagFields);
            }

        }
    }

    private void attachGuessesToTagFields(ArrayList<AutoCompleteTextView> tagFields){
        ArrayList<String> arrayListGuesses = mSharplesMenu.getDinnerMenu();
        String[] guessesArr = new String[arrayListGuesses.size()];
        guessesArr = arrayListGuesses.toArray(guessesArr);
        Log.d(TAG, "how many guesses do we have: "+guessesArr.length);
        ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, guessesArr);

        for (AutoCompleteTextView tagField : tagFields){
            Log.d(TAG,"attaching guesses to a tag field");
            tagField.setAdapter(adapter);
            tagField.setHint("autocomplete tag field");

        }
    }

    //endregion

}
