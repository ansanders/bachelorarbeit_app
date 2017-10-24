package development.andre.sanders.bachelorprojectapp.model.manager;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import development.andre.sanders.bachelorprojectapp.model.callbacks.CalculationListener;
import development.andre.sanders.bachelorprojectapp.model.callbacks.MatchingResultListener;
import development.andre.sanders.bachelorprojectapp.model.callbacks.events.MatchingResultEvent;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.tasks.MatchingTask;
import development.andre.sanders.bachelorprojectapp.model.tasks.PrepareMatchingTask;

/**
 * Created by andre on 13.08.17.
 * <p>
 * this class manages all things related to the matching process
 */

public class MatchingManager implements MatchingResultListener {

    private static final String TAG = "MatchingManager";

    //constants
    //number of consumerThreads processing incoming frames
    private static final int MAX_WORKER_THREADS = 3;
    //max. number of polygons the user can get the information of
    public static final int MAX_OBJECTS = 10;
    //max. number of incoming frames pushed in queue
    private static final int MAX_QUEUE_SIZE = 5;
    //accessable instances of detector, descriptor and matcher
    public static final FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SIFT);
    public static final DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
    public static final DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

    //flag, if matching process is running
    public static boolean isRunning = false;


    //Queue for incoming frames
    private BlockingQueue<Mat> frames = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    //list of all active consumerthreads
    private List<Thread> consumerThreads = new ArrayList<>(MAX_WORKER_THREADS);
    //list of current result polygons the user wants the information about
    private List<Polygon> currentResult = new ArrayList<>();

    private List<String> currentInformation = new ArrayList<>();

    private List<Boolean> lastResults = new ArrayList<>(20);


    //c'tor
    public MatchingManager(CalculationListener listener) {


        //if not happened yet, prepare the Content for this specific sourceObject like features and descriptors

        Source currentSource = ResourceManager.getInstance().getCurrentSource();
        if (currentSource.getFeatures() == null && currentSource.getDescriptors() == null) {
            new PrepareMatchingTask(listener, featureDetector, descriptorExtractor).execute();
        } else {
            listener.onCalculationCompleted();
        }
    }

    /**
     * start the featurematching. producer-consumer pattern
     */
    public void startMatching() {
        isRunning = true;
        startConsumers();
    }

    /**
     * stop feature matching and clear all data
     */
    public void stopMatching() {

        isRunning = false;

        if (!consumerThreads.isEmpty()) {
            stopConsumers();
        }

        for (Mat frame : frames) {
            frame.release();
        }
        this.currentResult.clear();
        this.frames.clear();
        this.lastResults.clear();
    }


    @Override
    public synchronized void onMatchingResult(MatchingResultEvent resultEvent) {
        if (resultEvent != null) {
            if (resultEvent.getResult() != null) {


                this.currentResult = resultEvent.getResult();
                Log.d(TAG, "neues Ergebnis!");
                addResult(true);


            }

        }

    }

    /**
     * method to push a frame from any other class
     *
     * @param frame to offer to queue
     */
    public void pushFrame(Mat frame) {

        boolean spaceAvailable = this.frames.offer(frame);
        if (!spaceAvailable)
            Log.d(TAG, "kein Platz in der Queue");
        else
            Log.d(TAG, "pushed Frame in Queue");
    }

    /**
     * init and start consumerThreads
     */
    private void startConsumers() {
        for (int i = 0; i < MAX_WORKER_THREADS; i++) {
            MatchingTask frameConsumer = new MatchingTask(frames, this);
            Thread consumerThread = new Thread(frameConsumer);
            consumerThread.start();
            consumerThreads.add(consumerThread);
        }
    }

    /**
     * stop all running consumerthreads
     */
    private void stopConsumers() {
        for (Thread consumerThread : consumerThreads) {
            consumerThread.interrupt();
            consumerThread = null;
        }

        consumerThreads.clear();
    }


    public List<Polygon> getCurrentResult() {
        return currentResult;
    }

    @Override
    public synchronized void addResult(Boolean result) {
        if (lastResults.size() > 10)
            lastResults.remove(0);

        lastResults.add(result);
    }

    public String getLossRateAsString(){
        StringBuilder lossRateString = new StringBuilder();

        if (lastResults.size() > 0) {
            int numberOfFailures =0;
            for (Boolean result : lastResults) {
                if (!result)
                    numberOfFailures++;
            }

            lossRateString.append(numberOfFailures);
            lossRateString.append(" / ");
            lossRateString.append(lastResults.size());

        }
        else
        lossRateString.append("0 / 0 ");

        lossRateString.append("Lossrate");

        return lossRateString.toString();
    }

    public double getCurrentMatchingRate() {
        if (lastResults.size() > 0) {
            int numberOfMatches = 0;
            for (Boolean result : lastResults) {
                if (result)
                    numberOfMatches++;
            }

            return ( (double)numberOfMatches/(double)lastResults.size()) * 100;
        }
        else
            return 0;
    }


}

