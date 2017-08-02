package development.andre.sanders.bachelorprojectapp.model.callbacks;

/**
 * Created by andre on 21.07.17.
 */

public interface OnDownloadCompleted {
    void onDownloadCompleted();
    void setProgress(Integer progress, String message);
    void showProgressDialog(String title);

}
