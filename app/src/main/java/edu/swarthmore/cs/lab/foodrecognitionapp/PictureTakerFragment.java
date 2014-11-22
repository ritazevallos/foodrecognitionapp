package edu.swarthmore.cs.lab.foodrecognitionapp;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
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
    public boolean using_emulator = true;


    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {

                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getActivity().getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8,
                this, mLoaderCallback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // we are not making sure there is an app to take pictures
        setHasOptionsMenu(true);

        mFoodPhotoStore = FoodPhotoStore.get(getActivity());
        UUID foodPhotoId = (UUID)getArguments().getSerializable(EXTRA_FOODPHOTO_ID);
        beforePhotoTaken = true;
        mTagFields = new ArrayList<AutoCompleteTextView>();
        CreateDirectoryForPictures();
        mFoodPhoto = mFoodPhotoStore.getFoodPhoto(foodPhotoId);
        mSegmentImageViews = new ArrayList<ImageView>();
        Log.d(TAG, "in onCreate");
        AsyncSharplesGetter dashScraper = new AsyncSharplesGetter();
        dashScraper.execute("go!");
        // populates mSharplesMenu, populates FOOD_GUESSES, and sets menuIsLoaded to true once done
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
        final View v = inflater.inflate(R.layout.activity_picture_taker, parent, false);

        LinearLayout mSegmentsContainer = (LinearLayout)v.findViewById(R.id.segmentsContainerLayout);
//        Button cameraButton = (Button)v.findViewById(R.id.cameraButton);
//        cameraButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                TakeAPicture();
//            }
//        });
        retakePhotoButton = (Button)v.findViewById(R.id.retake_photo_button);
        if(beforePhotoTaken) {
            retakePhotoButton.setVisibility(View.INVISIBLE);
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

//            mImageView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (beforePhotoTaken) {
//                            TakeAPicture();
//                        }
//                    }
//            });

        final LinearLayout tagContainer = (LinearLayout)v.findViewById(R.id.tagContainerLayout);

        mImageView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (beforePhotoTaken && !(using_emulator)) {
                        TakeAPicture();
                    } else {
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

        segmentImage();
    }


    public void segmentImage(){
        Mat imageMat = new Mat();
        Mat mask = new Mat();
        Utils.bitmapToMat(bitmap, imageMat);

        Imgproc.cvtColor(imageMat, mask, Imgproc.COLOR_BayerRG2GRAY);
        Imgproc.GaussianBlur(mask, mask, new Size(3, 3), 0);
        Imgproc.threshold(mask, mask, 0, 255, Imgproc.THRESH_OTSU);

        Core.multiply(mask, imageMat, imageMat);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(imageMat, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Mat> ROIs = new ArrayList<Mat>();
        for(int i=0; i< contours.size();i++) {
            if (Imgproc.contourArea(contours.get(i)) > 50) {
                Rect rect = Imgproc.boundingRect(contours.get(i));
                if (rect.height > 28) {
                    Core.rectangle(imageMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255));
                    ROIs.add(imageMat.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width));

                    ImageView segmentImageView = new ImageView(getActivity());

                    Bitmap segmentBitmap = Bitmap.createBitmap(imageMat.width(), imageMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(imageMat,segmentBitmap);
                    segmentImageView.setImageBitmap(segmentBitmap);

                    mSegmentsContainer.addView(segmentImageView);
                }
            }
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
