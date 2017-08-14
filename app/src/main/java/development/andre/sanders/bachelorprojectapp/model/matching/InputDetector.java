package development.andre.sanders.bachelorprojectapp.model.matching;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.utils.Constants;
import development.andre.sanders.bachelorprojectapp.view.OpenCvActivity;

/**
 * Created by andre on 10.08.17.
 * <p>
 * Dieser Detektor soll den gewünschten Userinput erkennen und für das Matching
 * zurechtschneiden.
 */

public class InputDetector {

    private final String TAG = "InputDetector";
    //Cascade classifier für studentenerkennung
    private CascadeClassifier faceDetector;
    //alle gefundenen Objekte im Bild
    private List<Rect> resultDetections = new ArrayList<>();

    //möglich sind Gesichter von Studenten und Polygone auf Gemälden
    private int detectionMode;


    //C'tor
    public InputDetector(String appMode) {


        switch (appMode) {
            case Constants.STUDENT_MODE:

                detectionMode = Constants.INPUT_TYPE_FACE;
                String cascadePath = "";
                try {
                    cascadePath = getCascadePath();
                    Log.d(TAG, "cascade classifier loaded");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (cascadePath == null)
                    throw new RuntimeException("cascade File cant be null");
                faceDetector = new CascadeClassifier(cascadePath);
                break;
            case Constants.MUSEUM_MODE:
                detectionMode = Constants.INPUT_TYPE_CLOSED_SHAPES;
                break;

            default:
                detectionMode = Constants.INPUT_TYPE_FACE;
                break;
        }

    }


    /**
     * @param src source image das gescanned werden soll
     * @return liste aller gefundenen Objekte im Bild
     */
    public List<Rect> scanFaces(Mat src) {

        MatOfRect detections = new MatOfRect();
        faceDetector.detectMultiScale(src, detections);
        Log.d(TAG, String.format("Detected %s faces", detections.toArray().length));

        return detections.toList();
    }

    public List<Rect> scanShapes(Mat src) {
        return new ArrayList<>();
    }

    /**
     * @param src Quelldatei, welche die Bildinformation enthält
     * @return alle gefundenen Regionen von Objekten
     */
    public List<Rect> detectObject(Mat src) {


                switch (detectionMode) {
                    case Constants.INPUT_TYPE_FACE:

                        resultDetections = scanFaces(src);


                    case Constants.INPUT_TYPE_CLOSED_SHAPES:
                        resultDetections = scanShapes(src);


                    default:


                }


                if (resultDetections.isEmpty()) {
                    //throw event


                } else if (resultDetections.toArray().length > 3) {
                   //throw event

                }



            return resultDetections;

    }

    public String getCascadePath() throws IOException {
        File cascadeFile;
        FileOutputStream os = null;
        InputStream is = (OpenCvActivity.getmContext().getResources().openRawResource(R.raw.haarcascade_frontalface_alt));
        File cascadeDir = OpenCvActivity.getmContext().getDir("cascade", Context.MODE_PRIVATE);
        cascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
        try {
            os = new FileOutputStream(cascadeFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        byte[] fileBuffer = new byte[4096];
        int bytesRead;
        if (os != null) {
            while ((bytesRead = is.read(fileBuffer)) != -1) {

                os.write(fileBuffer, 0, bytesRead);

            }

            is.close();
            os.close();

            return cascadeFile.getAbsolutePath();
        }

        return null;
    }

}
