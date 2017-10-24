package development.andre.sanders.bachelorprojectapp.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.model.callbacks.ActionController;
import development.andre.sanders.bachelorprojectapp.model.callbacks.CalculationListener;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.manager.MatchingManager;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;

/**
 * Created by andre on 04.07.17.
 * <p>
 * Diese Klasse repräsentiert eine OpenCV activity.
 */

public class OpenCvActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, CalculationListener {

    private static final String TAG = "MAIN";
    public static final Size INPUT_FRAMESIZE = new Size(360, 360);
    public static final Size OUTPUT_FRAMESIZE = new Size(1200, 800);
    public static final double scalingX = OUTPUT_FRAMESIZE.width / INPUT_FRAMESIZE.width;
    public static final double scalingY = OUTPUT_FRAMESIZE.height/ INPUT_FRAMESIZE.height;
    //matchingmanager instance
    private MatchingManager matchingManager = null;


    //Virtuelle Repräsentation der Opencv-Camera
    public static CustomJavaCameraView mOpenCvCamera;


    private ProgressDialog progressDialog;
    private boolean allInit = false;
    private boolean matchingEnabled = false;


    //Frame related data
    private Mat mRgba;
    private Mat mGrayScale;
    private Mat resizedInput;
    private int frameCounter;


    //Die aktuelle dem User angezeigte Information
    List<Polygon> resultPolys = new ArrayList<>();


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
                    if (matchingManager == null) {

                        matchingManager = new MatchingManager(OpenCvActivity.this);

                    }

//                    if (matcherFactory == null) {
//
//                        matcherFactory = new MatcherFactory(OpenCvActivity.this);
//                        if (activeMatcher == null) {
//                            //Bei der Erstellung des Filters werden alle Features und Vektoren der urbilder berechnet
//                            if (currentAppMode.equals(Constants.STUDENT_MODE))
//                                activeMatcher = matcherFactory.getFilter("student");
//                            else if (currentAppMode.equals(Constants.MUSEUM_MODE))
//                                activeMatcher = matcherFactory.getFilter("museum");
//                            else
//                                activeMatcher = matcherFactory.getFilter("student");
//                        }
//
//
//                    }

                    //enable user view
                    mOpenCvCamera.enableView();

                    final Button matchButton = (Button) findViewById(R.id.matchButton);
                    matchButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (matchingEnabled) {
                                matchingEnabled = false;
                                matchButton.setText(R.string.startMatching);
                                matchingManager.stopMatching();
                            } else {
                                matchingManager.startMatching();
                                matchingEnabled = true;
                                matchButton.setText(R.string.stopMatching);


                            }
                        }
                    });


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
     * @param savedInstanceState erstellt die Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


        this.setTitle("Du befindest dich vor dem Bild: " + ResourceManager.getInstance().getCurrentSource().getSourceId() + " im AppModus: " + ResourceManager.getInstance().getCurrentAppMode());

    }


    @Override
    protected void onStart() {
        super.onStart();


        mOpenCvCamera = (CustomJavaCameraView) findViewById(R.id.javaCameraView);

        mOpenCvCamera.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCamera.setCvCameraViewListener(OpenCvActivity.this);
        //reduce framesize to male processing faster
        mOpenCvCamera.setMaxFrameSize(1280, 960);


    }

    /**
     * called, when the cameraview got enabled
     *
     * @param width  -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */

    @Override
    public void onCameraViewStarted(int width, int height) {


        mOpenCvCamera.setOnTouchListener(new ActionController(mOpenCvCamera));
        mOpenCvCamera.setFocusMode(OpenCvActivity.this, "Auto");

        frameCounter = 0;
        mGrayScale = new Mat();
        mRgba = new Mat();
        resizedInput = new Mat();

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


        Imgproc.resize(mGrayScale, resizedInput, INPUT_FRAMESIZE);

        if (allInit) {

            //currentInputObjects = inputDetector.detectObject(mGrayScale);


            if (frameCounter % 15 == 0 && matchingEnabled) {

                matchingManager.pushFrame(resizedInput);

            }
            //alle 5 sekunden aufräumen
            if (frameCounter % 150 == 0) {
                System.gc();
                System.runFinalization();
            }

            resultPolys = matchingManager.getCurrentResult();

                if (resultPolys != null) {

                    if (resultPolys.size() > MatchingManager.MAX_OBJECTS) {

                        Log.d(TAG, "zu viele Matches");
                        Imgproc.putText(mRgba, "zu viele Objekte, du musst näher ran zoomen", new Point(0, mRgba.rows() / 5),
                                Core.FONT_HERSHEY_PLAIN, 1, new Scalar(0, 255, 255));
                    } else if (resultPolys.isEmpty()) {
                        Imgproc.putText(mRgba, "noch keine Matches", new Point(0, mRgba.rows() / 5),
                                Core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(0, 255, 255));
                    } else {
                        for (Polygon poly : resultPolys) {
                            Log.d(TAG, poly.getInformationAsText());
                            Imgproc.putText(mRgba, poly.getInformationAsText(), new Point(poly.getxPos() * scalingX, poly.getyPos()*scalingY),
                                    Core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(0, 255, 255));
                            if (poly.getCorners() != null && !poly.getCorners().isEmpty()) {
                                for (int i = 0; i < poly.getCorners().size() - 1; i++) {
                                    Imgproc.line(mRgba, poly.getCorners().get(i), poly.getCorners().get(i + 1), new Scalar(255, 255, 255), 4);
                                }
                                Imgproc.line(mRgba, poly.getCorners().get(poly.getCorners().size() - 1), poly.getCorners().get(0), new Scalar(255, 255, 255), 4);
                            }
                        }
                }
            }

        } else {
            Imgproc.putText(mRgba, "Bitte warten", new Point(mRgba.cols() / 2, mRgba.rows() / 2),
                    Core.FONT_HERSHEY_PLAIN, 2.0, new Scalar(0, 255, 255));
        }
        Imgproc.putText(mRgba, String.valueOf(new DecimalFormat("##.##").format(matchingManager.getCurrentMatchingRate())) + " %", new Point(10, 30),
                Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 255));
        Imgproc.putText(mRgba, String.valueOf(matchingManager.getLossRateAsString()), new Point(150, 30),
                Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 255));


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
        resizedInput.release();
        resultPolys.clear();
        matchingManager.stopMatching();

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


    /**
     * CALLBACKS
     */


    @Override
    public void onCalculationUpdate(Integer progress, String message) {
        if (progressDialog != null) {
            progressDialog.setMessage(message);
            progressDialog.incrementProgressBy(progress);
        } else {

        }

    }


    @Override
    public void onCalculationCompleted() {
        if(progressDialog!= null)
        progressDialog.dismiss();

        allInit = true;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "ON_DESTROY");
        ResourceManager.getInstance().releaseAllData();
        super.onDestroy();

    }

    @Override
    public void showProgressDialog(String title) {
        progressDialog = new ProgressDialog(this);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.setTitle(title);
        progressDialog.setMessage("Berechne");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        //The maximum number of items is 100
        progressDialog.setMax(100);
        //Set the current progress to zero
        progressDialog.setProgress(0);
        progressDialog.show();
    }


    /**
     * Menu Part
     *
     * @param menu menu to invoke
     * @return true if success
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


    public Context getmContext() {
        return this;
    }

}
