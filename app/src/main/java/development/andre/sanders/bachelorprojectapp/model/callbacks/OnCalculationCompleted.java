package development.andre.sanders.bachelorprojectapp.model.callbacks;

/**
 * Created by andre on 21.07.17.
 */

public interface OnCalculationCompleted {
    void onCalculationCompleted();
    void setProgress(Integer progress, String message);
}
