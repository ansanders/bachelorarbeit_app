package development.andre.sanders.bachelorprojectapp.utils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by andre on 28.07.17.
 */

public class MatHelper {

    private File matDir;

    public MatHelper(File matDir) {
        this.matDir = matDir;
        if (!matDir.exists())
            matDir.mkdirs();


    }

    /**
     * @param matToStore mat die gespeichert werden soll
     * @return true if success
     */
    public boolean writeMat(Mat matToStore) {

        File outputFile = null;


        if (matToStore instanceof MatOfKeyPoint)
            outputFile = new File(matDir, "features_mat.yml");
        else
            outputFile = new File(matDir, "descriptors_mat.yml");

        int cols = matToStore.cols();
        int type = matToStore.type();
        float[] data = new float[(int) matToStore.total() * matToStore.channels()];
        //kopiere data von matToStore in data
        matToStore.get(0, 0, data);


        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            outStream.writeObject(type);
            outStream.writeObject(cols);
            outStream.writeObject(data);
            outStream.close();

        } catch (ClassCastException | IOException e) {
            e.printStackTrace();
            return false;
        }


        return true;


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
                type = (int) inputStream.readObject();
                cols = (int) inputStream.readObject();
                data = (float[]) inputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (data == null || cols == 0)
                return null;

            Mat outPut = new Mat(data.length / cols, cols, type);
            outPut.put(0, 0, data);

            return outPut;
        }
    }

    public MatOfKeyPoint readFeatures() {

        File inputFile = new File(matDir, "features_mat.yml");
        if (!inputFile.exists())
            return null;
        else {
            int type =0;
            int cols = 0;
            float[] data = null;
            try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(inputFile))) {
                type = (int) inputStream.readObject();
                cols = (int) inputStream.readObject();
                data = (float[]) inputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (data == null || cols == 0)
                return null;

            MatOfKeyPoint outPut = new MatOfKeyPoint();
            outPut.create(data.length / cols, cols, type);
            outPut.put(0, 0, data);

            return outPut;
        }
    }

}
