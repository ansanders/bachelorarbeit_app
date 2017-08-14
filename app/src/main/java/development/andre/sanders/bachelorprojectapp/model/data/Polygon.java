package development.andre.sanders.bachelorprojectapp.model.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Rect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andre on 07.07.17.
 * Repräsentation eines Polygons samt seiner hinterlegten Information
 */

public class Polygon implements Serializable {

    //Information zu diesem Polygon
    private JSONObject information = null;

    //Kanten dieses Polygons
    private List<Line> edges = new ArrayList<>();
    //Mittelpunkt dieses Polygons
    private double xPos, yPos;

    //Default C'tor
    public Polygon() {

    }


    public Polygon(double xPos, double yPos, List<Line> edges, JSONObject information) {
        this.information = information;
        this.xPos = xPos;
        this.yPos = yPos;
        this.edges = edges;
    }



    /**
     * @param view Der Bildausschnitt der gecheckt werden soll
     * @return true, wenn der Mittelpunkt des Polygons im Rechtecht (Bildausschnitt) liegt
     */
    public boolean isVisible(Rect view) {

        return xPos > view.tl().x && xPos <= view.br().x && yPos < view.br().y && yPos > view.tl().y;
    }

    public JSONObject getInformation(){
        return information;
    }

    /**
     * Diese Methode liefert
     * @return die Polygoninformation als zusammenhängenden Text.
     */
    public String getInformationAsText() {
        StringBuilder string = new StringBuilder();

        try {

            Iterator<?> keys = information.keys();

            while( keys.hasNext() ) {
                String key = (String)keys.next();
                if ( information.get(key) instanceof JSONObject ) {
                    //nothing
                }
                else{
                    string.append(key);
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

    public List<Line> getEdges() {
        return edges;
    }

    public void setEdges(List<Line> edges) {
        this.edges = edges;
    }

    public double getxPos() {
        return xPos;
    }

    public void setxPos(int xPos) {
        this.xPos = xPos;
    }

    public double getyPos() {
        return yPos;
    }

    public void setyPos(int yPos) {
        this.yPos = yPos;
    }
}
