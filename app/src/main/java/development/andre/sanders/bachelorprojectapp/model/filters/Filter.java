package development.andre.sanders.bachelorprojectapp.model.filters;

import org.opencv.core.Mat;

import java.util.List;

import development.andre.sanders.bachelorprojectapp.model.data.Polygon;

/**
 * Created by andre on 17.04.17.
 */

public interface Filter {
    List<Polygon> apply(final Mat src);
}
