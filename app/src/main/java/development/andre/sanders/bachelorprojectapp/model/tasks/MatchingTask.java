package development.andre.sanders.bachelorprojectapp.model.tasks;

import android.os.AsyncTask;

import org.opencv.core.Mat;

import java.util.List;

import development.andre.sanders.bachelorprojectapp.model.callbacks.events.MatchingResultEvent;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.manager.EventBus;

/**
 * Created by andre on 13.08.17.
 * Task, der aktuelles src objekt matched.
 */

public class MatchingTask extends AsyncTask <Void, Integer, List<Polygon>>{

    Mat currentSrc;
    public MatchingTask (Mat currentSrc){

        this.currentSrc = currentSrc;

    }






    @Override
    protected List<Polygon> doInBackground(Void... params) {

        return null;
    }

    @Override
    protected void onPostExecute(List<Polygon> result){
        EventBus.getInstance().getBus().post(new MatchingResultEvent(result));

    }
}
