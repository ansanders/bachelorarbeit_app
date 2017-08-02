package development.andre.sanders.bachelorprojectapp.model.data;


import android.graphics.Point;

import org.opencv.core.Rect;

/**
 * Created by andre on 28.07.17.
 * Hilfsklasse. Repräsentiert eine Linie eines Polygons
 */

public class Line {

    private Point startPoint, endPoint;

    //REGION CODE CONSTANTS
    protected static final byte LEFT = 1;
    protected static final byte RIGHT = 2;
    protected static final byte BOTTOM = 4;
    protected static final byte TOP = 8;

    protected static final byte EMPTY = 0;

    public Line(Point startPoint, Point endPoint){

        if (startPoint.y >= endPoint.y){

            this.startPoint = startPoint;
            this.endPoint = endPoint;
        }
        else{
            this.startPoint = endPoint;
            this.endPoint = startPoint;
        }

    }

    /**
     *
     * @param rect
     * @return
     */
    public boolean isCompletlyInside(Rect rect){
        boolean finite_slope;
        double slope =0.0;
        byte C, C1, C2;
        finite_slope = (startPoint.x != endPoint.x);
        if(finite_slope)
            slope = (double) (endPoint.y - startPoint.y) / (double)(endPoint.x - startPoint.x);

        C1 = getRegionCode(startPoint, rect);
        C2 = getRegionCode(endPoint, rect);

        return C1 == EMPTY && C2 == EMPTY;
    }

    /**
     *
     * gibt den RegionCode zum Clipping rect und Point wieder
     * @param p für den der Code generiert werden soll
     * @param rect zum test
     * @return regioncode of point p
     */
    public byte getRegionCode(Point p, Rect rect){
        byte c;

        c = EMPTY;
        if(p.x <rect.tl().x)
            c = LEFT;
        else if (p.x > rect.br().x)
            c = RIGHT;
        if(p.y < rect.tl().y)
            c /= TOP;
        else if(p.y > rect.br().y)
            c /= BOTTOM;

        return(c);
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }
}
