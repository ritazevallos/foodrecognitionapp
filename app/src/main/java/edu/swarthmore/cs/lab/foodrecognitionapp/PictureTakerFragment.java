package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


public class PictureTakerFragment extends Fragment {
    public static final int MEDIA_TYPE_IMAGE = 1;
    private static final String TAG = "PictureTakerFragment";
    public static File mFile;
    public static File mDir;
    public static Bitmap bitmap;
    public static ImageView mImageView;
    private AutoCompleteTextView mTagField;
    private ArrayList<AutoCompleteTextView> mTagFields;
    private FoodPhoto mFoodPhoto;
    private FoodPhotoStore mFoodPhotoStore;
    private Button retakePhotoButton;
    public boolean beforePhotoTaken;
    private SharplesMenu mSharplesMenu;
    private boolean menuIsLoaded = false;
    private LinearLayout mSegmentsContainer;
    public static final String EXTRA_FOODPHOTO_ID =
            "edu.swarthmore.cs.lab.foodrecognitionapp.foodphoto_id";
    private ArrayList<ImageView> mSegmentImageViews;
    // make this true if you don't want it to break when opening up camera
    public boolean using_emulator = false;
    private ArrayList<Rect> ROIs;
    private boolean clickNTagActivated;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // we are not making sure there is an app to take pictures
        setHasOptionsMenu(true);
        clickNTagActivated = false;
        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        UUID foodPhotoId = (UUID)getArguments().getSerializable(EXTRA_FOODPHOTO_ID);
        beforePhotoTaken = true;
        mTagFields = new ArrayList<AutoCompleteTextView>();
        CreateDirectoryForPictures();
        mFoodPhoto = mFoodPhotoStore.getFoodPhoto(foodPhotoId);
        mSegmentImageViews = new ArrayList<ImageView>();
        AsyncSharplesGetter dashScraper = new AsyncSharplesGetter();
        dashScraper.execute("go!");
        // populates mSharplesMenu, populates FOOD_GUESSES, and sets menuIsLoaded to true once done


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        final View v = inflater.inflate(R.layout.activity_picture_taker, parent, false);

        mSegmentsContainer = (LinearLayout)v.findViewById(R.id.segmentsContainerLayout);

        final Button addTagsButton = (Button)v.findViewById(R.id.add_tags_button);
        final Button doneTagsButton = (Button)v.findViewById(R.id.done_tags_button);
        doneTagsButton.setVisibility(View.GONE);

        addTagsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickNTagActivated = true;
                doneTagsButton.setVisibility(View.VISIBLE);
                addTagsButton.setVisibility(View.GONE);

            }
        });
        doneTagsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickNTagActivated = false;
                addTagsButton.setVisibility(View.VISIBLE);
                doneTagsButton.setVisibility(View.GONE);

            }
        });

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


        Button saveButton = (Button)v.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "tag says: " + mFoodPhoto.getTags());
                if (mFoodPhoto.getTags().isEmpty()){
                    Toast toast = Toast.makeText(getActivity(), "Remember to tag your photo", Toast.LENGTH_SHORT);
                    toast.show();
                } else{
                    mFoodPhotoStore.addFoodPhoto(mFoodPhoto);
                    FoodPhotoStore.get(getActivity()).saveFoodPhotos();
                    openGallery();
                }
            }
        });

        mTagField = (AutoCompleteTextView)v.findViewById(R.id.pictureTag);
        mTagField.setVisibility(View.INVISIBLE);

        mTagField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String msg = s.toString();
                //mFoodPhoto.setOneTag(0, msg, 0, 0);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mImageView = (ImageView)v.findViewById(R.id.imageView1);
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
            bitmap = null;
        }

        final LinearLayout tagContainer = (LinearLayout)v.findViewById(R.id.tagContainerLayout);

        mImageView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (beforePhotoTaken && !(using_emulator)) {
                        TakeAPicture();
                    } else if (clickNTagActivated) {
                        Log.d(TAG, "mImageView.onTouchListener");
                        addTagField(event.getX(), event.getY(), tagContainer);
                        //mTagField.setVisibility(View.VISIBLE);

                        retakePhotoButton.setVisibility(View.VISIBLE);
                    }
                }
                return true;
            }
        });

        return v;


    }

    private void addTagField(final float x, final float y, final LinearLayout tagContainer){ // declared final so we can access in inner block
        final AutoCompleteTextView tag_field = new AutoCompleteTextView(getActivity());
        tag_field.setHint("Tag this picture");
        int index = mFoodPhoto.getTags().size();
        mFoodPhoto.setOneTag("",x,y,index);

        tag_field.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String)parent.getItemAtPosition(position);
                mFoodPhoto.setOneTag(selection, x, y);
            }
        });

        // doing this after so that mTagFields.size() is the correct index for the new tag
        mTagFields.add(tag_field);
        tagContainer.addView(tag_field);

        ArrayList<AutoCompleteTextView> final_tagfield = new ArrayList<AutoCompleteTextView>();
        final_tagfield.add(tag_field);
        if (menuIsLoaded) {
            attachGuessesToTagFields(final_tagfield);
        }
    }

    private void removeTag(){
        //TODO: need to get index of tag and remove both from the layout and the foodphoto.maybe this could be called by pressing the ESC key
    }

    private void CreateDirectoryForPictures()
    {
        mDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "BOOKS");
        if (!mDir.exists())
        {
            mDir.mkdirs();
        }
    }

    private void TakeAPicture()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        mFile = new File(mDir, String.format("foodPhoto_"+ UUID.randomUUID() + ".jpg"));
        Log.d(TAG, "file path: " + mFile.toString());
        mFoodPhoto.setFile(mFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mFile));

        beforePhotoTaken = false;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "in onActivityResult");

        super.onActivityResult(requestCode,resultCode,data);


        // make it available in the gallery
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(mFile);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);

        // display in ImageView. We will resize the bitmap to fit the display
        // Loading the full sized image will consume to much memory
        // and cause the application to crash.
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), contentUri);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        mImageView.setImageBitmap(bitmap);
        retakePhotoButton.setVisibility(View.VISIBLE);

        segmentImage();
    }


    public void segmentImage(){
        try {
            Mat imageMat = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC4); // fc means floating point matrices

            Utils.bitmapToMat(bitmap, imageMat);
            Mat mask = new Mat(imageMat.size(), imageMat.type());

            Imgproc.cvtColor(imageMat, mask, Imgproc.COLOR_RGBA2GRAY, 4); // or should we use COLOR_BayerRG2GRAY? COLOR_BGR2GRAY
            //mask.create(mask.rows(), mask.cols(), CvType.CV_8UC4);
            Imgproc.GaussianBlur(mask, mask, new Size(3, 3), 0);
            Imgproc.threshold(mask, mask, 0, 255, Imgproc.THRESH_OTSU);

            // uncomment to show the mask

//            Bitmap maskBitmap = Bitmap.createBitmap(mask.cols(), mask.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(mask, maskBitmap);
//            ImageView maskView = new ImageView(getActivity());
//            maskView.setImageBitmap(maskBitmap);
//            TextView maskText = new TextView(getActivity());
//            maskText.setText("mask");
//            mSegmentsContainer.addView(maskText);
//            mSegmentsContainer.addView(maskView);

            Mat maskedImg = new Mat(imageMat.size(), imageMat.type());
            maskedImg.setTo(new Scalar(0,0,0));
            imageMat.copyTo(maskedImg, mask);

            // uncomment to show the masked image

//            Bitmap maskedImgBitmap = Bitmap.createBitmap(maskedImg.cols(), maskedImg.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(maskedImg, maskedImgBitmap);
//            ImageView maskedImgView = new ImageView(getActivity());
//            maskedImgView.setImageBitmap(maskedImgBitmap);
//            TextView maskedText = new TextView(getActivity());
//            maskedText.setText("masked");
//            mSegmentsContainer.addView(maskedText);
//            mSegmentsContainer.addView(maskedImgView);

            List <MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(mask, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            ROIs = new ArrayList<Rect>();
            Scalar colorGreen = new Scalar(0, 255, 0,255);
            Scalar colorRed = new Scalar(255, 0, 0, 255);
            int minSize = 500; // we can figure out what this should be using the tray size: they should position the camera
            //so's the tray fills the camera screen

            TextView segmentsText = new TextView(getActivity());
            segmentsText.setText("segments");
            mSegmentsContainer.addView(segmentsText);

            List <MatOfPoint> bigContours = new ArrayList<MatOfPoint>();
            List <Point> centers = new ArrayList<Point>();

            for (int i = 0; i < contours.size(); i++) {
                // draw contour in green
                if (Imgproc.contourArea(contours.get(i)) > minSize) {
                    bigContours.add(contours.get(i));
                    Rect rect = Imgproc.boundingRect(contours.get(i));
                    Point center = new Point(rect.x+rect.width/2.0, rect.y+rect.height/2.0);
                    centers.add(center);
                    // draw bounding rectangle in green
//                        Core.rectangle(imageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), colorGreen);

                    Mat subMat = imageMat.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
                    ROIs.add(rect);

                    ImageView segmentImageView = new ImageView(getActivity());

                    Bitmap segmentBitmap = Bitmap.createBitmap(subMat.cols(), subMat.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(subMat, segmentBitmap);
                    segmentImageView.setImageBitmap(segmentBitmap);
                    mSegmentsContainer.addView(segmentImageView);
                }
            }

            // the image with contours
            Mat drawnContours = new Mat(imageMat.size(), imageMat.type());
            imageMat.copyTo(drawnContours);

            Log.d(TAG, "number of big contours: " + bigContours.size());
            for (int i=0; i< bigContours.size(); i++){
                // switch around the commented section to draw contours instead of rectangles
                //Imgproc.drawContours ( drawnContours, bigContours, i, colorGreen, 15);
                Rect rect = ROIs.get(i);
                Core.rectangle(drawnContours, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), colorGreen, 15);
                Point center = centers.get(i);
                Core.putText(drawnContours, Integer.toString(i+1), center, Core.FONT_HERSHEY_COMPLEX_SMALL, 0.8, colorRed);

                Log.d(TAG, "trying to label at "+ center.toString());
                //todo: labels aren't drawing
            }

            Bitmap contoursBitmap = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(drawnContours, contoursBitmap);
            ImageView contoursView = new ImageView(getActivity());
            contoursView.setImageBitmap(contoursBitmap);
            TextView contoursText = new TextView(getActivity());
            mSegmentsContainer.addView(contoursText);
            mImageView.setImageBitmap(contoursBitmap);

        } catch(Exception ex){
            Log.d(TAG, "Error in segmentImage: "+ex);
        }

    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.picture_taker, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.menu_item_gallery) {
            Log.d(TAG, "In OptionsItemSelected");

            openGallery();
        }

        return true;
    }

    public void openGallery(){
        Intent i = new Intent(getActivity(), FoodPhotoListActivity.class);
        startActivity(i);
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
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

    public static PictureTakerFragment newInstance(UUID healthID) {
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FOODPHOTO_ID, healthID);

        PictureTakerFragment fragment = new PictureTakerFragment();
        fragment.setArguments(args);

        return fragment;
    }

    private class AsyncSharplesGetter extends AsyncTask<String, Integer, String> {
        //todo: what are the string, int, string in the constructor?

        @Override
        protected void onPreExecute() {
            Log.d(TAG, "in AsyncSharplesGetter.onPreExecute()");
            super.onPreExecute();
            //mBreakfastMenuView.setText("loading menu");
            // show progress bar
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
            menuIsLoaded = true;

            // populate the guesses
//            ArrayList<String> arrayListGuesses = mSharplesMenu.getMenu(new Date());
//            String[] guessesArr = new String[arrayListGuesses.size()];
//            guessesArr = arrayListGuesses.toArray(guessesArr);
//            Log.d(TAG, "how many guesses do we have: "+guessesArr.length);
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, guessesArr);
//            mTagField.setAdapter(adapter);

            attachGuessesToTagFields(mTagFields);

        }
    }

    private void attachGuessesToTagFields(ArrayList<AutoCompleteTextView> tagFields){
        ArrayList<String> arrayListGuesses = mSharplesMenu.getMenu(new Date());
        String[] guessesArr = new String[arrayListGuesses.size()];
        guessesArr = arrayListGuesses.toArray(guessesArr);
        Log.d(TAG, "how many guesses do we have: "+guessesArr.length);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, guessesArr);

        for (AutoCompleteTextView tagField : tagFields){
            Log.d(TAG,"attaching guesses to a tag field");
            tagField.setAdapter(adapter);
            tagField.setHint("autocomplete tag field");

        }
    }

}
