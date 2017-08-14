package development.andre.sanders.bachelorprojectapp.model.callbacks.events;

import java.util.List;

import development.andre.sanders.bachelorprojectapp.model.data.Polygon;

/**
 * Created by andre on 13.08.17.
 */

public class MatchingResultEvent {
    //resultPolygon mit transformierten Koordinaten zur Darstellung für den User
    private List<Polygon> resultPolygons;

    public MatchingResultEvent(List<Polygon> resultPolygons){
        this.resultPolygons = resultPolygons;
    }

    public List<Polygon> getResult(){
        return resultPolygons;
    }
}
