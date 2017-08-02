package development.andre.sanders.bachelorprojectapp.view;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.model.FilterFactory;
import development.andre.sanders.bachelorprojectapp.model.callbacks.OnCalculationCompleted;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.filters.Filter;

/**
 * Created by andre on 04.07.17.
 * <p>
 * Diese Klasse repräsentiert eine OpenCV activity.
 */

public class OpenCvActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, OnCalculationCompleted {

    String currentAppMode;
    String TAG = "MAIN";

    //Virtuelle Repräsentation der Opencv-Camera
    public static CustomJavaCameraView mOpenCvCamera;

    //aktuell aktiver Filter, der auf das Inputframe agewendet wird
    private Filter activeFilter = null;

    private FilterFactory filterFactory = null;

    private String currentSourceId;
    private ProgressDialog progressDialog;
    private boolean allInit = false;
    private boolean matchingEnabled = false;


    //Frame related data
    private Mat mRgba;
    private Mat mGrayScale;

    private int frameCounter;

    //Die aktuelle dem User angezeigte Information
    String currentObjectInfo = "";
    List<Polygon> resultPolys = new ArrayList<>();

    //Touch input stuff
    static final int MAX_DURATION = 500;
    long startTime;
    /* variable for calculating the total time*/
    long duration;
    int tapCount = 0;


    /**
     * Dieser Callback wird aufgerufen, sobald OpenCv versucht wurde zu laden.
     * Dies passiert immer, wenn die OnResume methode getriggert wird.
     * Im Falle des Erfolgs werden alle weiteren wichtigen Aufrufe getätigt.
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {

                    Log.i("OpenCvActivity", "OpenCV loaded successfully");

                    if (filterFactory == null) {
                        showProgressDialog();
                        filterFactory = new FilterFactory(OpenCvActivity.this, OpenCvActivity.this);
                        if (activeFilter == null) {
                            //Bei der Erstellung des Filters werden alle Features und Vektoren der urbilder berechnet
                            activeFilter = filterFactory.getFilter("student");
                        }


                    }

                    //enable user view
                    mOpenCvCamera.enableView();

                    final Button matchButton = (Button) findViewById(R.id.matchButton);
                    matchButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (matchingEnabled) {
                                matchingEnabled = false;
                                matchButton.setText("start");

                            } else {
                                matchingEnabled = true;
                                matchButton.setText("stop");
                            }
                        }
                    });


                    mOpenCvCamera.setOnTouchListener(OpenCvActivity.this);


                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            currentAppMode = null;
            currentSourceId = null;
        } else {
            currentAppMode = extras.getString("appMode");
            currentSourceId = extras.getString("sourceObject");
        }


    }


    @Override
    protected void onStart() {
        super.onStart();


        mOpenCvCamera = (CustomJavaCameraView) findViewById(R.id.javaCameraView);

        mOpenCvCamera.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCamera.setCvCameraViewListener(OpenCvActivity.this);
        //reduce framesize to male processing faster
        mOpenCvCamera.setMaxFrameSize(768, 480);


    }

    /**
     * called, when the cameraview got enabled
     *
     * @param width  -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */

    @Override
    public void onCameraViewStarted(int width, int height) {

        mOpenCvCamera.setFocusMode(OpenCvActivity.this, "Auto");
        frameCounter = 0;
        mGrayScale = new Mat();
        mRgba = new Mat();

    }

    /**
     * @param inputFrame inputframe to process
     * @return augmented output frame
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if (frameCounter >= Integer.MAX_VALUE)
            frameCounter = 0;

        mGrayScale = inputFrame.gray();
        mRgba = inputFrame.rgba();

        final String errorMessage = "";

        // jede 1/2s
        if (frameCounter % 4 == 0 && allInit && matchingEnabled) {


            //Berechnungen werden asynchron durchgeführt, da sie je nach gerät länger dauern könnten
            AsyncTask<Void, Integer, Void> filterTask = new AsyncTask<Void, Integer, Void>() {

                //alle sichtbaren polygoninfos
                List<Polygon> currentPolys = new ArrayList<>();

                @Override
                protected Void doInBackground(Void[] params) {

                    currentPolys = activeFilter.apply(mGrayScale);

                    return null;
                }

                @Override
                protected void onProgressUpdate(Integer... values) {
                    super.onProgressUpdate(values);

                }


                //after executing the code in the thread
                @Override
                protected void onPostExecute(Void result) {
                    super.onPostExecute(result);
                        resultPolys = currentPolys;

                }
            }.execute();


        }

        if (resultPolys != null) {
            for (Polygon poly : resultPolys) {
                Log.d(TAG, poly.getInformationAsText());
                Imgproc.putText(mRgba, poly.getInformationAsText(), new Point(poly.getxPos(), poly.getyPos()),
                        Core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(0, 255, 255));
            }
        } else {
            Imgproc.putText(mRgba, "Bitte warten", new Point(mRgba.cols() / 2, mRgba.rows() / 2),
                    Core.FONT_HERSHEY_PLAIN, 2.0, new Scalar(0, 255, 255));
        }
        frameCounter++;
        return mRgba;
    }

    /**
     * release all Data when camera stopps
     */
    @Override
    public void onCameraViewStopped() {

        mRgba.release();
        mGrayScale.release();


    }

    //Touch Callback
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Log.i("OpenCvActivity", "entered onTouch " + event.getPointerCount());
        Camera.Parameters params = mOpenCvCamera.getParameters();
        int action = event.getAction();

        /*handle zoom when more than one finger detected*/
        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                mOpenCvCamera.setmDist(mOpenCvCamera.getFingerSpacing(event));
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                mOpenCvCamera.getCamera().cancelAutoFocus();
                mOpenCvCamera.handleZoom(event, params);

            }
        } else {

//            /*singletap - autofocus*/
//            switch (event.getAction() & MotionEvent.ACTION_MASK) {
//                case MotionEvent.ACTION_DOWN:
//                    startTime = System.currentTimeMillis();
//                    tapCount++;
//                    break;
//                case MotionEvent.ACTION_UP:
//                    mOpenCvCamera.handleFocus(event, params);
//                    long time = System.currentTimeMillis() - startTime;
//                    duration = duration + time;
//                    if (tapCount == 2) {
//                        if (duration <= MAX_DURATION) {
//                            //tap code
//                        }
//                        tapCount = 0;
//                        duration = 0;
//                        break;
//                    }
//            }
            // handle single touch events
            if (action == MotionEvent.ACTION_UP) {
                mOpenCvCamera.handleFocus(event, params);

            }

        }

        return true;
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCamera != null)
            mOpenCvCamera.disableView();
    }

    /**
     * Wird jedes mal aufgerufen, wenn die View sich ändert (Displayrotation, verlust des Appfokus etc)
     */
    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCvActivity", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, getApplicationContext(), mLoaderCallback);
        } else {
            Log.d("OpenCvActivity", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCamera != null)
            mOpenCvCamera.disableView();
    }

    /**
     * Menu Part
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;

    }

    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {


            case R.id.focus1:
                mOpenCvCamera.setFocusMode(this, "Auto");
                return true;


            case R.id.focus2:
                mOpenCvCamera.setFocusMode(this, "ContiniousVideo");
                return true;


            case R.id.focus3:
                mOpenCvCamera.setFocusMode(this, "EDOF");
                return true;


            case R.id.focus4:
                mOpenCvCamera.setFocusMode(this, "Fixed");
                return true;


            case R.id.focus5:
                mOpenCvCamera.setFocusMode(this, "Infinity");
                return true;


            case R.id.focus6:
                mOpenCvCamera.setFocusMode(this, "Makro");
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onCalculationCompleted() {

        progressDialog.dismiss();
        allInit = true;
    }

    void showProgressDialog() {
        progressDialog = new ProgressDialog(this);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.setTitle("Bitte warten");
        progressDialog.setMessage("Berechne");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        //The maximum number of items is 100
        progressDialog.setMax(100);
        //Set the current progress to zero
        progressDialog.setProgress(0);

        progressDialog.show();
    }

    @Override
    public void setProgress(Integer progress, String message) {
        progressDialog.setMessage(message);
        progressDialog.incrementProgressBy(progress);

    }

    public String getCurrentAppMode() {
        return currentAppMode;
    }

    public void setCurrentAppMode(String currentAppMode) {
        this.currentAppMode = currentAppMode;
    }

    public String getCurrentSourceId() {
        return currentSourceId;
    }

    public void setCurrentSourceId(String currentSource) {
        this.currentSourceId = currentSource;
    }

}
