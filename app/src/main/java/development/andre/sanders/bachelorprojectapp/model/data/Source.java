package development.andre.sanders.bachelorprojectapp.model.data;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by andre on 07.07.17.
 * <p>
 * this class represents a source object. A source object contains the original image or the descriptors
 * of it and all the known polygon information of this image.
 *
 */


public class Source {

    private static final String TAG = "Source.class";
    //every original image should get scaled to this resolution
    private static final int REDUCED_SOURCE_WIDTH = 760;
    private static final int REDUCED_SOURCE_HEIGHT = 620;


    //original image. Nessessary for the feature detection
    private Bitmap originalImage;

    //key for indexing this source in any kind of collections
    private String sourceId;

    //List of all known polygons
    private List<Polygon> shapes = new ArrayList<>();

    //keypoints of original image
    private MatOfKeyPoint features;

    //descriptors of original image
    private Mat descriptors;

    //title of the Source
    private String title;

    //published year
    private int published;
    //artist name
    private String artistName;




    //Default C'tor
    public Source() {

    }
    //special c'tor
    public Source(String sourceId, Bitmap originalImage, List<Polygon> shapes) {
        this.originalImage = originalImage;
        this.sourceId = sourceId;
        this.shapes = shapes;

    }


    /**
     * release all allocated memory for mat objects
     */
    public void releaseAll() {
        if(features!= null)
        features.release();

        if(descriptors!= null)
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

    public static int getReducedSourceWidth() {
        return REDUCED_SOURCE_WIDTH;
    }

    public static int getReducedSourceHeight() {
        return REDUCED_SOURCE_HEIGHT;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPublished() {
        return published;
    }

    public void setPublished(int published) {
        this.published = published;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}
