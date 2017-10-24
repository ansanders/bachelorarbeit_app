package development.andre.sanders.bachelorprojectapp.model.tasks;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import development.andre.sanders.bachelorprojectapp.model.callbacks.CalculationListener;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;

/**
 * Created by andre on 14.08.17.
 *
 * Dieser Task bereitet die Source Dateien zum Matching vor, indem er für das aktuelle Bild, wenn noch
 * nicht geschehen, die Features und Deskriptoren berechnet.
 */

public class PrepareMatchingTask extends AsyncTask<Void, Integer, Void> {
    private static final String TAG = "PrepareMatchingTask";
    private FeatureDetector detector;
    private DescriptorExtractor extractor;
    private CalculationListener listener;


    public PrepareMatchingTask(CalculationListener listener, FeatureDetector detector, DescriptorExtractor extractor) {
        super();
        this.listener = listener;
        this.detector = detector;
        this.extractor = extractor;

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.showProgressDialog("Berechne Features und Descriptoren");

    }

    @Override
    protected Void doInBackground(Void... voids) {


        Mat originalMat = new Mat();
        Source currentSource = ResourceManager.getInstance().getCurrentSource();
        String sourceName = currentSource.getSourceId();

        MatOfKeyPoint loadedFeatures = ResourceManager.getInstance().readFeatures(sourceName);
        Mat loadedDescriptors = ResourceManager.getInstance().readDescriptors(sourceName);


        //Allokiere Speicher für die Matobjekte
        currentSource.setFeatures(new MatOfKeyPoint());
        currentSource.setDescriptors(new Mat());

        //Wandle die original bitmap in eine Graustufen-Matrix um und speichere im Objekt ab

        Log.d(TAG, "originalBitmapSize: " + currentSource.getOriginalImage().getHeight() + " " + currentSource.getOriginalImage().getWidth());
        Bitmap bmp32 = currentSource.getOriginalImage().copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, originalMat);
        Imgproc.cvtColor(originalMat, originalMat, Imgproc.COLOR_RGBA2GRAY);


        Log.d(TAG, "Berechne Deskr. für " + currentSource.getSourceId());
        publishProgress(50);
        //suche Features im originalBild und speichere diese ab
        if (loadedFeatures != null) {

            loadedFeatures.copyTo(currentSource.getFeatures());
            Log.d(TAG, "loaded features : size : " + currentSource.getFeatures().size());

            //currentSource.setFeatures(loadedFeatures);
            loadedFeatures.release();
        } else {
            detector.detect(originalMat, currentSource.getFeatures());
            Log.d(TAG, "source features : size : " + currentSource.getFeatures().size());
            if (ResourceManager.getInstance().writeMat(currentSource.getFeatures(), sourceName)) {
                Log.d(TAG, "Feature Mat " + sourceName + " gespeichert");

            } else {
                throw new RuntimeException("Da ist wohl was schiefgegangen beim Features speichern");
            }
        }
        publishProgress(50);
        //beschreibe die Features im Orginalbild und speichere diese ab

        if (loadedDescriptors != null) {
          //  currentSource.setDescriptors(loadedDescriptors);
            loadedDescriptors.copyTo(currentSource.getDescriptors());
            Log.d(TAG, "loaded descriptors : size : " + currentSource.getDescriptors().size());

            loadedDescriptors.release();
            Log.d(TAG, "Deskriptoren aus Speicher geladen.");
        } else {
            extractor.compute(originalMat, currentSource.getFeatures(), currentSource.getDescriptors());
            //Speichere Deskriptoren
            Log.d(TAG, "source descriptors: " + currentSource.getDescriptors().size());
            if (ResourceManager.getInstance().writeMat(currentSource.getDescriptors(), sourceName)) {
                Log.d(TAG, "Deskriptor Mat " + sourceName + " gespeichert");
            } else {
                MatOfKeyPoint test = new MatOfKeyPoint();

                throw new RuntimeException("Da ist wohl was schiefgegangen beim Deskriptor speichern");
            }
        }


        Log.d(TAG, "Fertig mit " + sourceName);



        return null;

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        listener.onCalculationUpdate(values[0], "Berechne Features und Deskriptoren " + ResourceManager.getInstance().getCurrentSource().getSourceId());

    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        //call listener and update ui
        listener.onCalculationCompleted();

    }

}
