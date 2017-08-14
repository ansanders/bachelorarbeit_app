package development.andre.sanders.bachelorprojectapp.model.matching;

import org.opencv.core.Mat;

import java.util.List;

import development.andre.sanders.bachelorprojectapp.model.data.Polygon;

/**
 * Created by andre on 17.04.17.
 *
 * Ein Matcher
 */


public interface Matcher {
    List<Polygon> match(final Mat src);
}
