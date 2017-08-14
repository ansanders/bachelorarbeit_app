package development.andre.sanders.bachelorprojectapp.utils;

import android.graphics.Point;

import java.util.List;

/**
 * Created by andre on 13.08.17.
 */

public class CalcUtils {


    public static Point calculateCenterOfPolygon(List<Point> corners) {
        int x = 0;
        int y = 0;
        int pointCount = corners.size();
        for (int i = 0; i <= pointCount - 1; i++) {
            final Point point = corners.get(i);
            x += point.x;
            y += point.y;
        }

        x = x / pointCount;
        y = y / pointCount;

        return new Point(x, y);
    }

}
