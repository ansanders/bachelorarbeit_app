package development.andre.sanders.bachelorprojectapp.model.data;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Rect;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andre on 07.07.17.
 * Repräsentation eines Polygons samt seiner hinterlegten Information
 */

public class Polygon implements Serializable {

    //Information of this polygon
    private JSONObject information = null;

    //corners of this polygon
    private List<Line> edges = new ArrayList<>();
    //position of this polygon
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
     * @param view the bounding rect to check
     * @return true, if polygon is partial inside the view
     */
    public boolean isVisible(Rect view) {

        return xPos > view.tl().x && xPos <= view.br().x && yPos < view.br().y && yPos > view.tl().y;
    }

    public JSONObject getInformation(){
        return information;
    }

    public String getInformationAsText() {
        StringBuilder string = new StringBuilder();

        try {
            string.append(information.getString("firstname"));
            string.append(System.getProperty("line.separator"));
            string.append(information.getString("lastname"));
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
