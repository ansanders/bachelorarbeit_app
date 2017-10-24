package development.andre.sanders.bachelorprojectapp.model.tasks;

import android.util.Log;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import development.andre.sanders.bachelorprojectapp.model.callbacks.MatchingResultListener;
import development.andre.sanders.bachelorprojectapp.model.callbacks.events.MatchingResultEvent;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.manager.MatchingManager;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;

import static org.opencv.core.CvType.CV_32FC2;

/**
 * Created by andre on 13.08.17.
 * Diese Klasse dient als Consumer und verarbeitet vorhandene Frames in der Queue
 */
public class MatchingTask implements Runnable {

    private static final String TAG = "Consumer";

    //this is the shared queue from matchingmanager for producer-consumer pattern
    private BlockingQueue<Mat> frameQueue;
    private MatchingResultListener listener;


    //Matrizen zum speichern der Features und der Deskriptoren
    private MatOfKeyPoint teilBildFeatures = new MatOfKeyPoint();
    private Mat teilBildDescriptor = new Mat();

    private Mat sceneCorners = new Mat(4, 1, CV_32FC2);
    private Mat targetCorners = new Mat(4, 1, CV_32FC2);

    private Mat outputMask = new Mat();
    private MatOfPoint2f scenePointsMat = new MatOfPoint2f();
    private MatOfPoint2f targetPointsMat = new MatOfPoint2f();

    private MatOfPoint mIntSceneCorners = new MatOfPoint();

    private Mat outImage = new Mat();


    public MatchingTask(BlockingQueue<Mat> frameQueue, MatchingResultListener listener) {

        this.frameQueue = frameQueue;
        this.listener = listener;

    }

    @Override
    public void run() {
        long startTime, elapsedTime, wholeTime, elapsedWholeTime;

        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Mat currentFrame;

        while (MatchingManager.isRunning) {
            try {
                //check if there is a incoming frame waiting for processing
                currentFrame = frameQueue.take();
                if (currentFrame == null)
                    continue;

                wholeTime = System.nanoTime();
                List<Polygon> visiblePolys = new ArrayList<>();
                Source currentSource = ResourceManager.getInstance().getCurrentSource();
                startTime = System.nanoTime();

                //detect teilFeatures
                MatchingManager.featureDetector.detect(currentFrame, teilBildFeatures);
                //describe

                elapsedTime = System.nanoTime() - startTime;

                Log.d(TAG, "FeatureDetection took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");
                if (teilBildFeatures.empty() || currentSource.getFeatures().empty()) {
                    Log.d(TAG, "teilFeatures: " + teilBildFeatures.empty() + " sourceFeatures: " + currentSource.getFeatures().empty());
                    listener.addResult(false);
                    continue;


                }

                startTime = System.nanoTime();
                MatchingManager.descriptorExtractor.compute(currentFrame, teilBildFeatures, teilBildDescriptor);

                elapsedTime = System.nanoTime() - startTime;
                Log.d(TAG, "FeatureDescription took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                //Liste für ergaebnisse der K-nearest-neighbour suche
                List<MatOfDMatch> knnMatchesList = new ArrayList<>();


                if (teilBildDescriptor.empty() || currentSource.getDescriptors().empty()) {
                    Log.d(TAG, "Descriptor fehler" + "teilBild: " + teilBildDescriptor.empty() + "source: " + currentSource.getDescriptors().empty());
                    listener.addResult(false);
                    continue;
                } else {
                    //Matche Deskriptoren sets und schreibe in knnMatch liste
                    startTime = System.nanoTime();
                    MatchingManager.matcher.knnMatch(teilBildDescriptor, currentSource.getDescriptors(), knnMatchesList, 2);
                    elapsedTime = System.nanoTime() - startTime;
                    Log.d(TAG, "Matching took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                }

                startTime = System.nanoTime();
                // ratio test
                LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
                for (MatOfDMatch matOfDMatch : knnMatchesList) {
                    if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 1) {
                        good_matches.add(matOfDMatch.toArray()[0]);

                    }
                }
                elapsedTime = System.nanoTime() - startTime;
                Log.d(TAG, "Ratio Test took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                // get keypoint coordinates of good matches to find homography and remove outliers using ransac
                List<Point> scenePointsList = new ArrayList<>();
                List<Point> targetPointsList = new ArrayList<>();
                for (int i = 0; i < good_matches.size(); i++) {
                    scenePointsList.add(teilBildFeatures.toList().get(good_matches.get(i).queryIdx).pt);
                    targetPointsList.add(currentSource.getFeatures().toList().get(good_matches.get(i).trainIdx).pt);
                }


                if (scenePointsList.isEmpty() ||scenePointsList.size() < 4|| targetPointsList.isEmpty()|| targetPointsList.size() < 4){
                    listener.addResult(false);
                    continue;

                }

                scenePointsMat.fromList(scenePointsList);
                targetPointsMat.fromList(targetPointsList);

                startTime = System.nanoTime();
                //Finde die Transformationsmatrix der übrig gebliebenen Features der Scene zu denen im Urbild
                Mat Homog = Calib3d.findHomography(scenePointsMat, targetPointsMat, Calib3d.RANSAC, 5, outputMask, 2000, 0.995);
                elapsedTime = System.nanoTime() - startTime;
                Log.d(TAG, "Trans.matrix calc took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                if (Homog.empty()){
                    listener.addResult(false);
                    continue;
                }

                //Define the scene in Corner coordinates to find the target region
                sceneCorners.put(0, 0, new double[]{0, 0});
                sceneCorners.put(1, 0, new double[]{currentFrame.cols(), 0});
                sceneCorners.put(2, 0, new double[]{currentFrame.cols(), currentFrame.rows()});
                sceneCorners.put(3, 0, new double[]{0, currentFrame.rows()});

                startTime = System.nanoTime();
                //Perfom perspective transformation to find targetcorners
                Core.perspectiveTransform(sceneCorners, targetCorners, Homog);
                elapsedTime = System.nanoTime() - startTime;
                if(targetCorners.empty()){
                    listener.addResult(false);
                    continue;
                }

                Log.d(TAG, "Perspectivetransform took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "Milliseconds");

                targetCorners.convertTo(mIntSceneCorners, CvType.CV_32S);

                startTime = System.nanoTime();
                if (!Imgproc.isContourConvex(mIntSceneCorners)) {
                    Log.d(TAG, "keine konvexe Hülle");
                    listener.addResult(false);
                    continue;
                }
                elapsedTime = System.nanoTime() - startTime;
                Log.d(TAG, "ConvexCheck took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");


//                    List<Point> targetCornersList = new LinkedList<>();
//                    targetCornersList.add(new Point(targetCorners.get(0, 0)));
//                    targetCornersList.add(new Point(targetCorners.get(1, 0)));
//                    targetCornersList.add(new Point(targetCorners.get(2, 0)));
//                    targetCornersList.add(new Point(targetCorners.get(3, 0)));

                //  System.out.println("TARGET CORNERS : " + targetCornersList.toString());

                List<Polygon> referencePolygons = currentSource.getShapes();


                Polygon resultPoly;
                Rect targetRect = new Rect(new Point(targetCorners.get(0, 0)), new Point(targetCorners.get(2, 0)));
                startTime = System.nanoTime();
                Mat inverseMat = Homog.inv();
                elapsedTime = System.nanoTime() - startTime;
                Log.d(TAG, "Inverse matrix took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                startTime = System.nanoTime();
                for (Polygon poly : referencePolygons) {

//
                    if (poly.isVisible(targetRect))
                        resultPoly = poly;
                    else
                        resultPoly = null;


                    if (resultPoly != null) {
                            //transformiere gefundene Polygondaten in Bildschirmkoordinaten
//                            List<Point> transCorners = new ArrayList<>();
//                            for(Point corner : resultPoly.getCorners()){
//                                Point transformedPoint = transformPoint(corner, inverseMat);
//                                transCorners.add(new Point(transformedPoint.x * OpenCvActivity.scalingX, transformedPoint.y * OpenCvActivity.scalingY));
//                            }

                        Point transformedCenter = transformPoint(new Point(resultPoly.getxPos(), resultPoly.getyPos()), inverseMat);

                        visiblePolys.add(new Polygon(transformedCenter.x, transformedCenter.y, null, resultPoly.getInformation()));

                    }
                }
                inverseMat.release();

                elapsedTime = System.nanoTime() - startTime;

                Log.d(TAG, "Big Polygoncheck took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");


//                // outputMask contains zeros and ones indicating which matches are filtered
//                LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
//                for (int i = 0; i < good_matches.size(); i++) {
//                    if (outputMask.get(i, 0)[0] != 0.0) {
//                        better_matches.add(good_matches.get(i));
//                    }
//                }

//                Mat better_matches_mat = new Mat();
//                if (!better_matches.isEmpty())
//                    better_matches_mat.fromList(better_matches);
////
//                    Imgproc.cvtColor(currentSource.getOriginalMat(), currentSource.getOriginalMat(), Imgproc.COLOR_GRAY2RGB);
//                    Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_GRAY2RGB);
////
////
//                    Mat copyOfOriginal = currentSource.getOriginalMat().clone();
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(0, 0)), new Point(targetCorners.get(1, 0)), new Scalar(255, 255, 255), 4);
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(1, 0)), new Point(targetCorners.get(2, 0)), new Scalar(0, 255, 0), 4);
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(2, 0)), new Point(targetCorners.get(3, 0)), new Scalar(0, 255, 0), 4);
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(3, 0)), new Point(targetCorners.get(0, 0)), new Scalar(0, 255, 0), 4);
//                    Imgproc.drawMarker(imageROI, rectCentre, new Scalar(255, 0, 0));
//                    Features2d.drawMatches(imageROI, teilBildFeatures, copyOfOriginal, currentSourceObject.getFeatures(), better_matches_mat, outImage);
////
//                    Bitmap matchBitMap = Bitmap.createBitmap(outImage.cols(), outImage.rows(), Bitmap.Config.ARGB_8888);
//                    Utils.matToBitmap(outImage, matchBitMap);
//
//                    ResourceManager.saveImageToExternalStorage(OpenCvActivity.class, matchBitMap);
////
//
//
//                    //mImg.setImageBitmap(matchBitMap);
//                }
                    Homog.release();
                    currentFrame.release();
                    elapsedWholeTime = System.nanoTime() - wholeTime;
                    Log.d(TAG, "Whole Process took : " + TimeUnit.MILLISECONDS.convert(elapsedWholeTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                listener.onMatchingResult(new MatchingResultEvent(visiblePolys));


                    //release allocated memory


                } catch(InterruptedException e){
                    e.printStackTrace();
                    freeAllMemory();
                }
            }
        }

    private void freeAllMemory() {
        teilBildFeatures.release();
        teilBildDescriptor.release();
        sceneCorners.release();
        targetCorners.release();
        outputMask.release();
        scenePointsMat.release();
        targetPointsMat.release();
        mIntSceneCorners.release();

    }

    /**
     *
     * @param originPoint Point before transformation
     * @param transMat transformation matrix
     * @return transformed Point
     */
    private Point transformPoint (Point originPoint, Mat transMat){
        Point transPoint;
        Mat originPointMat = new Mat(1,1, CV_32FC2);
        Mat transPointMat = new Mat(1,1, CV_32FC2);

        originPointMat.put(0,0, new double[]{originPoint.x, originPoint.y});
        Core.perspectiveTransform(originPointMat, transPointMat, transMat);
        transPoint = new Point(transPointMat.get(0,0));

        return transPoint;
    }
}

