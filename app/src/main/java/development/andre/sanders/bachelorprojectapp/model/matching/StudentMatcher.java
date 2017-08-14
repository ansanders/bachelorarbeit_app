package development.andre.sanders.bachelorprojectapp.model.matching;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

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
import java.util.concurrent.TimeUnit;

import development.andre.sanders.bachelorprojectapp.model.callbacks.CalculationListener;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;

/**
 * Created by andre on 17.04.17.
 * <p>
 * Detection-Matcher, der auf ein Frame angewendet werden kann und die entsprechenden
 * Informationen als output liefern soll.
 */

public class StudentMatcher implements Matcher {

    //class-tag
    private final static String TAG = "FacedetectionFilter";


    private CalculationListener calculationListener;


    ////////////////////////////////////// FEATURE DETECTION /////////////////////////////////////////////
    private final FeatureDetector orbFeatureDetector = FeatureDetector.create(FeatureDetector.ORB);
    private final FeatureDetector siftFeatureDetector = FeatureDetector.create(FeatureDetector.SIFT);
    private final FeatureDetector surfFeatureDetector = FeatureDetector.create(FeatureDetector.SURF);

    ////////////////////////////////////  FEATURE DESCRIPTION /////////////////////////////////////////////
    private final DescriptorExtractor orbExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
    private final DescriptorExtractor siftExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
    private final DescriptorExtractor surfExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);

    ///////////////////////////////////  FEATURE MATCHING //////////////////////////////////////////////////
    private final DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);


    //Matrizen zum speichern der Features und der Deskriptoren
    private MatOfKeyPoint teilBildFeatures = new MatOfKeyPoint();
    private Mat teilBildDescriptor = new Mat();

    private Mat sceneCorners = new Mat(4, 1, CvType.CV_32FC2);
    private Mat targetCorners = new Mat(4, 1, CvType.CV_32FC2);

    private Mat outputMask = new Mat();
    private MatOfPoint2f scenePointsMat = new MatOfPoint2f();
    private MatOfPoint2f targetPointsMat = new MatOfPoint2f();
    private Source currentSource = null;


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


    public StudentMatcher(CalculationListener listener1) {

        //Listener für Userfeedback

        calculationListener = listener1;


        //     ResourceManager.getInstance().saveFile(orbParamsFileName, "%YAML:1.0\nscaleFactor: 1.2\nnLevels: 8\nfirstLevel: 0 \nedgeThreshold: 31\npatchSize: 31\nWTA_K: 2\nscoreType: 0\nnFeatures: 500\n");


        //Berechnungen werden asynchron durchgeführt, da sie je nach gerät länger dauern könnten
        AsyncTask<Void, Integer, Void> calculationTask = new AsyncTask<Void, Integer, Void>() {

            @Override
            protected Void doInBackground(Void[] params) {
                currentSource = ResourceManager.getInstance().getCurrentSource();
                String sourceName = currentSource.getSourceId().toString();


                if (currentSource.getFeatures() == null && currentSource.getDescriptors() == null) {

                    Mat loadedDescriptors = ResourceManager.getInstance().readDescriptors(sourceName);
                    MatOfKeyPoint loadedFeatures = ResourceManager.getInstance().readFeatures(sourceName);

                    //Allokiere Speicher für die Matobjekte
                    currentSource.setOriginalMat(new Mat());
                    currentSource.setFeatures(new MatOfKeyPoint());
                    currentSource.setDescriptors(new Mat());

                    //Wandle die original bitmap in eine Graustufen-Matrix um und speichere im Objekt ab

                    Log.d(TAG, "originalBitmapSize: " + currentSource.getOriginalImage().getHeight() + " " + currentSource.getOriginalImage().getWidth());
                    Bitmap bmp32 = currentSource.getOriginalImage().copy(Bitmap.Config.ARGB_8888, true);
                    Utils.bitmapToMat(bmp32, currentSource.getOriginalMat());
                    Imgproc.cvtColor(currentSource.getOriginalMat(), currentSource.getOriginalMat(), Imgproc.COLOR_RGBA2GRAY);


                    Log.d(TAG, "Berechne Deskr. für " + currentSource.getSourceId());
                    publishProgress(50);
                    //suche Features im originalBild und speichere diese ab
                    if (loadedFeatures != null) {
                        currentSource.setFeatures(loadedFeatures);
                    } else {
                        siftFeatureDetector.detect(currentSource.getOriginalMat(), currentSource.getFeatures());

                        if (ResourceManager.getInstance().writeMat(currentSource.getFeatures(), sourceName)) {
                            Log.d(TAG, "Mat " + sourceName + " gespeichert");
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
                        if (ResourceManager.getInstance().writeMat(currentSource.getDescriptors(), sourceName)) {
                            Log.d(TAG, "Mat " + sourceName + " gespeichert");
                        } else {
                            throw new RuntimeException("Da ist wohl was schiefgegangen beim Deskriptor speichern");
                        }
                    }


                    Log.d(TAG, "Fertig mit " + sourceName);

                }


                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                calculationListener.showCalculationStatus(values[0], "Berechne Features und Deskriptoren " + currentSource.getSourceId());

            }


            //after executing the code in the thread
            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                calculationListener.onCalculationCompleted();


            }
        }.execute();

    }


    public StudentMatcher() {

    }

    @Override
    public List<Polygon> match(Mat src) {
        long startTime, elapsedTime;
        List<Polygon> visiblePolygons = new ArrayList<>();


        Mat imageROI = src;

//
//        Point rectCentre = new Point(src.width() / 2, src.height() / 2);
//
//        int radius = 300;
//
//        Rect boundingRect = new Rect(((int) rectCentre.x) - radius, ((int) rectCentre.y) - radius, radius * 2, radius * 2);
//
//
//        //Region of interest
//        Mat imageROI = new Mat(src, boundingRect);
        startTime = System.nanoTime();

        //detect teilFeatures
        siftFeatureDetector.detect(imageROI, teilBildFeatures);
        //describe

        elapsedTime = System.nanoTime() - startTime;

        Log.d(TAG, "FeatureDetection took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");
        if (teilBildFeatures.empty()) {
            Log.d(TAG, "teilFeatures empty");
            return null;
        }

        startTime = System.nanoTime();
        siftExtractor.compute(imageROI, teilBildFeatures, teilBildDescriptor);

        elapsedTime = System.nanoTime() - startTime;
        Log.d(TAG, "FeatureDescription took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

        //Liste für ergaebnisse der K-nearest-neighbour suche
        List<MatOfDMatch> knnMatchesList = new ArrayList<>();

        if (!teilBildDescriptor.empty() && !currentSource.getDescriptors().empty()) {
            //Matche Deskriptoren sets und schreibe in knnMatch liste
            startTime = System.nanoTime();
            descriptorMatcher.knnMatch(teilBildDescriptor, currentSource.getDescriptors(), knnMatchesList, 2);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TAG, "Matching took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

        } else {


            return null;

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
        List<Point> scenePointsList = new ArrayList<Point>();
        List<Point> targetPointsList = new ArrayList<Point>();
        for (int i = 0; i < good_matches.size(); i++) {
            scenePointsList.add(teilBildFeatures.toList().get(good_matches.get(i).queryIdx).pt);
            targetPointsList.add(currentSource.getFeatures().toList().get(good_matches.get(i).trainIdx).pt);
        }


        if (!scenePointsList.isEmpty() && !targetPointsList.isEmpty()) {

            if (scenePointsList.size() < 4 || targetPointsList.size() < 4) {
                Log.d(TAG, "nicht genug Punkte für Transformation");
                //nicht genug punk
                return null;
            } else {
                //Umwandlung der Datentypen zur weiteren Verarbeitung

                scenePointsMat.fromList(scenePointsList);
                targetPointsMat.fromList(targetPointsList);

                startTime = System.nanoTime();
                //Finde die Transformationsmatrix der übrig gebliebenen Features der Scene zu denen im Urbild
                Mat Homog = Calib3d.findHomography(scenePointsMat, targetPointsMat, Calib3d.RANSAC, 2, outputMask, 2000, 0.995);
                elapsedTime = System.nanoTime() - startTime;
                Log.d(TAG, "Trans.matrix calc took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                if (!Homog.empty()) {


                    //Define the scene in Corner coordinates to find the target region
                    sceneCorners.put(0, 0, new double[]{0, 0});
                    sceneCorners.put(1, 0, new double[]{imageROI.cols(), 0});
                    sceneCorners.put(2, 0, new double[]{imageROI.cols(), imageROI.rows()});
                    sceneCorners.put(3, 0, new double[]{0, imageROI.rows()});

                    startTime = System.nanoTime();
                    //Perfom perspective transformation to find targetcorners
                    Core.perspectiveTransform(sceneCorners, targetCorners, Homog);
                    elapsedTime = System.nanoTime() - startTime;

                    Log.d(TAG, "Perspectivetransform took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "Milliseconds");

                    targetCorners.convertTo(mIntSceneCorners, CvType.CV_32S);

                    startTime = System.nanoTime();
                    if (!Imgproc.isContourConvex(mIntSceneCorners)) {
                        return null;
                    }
                    elapsedTime = System.nanoTime() - startTime;
                    Log.d(TAG, "ConvexCheck took : " + TimeUnit.MILLISECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) + "milliSeconds");

                    mIntSceneCorners.release();

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

                            //Log.d(TAG, "ist drin!:  " + resultPoly.getInformationAsText() + " " + centreTransPoint.x + "/" + centreTransPoint.y);
                            visiblePolygons.add(new Polygon(centreTransPoint.x, centreTransPoint.y, null, resultPoly.getInformation()));

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
//                    MatStorageUtils.matToBitmap(outImage, matchBitMap);
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


            }
        } else {
            return null;
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
