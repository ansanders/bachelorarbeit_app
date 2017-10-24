package development.andre.sanders.bachelorprojectapp.utils;


import android.graphics.Bitmap;
import android.graphics.Matrix;

import org.opencv.core.Point;

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

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}
