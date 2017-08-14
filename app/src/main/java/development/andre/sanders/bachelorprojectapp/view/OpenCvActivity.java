package development.andre.sanders.bachelorprojectapp.view;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
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
import android.widget.Toast;

import com.squareup.otto.Subscribe;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.model.callbacks.ActionController;
import development.andre.sanders.bachelorprojectapp.model.callbacks.CalculationListener;
import development.andre.sanders.bachelorprojectapp.model.callbacks.events.MatchingResultEvent;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.manager.EventBus;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;
import development.andre.sanders.bachelorprojectapp.model.matching.Matcher;
import development.andre.sanders.bachelorprojectapp.model.matching.MatcherFactory;
import development.andre.sanders.bachelorprojectapp.utils.Constants;

/**
 * Created by andre on 04.07.17.
 * <p>
 * Diese Klasse repräsentiert eine OpenCV activity.
 */

public class OpenCvActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, CalculationListener {

    private String TAG = "MAIN";
    private static Context mContext;

    //aktueller appModus
    private String currentAppMode;

    //Virtuelle Repräsentation der Opencv-Camera
    public static CustomJavaCameraView mOpenCvCamera;

    //aktuell aktiver Matcher, der auf das Inputframe agewendet wird
    private MatcherFactory matcherFactory = null;
    private Matcher activeMatcher = null;


    private ProgressDialog progressDialog;
    private boolean allInit = false;
    private boolean matchingEnabled = false;


    //Frame related data
    private Mat mRgba;
    private Mat mGrayScale;

    private int frameCounter;


    private String visibleStatusMessage = "";

    Queue<AsyncTask> runningTasks = new LinkedList<>();

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


                    if (matcherFactory == null) {
                        showProgressDialog();
                        matcherFactory = new MatcherFactory(OpenCvActivity.this);
                        if (activeMatcher == null) {
                            //Bei der Erstellung des Filters werden alle Features und Vektoren der urbilder berechnet
                            if (currentAppMode.equals(Constants.STUDENT_MODE))
                                activeMatcher = matcherFactory.getFilter("student");
                            else if (currentAppMode.equals(Constants.MUSEUM_MODE))
                                activeMatcher = matcherFactory.getFilter("museum");
                            else
                                activeMatcher = matcherFactory.getFilter("student");
                        }


                    }

                   // EventBus.getInstance().getBus().register(this);

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

        Bundle extras = getIntent().getExtras();

        currentAppMode = extras.getString("appMode");
        String currentSourceId = extras.getString("sourceObject");


        ResourceManager.getInstance().setCurrentSourceById(currentSourceId);


        mContext = this;

    }


    @Override
    protected void onStart() {
        super.onStart();


        mOpenCvCamera = (CustomJavaCameraView) findViewById(R.id.javaCameraView);

        mOpenCvCamera.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCamera.setCvCameraViewListener(OpenCvActivity.this);
        //reduce framesize to male processing faster
        mOpenCvCamera.setMaxFrameSize(640, 480);




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


        if (allInit) {

            //currentInputObjects = inputDetector.detectObject(mGrayScale);

            final String errorMessage = "";

            // jede 1/2s
            if (frameCounter % 4 == 0 && matchingEnabled) {


                //Berechnungen werden asynchron durchgeführt, da sie je nach gerät länger dauern könnten
                final AsyncTask<Void, Integer, Void> filterTask = new AsyncTask<Void, Integer, Void>() {

                    //alle sichtbaren polygoninfos
                    List<Polygon> currentPolys = new ArrayList<>();
                    @Override
                    protected void onPreExecute(){
                      //  runningTasks.add(this);
                    }
                    @Override
                    protected Void doInBackground(Void[] params) {

                        // if(currentInputObjects != null &&!currentInputObjects.isEmpty())
                        currentPolys = activeMatcher.match(mGrayScale);

                        return null;
                    }

                    @Override
                    protected void onProgressUpdate(Integer... values) {
                        super.onProgressUpdate(values);

                    }


                    //after executing the code in the thread
                    @Override
                    protected void onPostExecute(Void result) {
                      //  runningTasks.remove(this);
                        super.onPostExecute(result);
                        resultPolys = currentPolys;

                    }
                }.execute();


            }

            if (resultPolys != null) {

                if (resultPolys.size() > 4) {
                    visibleStatusMessage = "zu viele Objekte, du musst näher ran zoomen";
                    Log.d(TAG, "zu viele Gesichter");
                    Imgproc.putText(mRgba, visibleStatusMessage, new Point(0, mGrayScale.rows() / 5),
                            Core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(0, 255, 255));
                } else {
                    for (Polygon poly : resultPolys) {
                        Log.d(TAG, poly.getInformationAsText());
                        Imgproc.putText(mRgba, poly.getInformationAsText(), new Point(poly.getxPos(), poly.getyPos()),
                                Core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(0, 255, 255));
                    }
                }

            } else {
                Imgproc.putText(mRgba, "Bitte warten", new Point(mRgba.cols() / 2, mRgba.rows() / 2),
                        Core.FONT_HERSHEY_PLAIN, 2.0, new Scalar(0, 255, 255));
            }
        }
//        if (currentInputObjects != null && !currentInputObjects.isEmpty()) {
//            for (Rect rect : currentInputObjects) {
//                Imgproc.rectangle(mRgba, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height),
//                        new Scalar(0, 255, 0));
//            }
//        }


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

    @Subscribe
    public void onMatchingResult(MatchingResultEvent ev){
        Toast.makeText(this, "MATCH", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showCalculationStatus(Integer progress, String message) {
        if (progressDialog != null) {
            progressDialog.setMessage(message);
            progressDialog.incrementProgressBy(progress);
        }

    }


    @Override
    public void onCalculationCompleted() {
        progressDialog.dismiss();
        progressDialog = null;
        allInit = true;
    }


    public void onDestroy() {
        if (mContext != null)
            mContext = null;

        if (mOpenCvCamera != null)
            mOpenCvCamera.disableView();

       // EventBus.getInstance().getBus().unregister(this);

        //stopAllTasks();

        ResourceManager.getInstance().releaseAllData();

        super.onDestroy();

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

    public void stopAllTasks(){
        if(!runningTasks.isEmpty()){
            for(AsyncTask task : runningTasks){
               task.cancel(true);
            }
        }
    }

    public String getCurrentAppMode() {
        return currentAppMode;
    }

    public void setCurrentAppMode(String currentAppMode) {
        this.currentAppMode = currentAppMode;
    }


    public static Context getmContext() {
        return mContext;
    }

}
