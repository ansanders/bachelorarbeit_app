package development.andre.sanders.bachelorprojectapp.model.data;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by andre on 07.07.17.
 * <p>
 * Repräsentation eines Quellbildes samt hinterlegter Polygone und ihrer Informationen
 */

public class Source {

    //Das Quellbild. Nur bei erstem start zu Featureberechnung gesetzt. Danach null
    private Bitmap originalImage;

    //Mat Repräsentation des Originalbildes
    private Mat originalMat;
    //Object id- year, or name, or id....
    private String sourceId;

    //Map aller shapes, die das Bild beinhaltet. Irgendein Objektschlüssel
    private List<Polygon> shapes = new ArrayList<>();

    //Merkmale des Bildes
    private MatOfKeyPoint features;

    //Deskriptoren des Bildes
    private Mat descriptors;


    //Default C'tor
    public Source() {

    }

    public Source(String sourceId, Bitmap originalImage, List<Polygon> shapes) {
        this.originalImage = originalImage;
        this.sourceId = sourceId;
        this.shapes = shapes;

    }


    /**
     * release all allocated memory for mat objects
     */
    public void releaseAll() {
        originalMat.release();
        features.release();
        descriptors.release();
    }


    public Bitmap getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(Bitmap originalImage) {
        this.originalImage = originalImage;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public List<Polygon> getShapes() {
        return shapes;
    }

    public void setShapes(List<Polygon> shapes) {
        this.shapes = shapes;
    }

    public MatOfKeyPoint getFeatures() {
        return features;
    }

    public void setFeatures(MatOfKeyPoint features) {
        this.features = features;
    }

    public Mat getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(Mat descriptors) {
        this.descriptors = descriptors;
    }

    public Mat getOriginalMat() {
        return originalMat;
    }

    public void setOriginalMat(Mat originalMat) {
        this.originalMat = originalMat;
    }
}
