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

public class Source{

    //Bitmap representation of original source image.
    private Bitmap originalImage;

    //Mat Representation of original image
    private Mat originalMat;
    //Object id- year, or name, or id....
    private Object sourceId;

    //Map aller shapes, die das Bild beinhaltet. Irgendein Objektschlüssel
    private List<Polygon> shapes = new ArrayList<>();

    //Merkmale des Bildes
    private MatOfKeyPoint features;

    //Deskriptoren des Bildes
    private Mat descriptors;





    //Default C'tor
    public Source() {

    }

    public Source(Object sourceId, Bitmap originalImage, List<Polygon> shapes) {
        this.originalImage = originalImage;
        this.sourceId = sourceId;
        this.shapes = shapes;

    }

    /**
     * @param polygon1
     * @return returns the nearest shape to polygon1
     */
    public Polygon getNearestPolygon(Polygon polygon1) {

        return null;
    }


    /**
     * release all allocated memory for mat objects
     */
    public void releaseAll(){
        originalMat.release();
        features.release();
        descriptors.release();
    }
    /**
     * @param polygon
     * @return return information of specified polygon
     */
    public Object getInformationOfPolygon(Polygon polygon) {
        return null;
    }


    public Bitmap getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(Bitmap originalImage) {
        this.originalImage = originalImage;
    }

    public Object getSourceId() {
        return sourceId;
    }

    public void setSourceId(Object sourceId) {
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
