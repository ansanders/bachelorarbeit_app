package development.andre.sanders.bachelorprojectapp.utils;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.opencv.core.CvType.CV_16S;
import static org.opencv.core.CvType.CV_16U;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_32S;
import static org.opencv.core.CvType.CV_64F;
import static org.opencv.core.CvType.CV_8S;
import static org.opencv.core.CvType.CV_8U;

/**
 * Created by andre on 28.07.17.
 *
 * Helper class for MatStorage
 */


public class MatStorageUtils {
    private static final String TAG = "MatStorageUtils";
    private File matDir;

    public MatStorageUtils(File matDir) {
        this.matDir = matDir;
        if (!matDir.exists())
            matDir.mkdirs();


    }

    /**
     * @param matToStore mat die gespeichert werden soll
     * @return true if success
     */
    public boolean writeDescriptorMat(Mat matToStore) {


        File outputFile;

        String matType = printMatType(matToStore);



            outputFile = new File(matDir, "descriptors_mat.yml");
            Log.d(TAG, "Store Descriptor Mat : type: " + matType + "size : " + matToStore.size());


        int cols = matToStore.cols();

        float[] data = new float[(int) matToStore.total() * matToStore.channels()];

        matToStore.get(0, 0, data);


        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            outStream.writeObject(cols);
            outStream.writeObject(data);
            outStream.close();

        } catch (ClassCastException | IOException e) {
            e.printStackTrace();
            return false;
        }


        return true;


    }

    public MatOfKeyPoint featuresFromJson(String jsonString){
        MatOfKeyPoint result = new MatOfKeyPoint();

        JsonParser parser = new JsonParser();
        JsonArray jsonArr = parser.parse(jsonString).getAsJsonArray();

        int size = jsonArr.size();

        KeyPoint[] kpArray = new KeyPoint[size];

        for(int i=0; i<size; i++){
            KeyPoint kp = new KeyPoint();

            JsonObject obj = (JsonObject) jsonArr.get(i);

            kp.pt       = new Point(
                    obj.get("x").getAsDouble(),
                    obj.get("y").getAsDouble()
            );
            kp.class_id = obj.get("class_id").getAsInt();
            kp.size     =     obj.get("size").getAsFloat();
            kp.angle    =    obj.get("angle").getAsFloat();
            kp.octave   =   obj.get("octave").getAsInt();
            kp.response = obj.get("response").getAsFloat();

            kpArray[i] = kp;
        }

        result.fromArray(kpArray);

        return result;
    }

    /**
     *
     * @param mat to store in filestorage
     *            Referenz Stackoverflow : https://stackoverflow.com/questions/21849938/how-to-save-opencv-keypoint-features-to-database
     *
     */
    public String featuresToJson(MatOfKeyPoint mat){
        if(mat!= null && !mat.empty()){

            Gson gson = new Gson();

            JsonArray jsonArr = new JsonArray();

            KeyPoint[] array = mat.toArray();
            for (KeyPoint kp : array) {
                JsonObject obj = new JsonObject();

                obj.addProperty("class_id", kp.class_id);
                obj.addProperty("x", kp.pt.x);
                obj.addProperty("y", kp.pt.y);
                obj.addProperty("size", kp.size);
                obj.addProperty("angle", kp.angle);
                obj.addProperty("octave", kp.octave);
                obj.addProperty("response", kp.response);

                jsonArr.add(obj);
            }

            return gson.toJson(jsonArr);
        }
        else
            return null;
    }

    /*
     * Read a specific mat object from storage
     * type could be Mat, or MatOfKeyPoint
     */
    public Mat readDescriptors() {

        File inputFile = new File(matDir, "descriptors_mat.yml");
        if (!inputFile.exists())
            return null;
        else {
            int cols = 0;
            float[] data = null;
            int type = 0;
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(inputFile))) {
                cols = (int) inputStream.readObject();
                data = (float[]) inputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (data == null || cols == 0)
                return null;

            Mat outPut = new Mat(data.length / cols, cols, CV_32FC1);
            outPut.put(0, 0, data);
            Log.d(TAG, "read Descriptor Mat : type: " + outPut.type() + "size : " + outPut.size());

            return outPut;
        }
    }

    public MatOfKeyPoint readFeatures() {

        File inputFile = new File(matDir, "features_mat.yml");
        if (!inputFile.exists()){
            return null;

        }
        else {
            int cols = 0;
            float[] data = null;
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(inputFile))) {
                cols = (int) inputStream.readObject();
                data = (float[]) inputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (data == null || cols == 0)
                return null;

            MatOfKeyPoint outPut = new MatOfKeyPoint();
            outPut.create(data.length / cols, cols, 53);
            outPut.put(0, 0, data);
            Log.d(TAG, "read Feature Mat : type: " + outPut.type() + "size : " + outPut.size());
            return outPut;
        }
    }

    private String printMatType(Mat checkMat) {
        String r;
        int depth = checkMat.depth();
        int channels = checkMat.channels();

        switch ( depth ) {
            case CV_8U:  r = "8U"; break;
            case CV_8S:  r = "8S"; break;
            case CV_16U: r = "16U"; break;
            case CV_16S: r = "16S"; break;
            case CV_32S: r = "32S"; break;
            case CV_32F: r = "32F"; break;
            case CV_64F: r = "64F"; break;
            default:     r = "User"; break;


        }

        r += "C";
        r += (channels);

        return r;
    }

}
