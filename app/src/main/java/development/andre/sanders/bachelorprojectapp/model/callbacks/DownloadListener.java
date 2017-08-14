package development.andre.sanders.bachelorprojectapp.model.callbacks;

/**
 * Created by andre on 21.07.17.
 */

public interface DownloadListener {
    void onDownloadResult();
    void showDownloadStatus(Integer progress, String message);
    void showProgressDialog(String title);

}
