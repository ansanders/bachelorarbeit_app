package development.andre.sanders.bachelorprojectapp.model.filters;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import org.opencv.android.Utils;
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
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.model.ResourceManager;
import development.andre.sanders.bachelorprojectapp.model.callbacks.OnCalculationCompleted;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.view.OpenCvActivity;

/**
 * Created by andre on 17.04.17.
 * <p>
 * Detection-Filter, der auf ein Frame angewendet werden kann und die entsprechenden
 * Informationen als output liefern soll.
 */

public class StudentDetectionFilter implements Filter {

    //class-tag
    private final static String TAG = "FacedetectionFilter";
    // context reference
    private OpenCvActivity activity;

    ////////////////////////////////////// FEATURE DETECTION /////////////////////////////////////////////
    private final FeatureDetector orbFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
    private final FeatureDetector siftFeatureDetector = FeatureDetector.create(FeatureDetector.SIFT);
    private final FeatureDetector surfFeatureDetector = FeatureDetector.create(FeatureDetector.SURF);

    ////////////////////////////////////  FEATURE DESCRIPTION /////////////////////////////////////////////
    private final DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    private final DescriptorExtractor siftExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
    private final DescriptorExtractor surfExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);

    ///////////////////////////////////  FEATURE MATCHING //////////////////////////////////////////////////
    private final DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);


    private Object currentCalcKey;
    private boolean isCalculating = false, isMatching = false;
    //Matrizen zum speichern der Features und der Deskriptoren
    private MatOfKeyPoint teilBildFeatures = new MatOfKeyPoint();
    private Mat teilBildDescriptor = new Mat();
    //TESTDATA
    //param file for ORB-Detection

    private String orbParamsFileName = "orb_params.yml";


    private Mat outImage = new Mat();
    Mat centreTrans = new Mat(1, 1, CvType.CV_32FC2);
    Mat centreOrigin = new Mat(1, 1, CvType.CV_32FC2);
    MatOfPoint mIntSceneCorners = new MatOfPoint();
    private MatOfDMatch matches = new MatOfDMatch();
    private MatOfDMatch goodMatches = new MatOfDMatch();
    private MatOfDMatch better_matches_mat = new MatOfDMatch();
    private Mat testOriginalMat = new Mat();
    int matchesCount = 0;
    //SECOND TESTCASE

    /**
     * @param activity OpenCvActivity, für die der Filter angewendet werden soll.
     */
    public StudentDetectionFilter(final OpenCvActivity activity, OnCalculationCompleted listener) {


        this.activity = activity;
        final OnCalculationCompleted calculationCompletedListener = listener;


        //     ResourceManager.getInstance().saveFile(orbParamsFileName, "%YAML:1.0\nscaleFactor: 1.2\nnLevels: 8\nfirstLevel: 0 \nedgeThreshold: 31\npatchSize: 31\nWTA_K: 2\nscoreType: 0\nnFeatures: 500\n");

        final Map<Object, Source> sources = ResourceManager.getInstance().getModeResources();


        //Berechnungen werden asynchron durchgeführt, da sie je nach gerät länger dauern könnten
        AsyncTask<Void, Integer, Void> calculationTask = new AsyncTask<Void, Integer, Void>() {

            @Override
            protected Void doInBackground(Void[] params) {
                isCalculating = true;


                Object currentSourceKey = activity.getCurrentSourceId();
                Source currentSource = sources.get(currentSourceKey);

                currentCalcKey = currentSourceKey;


                if (currentSource.getFeatures() == null && currentSource.getDescriptors() == null) {

                    Mat loadedDescriptors = ResourceManager.getInstance().readDescriptors(currentSourceKey);
                    MatOfKeyPoint loadedFeatures = ResourceManager.getInstance().readFeatures(currentSourceKey);

                    //Allokiere Speicher für die Matobjekte
                    currentSource.setOriginalMat(new Mat());
                    currentSource.setFeatures(new MatOfKeyPoint());
                    currentSource.setDescriptors(new Mat());

                    //Wandle die original bitmap in eine Graustufen-Matrix um und speichere im Objekt ab

                    Log.d(TAG, "originalBitmapSize: " + currentSource.getOriginalImage().getHeight() + " " + currentSource.getOriginalImage().getWidth());
                    Bitmap bmp32 = currentSource.getOriginalImage().copy(Bitmap.Config.ARGB_8888, true);
                    Utils.bitmapToMat(bmp32, currentSource.getOriginalMat());
                    Imgproc.cvtColor(currentSource.getOriginalMat(), currentSource.getOriginalMat(), Imgproc.COLOR_RGBA2GRAY);


                    Log.d(TAG, "Berechne Deskr. für " + currentSourceKey.toString());
                    publishProgress(50);
                    //suche Features im originalBild und speichere diese ab
                    if (loadedFeatures != null) {
                        currentSource.setFeatures(loadedFeatures);
                    } else {
                        siftFeatureDetector.detect(currentSource.getOriginalMat(), currentSource.getFeatures());

                        if (ResourceManager.getInstance().writeMat(currentSource.getFeatures(), currentSourceKey)) {
                            Log.d(TAG, "Mat " + currentSourceKey + " gespeichert");
                        } else {
                            throw new RuntimeException("Da ist wohl was schiefgegangen beim Features speichern");
                        }
                    }
                    publishProgress(50);
                    //beschreibe die Features im Orginalbild und speichere diese ab

                    if (loadedDescriptors != null) {
                        currentSource.setDescriptors(loadedDescriptors);
                        Log.d(TAG, "Deskriptoren aus Speicher geladen.");
                    } else {
                        siftExtractor.compute(currentSource.getOriginalMat(), currentSource.getFeatures(), currentSource.getDescriptors());
                        //Speichere Deskriptoren
                        if (ResourceManager.getInstance().writeMat(currentSource.getDescriptors(), currentSourceKey)) {
                            Log.d(TAG, "Mat " + currentSourceKey + " gespeichert");
                        } else {
                            throw new RuntimeException("Da ist wohl was schiefgegangen beim Deskriptor speichern");
                        }
                    }


                    Log.d(TAG, "Fertig mit " + currentSourceKey.toString());

                }


                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                calculationCompletedListener.setProgress(values[0], "Berechne Features und Deskriptoren " + currentCalcKey.toString());

            }


            //after executing the code in the thread
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                isCalculating = false;
                calculationCompletedListener.onCalculationCompleted();


            }
        }.execute();

    }


    public StudentDetectionFilter() {

    }

    @Override
    public List<Polygon> apply(Mat src) {

        //Hole aktuelles Source-Object aus dem Res. Manager
        Source currentSourceObject = ResourceManager.getInstance().getModeResources().get(currentCalcKey.toString());

        Point rectCentre = new Point(src.width() / 2, src.height() / 2);

        int radius = 150;

        Rect boundingRect = new Rect(((int) rectCentre.x) - radius, ((int) rectCentre.y) - radius, radius * 2, radius * 2);


        //Region of interest
        Mat imageROI = new Mat(src, boundingRect);

        //detect teilFeatures
        siftFeatureDetector.detect(imageROI, teilBildFeatures);
        //describe
        siftExtractor.compute(imageROI, teilBildFeatures, teilBildDescriptor);


        //Liste für ergebnisse der K-nearest-neighbour suche
        List<MatOfDMatch> knnMatchesList = new ArrayList<>();

        if (!teilBildDescriptor.empty() && !currentSourceObject.getDescriptors().empty()) {
            //Matche Deskriptoren sets und schreibe in knnMatch liste
            descriptorMatcher.knnMatch(teilBildDescriptor, currentSourceObject.getDescriptors(), knnMatchesList, 2);

        } else {

            Snackbar.make(activity.findViewById(android.R.id.content), "No Descriptors found. Try Again?",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    })
                    .show();

            return null;

        }

        // ratio test
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (MatOfDMatch matOfDMatch : knnMatchesList) {
            if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 1) {
                good_matches.add(matOfDMatch.toArray()[0]);

            }
        }

        // get keypoint coordinates of good matches to find homography and remove outliers using ransac
        List<Point> scenePointsList = new ArrayList<Point>();
        List<Point> targetPointsList = new ArrayList<Point>();
        for (int i = 0; i < good_matches.size(); i++) {
            scenePointsList.add(teilBildFeatures.toList().get(good_matches.get(i).queryIdx).pt);
            targetPointsList.add(currentSourceObject.getFeatures().toList().get(good_matches.get(i).trainIdx).pt);
        }

        List<Polygon> visiblePolygons = new ArrayList<>();

        if (!scenePointsList.isEmpty() && !targetPointsList.isEmpty()) {

            if (scenePointsList.size() < 4 || targetPointsList.size() < 4) {
                //nicht genug punk
                return null;
            } else {
                //Umwandlung der Datentypen zur weiteren Verarbeitung
                Mat outputMask = new Mat();
                MatOfPoint2f scenePointsMat = new MatOfPoint2f();
                scenePointsMat.fromList(scenePointsList);
                MatOfPoint2f targetPointsMat = new MatOfPoint2f();
                targetPointsMat.fromList(targetPointsList);

                //Finde die Transformationsmatrix der übrig gebliebenen Features der Scene zu denen im Urbild
                Mat Homog = Calib3d.findHomography(scenePointsMat, targetPointsMat, Calib3d.RANSAC, 5, outputMask, 2000, 0.995);

                if (!Homog.empty()) {



                    Mat sceneCorners = new Mat(4, 1, CvType.CV_32FC2);
                    Mat targetCorners = new Mat(4, 1, CvType.CV_32FC2);


                    //Define the scene in Corner coordinates to find the target region
                    sceneCorners.put(0, 0, new double[]{0, 0});
                    sceneCorners.put(1, 0, new double[]{imageROI.cols(), 0});
                    sceneCorners.put(2, 0, new double[]{imageROI.cols(), imageROI.rows()});
                    sceneCorners.put(3, 0, new double[]{0, imageROI.rows()});


                    //Perfom perspective transformation to find targetcorners
                    Core.perspectiveTransform(sceneCorners, targetCorners, Homog);

                    targetCorners.convertTo(mIntSceneCorners, CvType.CV_32S);

                    if (!Imgproc.isContourConvex(mIntSceneCorners)) {
                        return null;
                    }

                    mIntSceneCorners.release();

                    List<Point> targetCornersList = new LinkedList<>();
                    targetCornersList.add(new Point(targetCorners.get(0, 0)));
                    targetCornersList.add(new Point(targetCorners.get(1, 0)));
                    targetCornersList.add(new Point(targetCorners.get(2, 0)));
                    targetCornersList.add(new Point(targetCorners.get(3, 0)));

                    //  System.out.println("TARGET CORNERS : " + targetCornersList.toString());

                    List<Polygon> referencePolygons = currentSourceObject.getShapes();

                    double smallestDistance = Double.MAX_VALUE;
                    Polygon resultPoly;
                    Rect targetRect = new Rect(new Point(targetCorners.get(0, 0)), new Point(targetCorners.get(2, 0)));

                    Mat inverseMat = Homog.inv();


                    for (Polygon poly : referencePolygons) {

//                        Log.d(TAG, "vorher " + poly.getxPos() + "/" + poly.getyPos());
//                        if(poly.isVisible(targetRect)){

//                            visiblePolygons.add(new Polygon(centreTransPoint.x, centreTransPoint.y, null, poly.getInformation()));
//
//                            centreTrans.release();
//                            centreOrigin.release();
//                        }


//                        int linesVisible =0;
//                        float tmpDistance = 0;
//
//                        float deltaX, deltaY;
//
//                        //y2 - y1 ...
//                        deltaX = (float) poly.getxPos() - (float) polyCenter.x;
//                        deltaY = (float) poly.getyPos() - (float) polyCenter.y;
//
//                        tmpDistance = (float) Math.sqrt(Math.pow(deltaY, 2) - Math.pow(deltaX, 2));
//
//                        if (tmpDistance < smallestDistance) {
//                            smallestDistance = tmpDistance;
//                            resultPoly = poly;
//                        }
                        if (poly.isVisible(targetRect))
                            resultPoly = poly;
                        else
                            resultPoly = null;


                        if (resultPoly != null) {

                            centreOrigin.put(0, 0, new double[]{resultPoly.getxPos(), resultPoly.getyPos()});
                            Core.perspectiveTransform(centreOrigin, centreTrans, inverseMat);
                            Point centreTransPoint = new Point(centreTrans.get(0, 0));
                            Log.d(TAG, "ist drin!:  " + resultPoly.getInformationAsText() + " " + centreTransPoint.x + "/" + centreTransPoint.y);
                            visiblePolygons.add(new Polygon(centreTransPoint.x, centreTransPoint.y, null, resultPoly.getInformation()));

                        }
                    }
//                    inverseMat.release();

//                    if (resultPoly != null)
//                        return resultPoly.getInformation();

                    //                if (resultPoly != null) {
////                    Log.d(TAG, "centre of target: " + polyCenter);
////                    Log.d(TAG, "result x : " + resultPoly.getxPos() + " result y : " + resultPoly.getyPos());
////                    Log.d(TAG, "result smallest distance " + smallestDistance);
////
////                    ((OpenCvActivity) activity).tellResultToUser(resultPoly);
//                }
//
//
//                // outputMask contains zeros and ones indicating which matches are filtered
//                LinkedList<DMatch> better_matches = new LinkedList<DMatch>();
//                for (int i = 0; i < good_matches.size(); i++) {
//                    if (outputMask.get(i, 0)[0] != 0.0) {
//                        better_matches.add(good_matches.get(i));
//                    }
//                }
//
//
//                if (!better_matches.isEmpty()) {
//                    better_matches_mat.fromList(better_matches);
////
////                Imgproc.cvtColor(currentSourceObject.getOriginalMat(), currentSourceObject.getOriginalMat(), Imgproc.COLOR_GRAY2RGB);
////                Imgproc.cvtColor(src, src, Imgproc.COLOR_GRAY2RGB);
//
//
//                    Mat copyOfOriginal = currentSourceObject.getOriginalMat().clone();
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(0, 0)), new Point(targetCorners.get(1, 0)), new Scalar(255, 255, 255), 4);
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(1, 0)), new Point(targetCorners.get(2, 0)), new Scalar(0, 255, 0), 4);
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(2, 0)), new Point(targetCorners.get(3, 0)), new Scalar(0, 255, 0), 4);
//                    Imgproc.line(copyOfOriginal, new Point(targetCorners.get(3, 0)), new Point(targetCorners.get(0, 0)), new Scalar(0, 255, 0), 4);
//                    Imgproc.drawMarker(imageROI, rectCentre, new Scalar(255,0,0));
//                    Features2d.drawMatches(imageROI, teilBildFeatures, copyOfOriginal, currentSourceObject.getFeatures(), better_matches_mat, outImage);
//
//                    Bitmap matchBitMap = Bitmap.createBitmap(outImage.cols(), outImage.rows(), Bitmap.Config.ARGB_8888);
//
//                    Utils.matToBitmap(outImage, matchBitMap);
//
//
//                        ResourceManager.saveImageToExternalStorage(activity, matchBitMap);
////
//
//
//                    //mImg.setImageBitmap(matchBitMap);
//                }

                }

                //release allocated memory

                imageROI.release();
                Homog.release();
                outputMask.release();
                scenePointsMat.release();
                targetPointsMat.release();
            }
        } else {
            Snackbar.make(activity.findViewById(android.R.id.content), "keine stabilen Punkte gefunden",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                        }
                    })
                    .show();
        }
//
//        fastFeatureDetector.detect(src, sceneKeyPoints);
//
//        Imgproc.cvtColor(dst, rgbScene, Imgproc.COLOR_RGBA2RGB);
//        Features2d.drawKeypoints(rgbScene, sceneKeyPoints, rgbScene);
//        Imgproc.cvtColor(rgbScene, dst, Imgproc.COLOR_RGB2RGBA);
        return visiblePolygons;
    }

    /**
     * @param corners list of corners of a given polygon
     * @return the center of this polygon
     */
    private Point calculateCenterOfPolygon(List<Point> corners) {
        double x = 0.;
        double y = 0.;
        int pointCount = corners.size();
        for (int i = 0; i <= pointCount - 1; i++) {
            final Point point = corners.get(i);
            x += point.x;
            y += point.y;
        }

        x = x / pointCount;
        y = y / pointCount;

        return new Point(x, y);
    }


}
