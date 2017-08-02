package development.andre.sanders.bachelorprojectapp.model;

import android.app.Activity;

import development.andre.sanders.bachelorprojectapp.model.filters.Filter;
import development.andre.sanders.bachelorprojectapp.model.filters.MuseumDetectionFilter;
import development.andre.sanders.bachelorprojectapp.model.filters.StudentDetectionFilter;
import development.andre.sanders.bachelorprojectapp.model.callbacks.OnCalculationCompleted;
import development.andre.sanders.bachelorprojectapp.view.OpenCvActivity;

/**
 * Created by andre on 10.07.17.
 *
 * Factory für die Filter
 */

public class FilterFactory {

    private OpenCvActivity activity = null;
    private OnCalculationCompleted listener;
    public FilterFactory(OpenCvActivity application, OnCalculationCompleted listener){
        activity = application;
        this.listener = listener;
    }
    public Filter getFilter(String filterType){
        if(filterType == null){
            return null;
        }

        if(filterType.equalsIgnoreCase("student")){
            return new StudentDetectionFilter(activity, listener);
        }
        else if(filterType.equalsIgnoreCase("museum")){
            return new MuseumDetectionFilter();
        }

        return null;
    }
}
