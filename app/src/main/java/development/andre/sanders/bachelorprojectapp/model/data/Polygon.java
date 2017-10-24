package development.andre.sanders.bachelorprojectapp.model.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andre on 07.07.17.
 * this class represents a known polygon include all known information of it
 */

public class Polygon implements Serializable {

    //region codes to identify relative location
    private static final int INSIDE = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int BOTTOM = 4;
    private static final int TOP = 8;


    //information of this polygon
    private JSONObject information = null;

    //Kanten dieses Polygons
    private List<Point> corners = new ArrayList<>();
    //Mittelpunkt dieses Polygons
    private double xPos, yPos;

    //Default C'tor
    public Polygon() {

    }


    public Polygon(double xPos, double yPos, List<Point> corners, JSONObject information) {
        this.information = information;
        this.xPos = xPos;
        this.yPos = yPos;
        this.corners = corners;

    }


    /**
     * @param view Der Bildausschnitt der gecheckt werden soll
     * @return true, wenn der Mittelpunkt des Polygons im Rechtecht (Bildausschnitt) liegt
     */
    public boolean isVisible(Rect view) {

//        int cornersInside = 0;
//
//        for (Point point : corners) {
//            int rc = getRegionCode(point, view);
//            if (rc == INSIDE)
//                cornersInside++;
//            if(rc != INSIDE)
//                return false;
//
//        }

       // return cornersInside == corners.size();


        return xPos > view.tl().x && xPos <= view.br().x && yPos < view.br().y && yPos > view.tl().y;
    }

    /**
     *
     * @param point to get the region code for
     * @param clippingView view to define the region of point
     * @return regioncode of point
     */
    private int getRegionCode(Point point, Rect clippingView) {
        int code = xPos < clippingView.tl().x
                ? LEFT
                : xPos > clippingView.br().x
                ? RIGHT
                : INSIDE;
        if (yPos > clippingView.br().y) code |= BOTTOM;
        else if (yPos < clippingView.tl().y) code |= TOP;
        return code;
    }

    public JSONObject getInformation() {
        return information;
    }

    /**
     * Diese Methode liefert
     *
     * @return die Polygoninformation als zusammenhängenden Text.
     */
    public String getInformationAsText() {
        StringBuilder string = new StringBuilder();

        try {

            Iterator<?> keys = information.keys();

            while (keys.hasNext()) {
                String key = (String) keys.next();
                if (information.get(key) instanceof JSONObject) {
                    //nothing
                } else {

                    string.append(information.get(key));
                    string.append(" ");
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return string.toString();
    }

    public void setInformation(JSONObject information) {
        this.information = information;
    }

    public List<Point> getCorners() {
        return corners;
    }

    public void setCorners(List<Point> corners) {
        this.corners = corners;
    }

    public double getxPos() {
        return xPos;
    }

    public void setxPos(double xPos) {
        this.xPos = xPos;
    }

    public double getyPos() {
        return yPos;
    }

    public void setyPos(double yPos) {
        this.yPos = yPos;
    }
}
