package development.andre.sanders.bachelorprojectapp.model.filters;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import development.andre.sanders.bachelorprojectapp.model.data.Polygon;

/**
 * Created by andre on 17.04.17.
 */

public class NoneFilter implements Filter {

    public NoneFilter() {
    }

    @Override
    public List<Polygon> apply(Mat src) {

        return null;

    }
}
