package development.andre.sanders.bachelorprojectapp.model.callbacks;

/**
 * Created by andre on 11.08.17.
 */

public interface CalculationListener {
    void onCalculationCompleted();
    void onCalculationUpdate(Integer progress, String message);
    void showProgressDialog(String title);
}
