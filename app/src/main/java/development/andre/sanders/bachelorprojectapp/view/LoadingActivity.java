package development.andre.sanders.bachelorprojectapp.view;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import development.andre.sanders.bachelorprojectapp.model.ResourceManager;
import development.andre.sanders.bachelorprojectapp.model.callbacks.OnDownloadCompleted;

/**
 * Created by andre on 12.07.17.
 * <p>
 * HelperActivity to load the content on Appstarts
 */

public class LoadingActivity extends AppCompatActivity implements OnDownloadCompleted {

    private String currentAppMode;
    private String currentSource;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            currentAppMode = null;
            currentSource = null;
        } else {
            currentAppMode = extras.getString("appMode");
            currentSource = extras.getString("sourceObject");
        }

        if (currentAppMode == null || currentSource == null)
            throw new RuntimeException("APPMODE AND WANTED SOURCE MUST BE SET!");


        showProgressDialog("Loading....");

        //ResourceManager lädt alle benötigten Daten.
        ResourceManager.getInstance().init(this, currentAppMode, this);


    }

    /**
     * UI-Callback, der getriggered wird, wenn alle downloads abgeschlossen sind
     */
    @Override
    public void onDownloadCompleted() {
        //Speicher heruntergeladene Daten und zeige neeuen prozess an

        progressDialog.dismiss();

        //start the actvity after download
        Intent intent = new Intent(getApplicationContext(), OpenCvActivity.class);
        intent.putExtra("appMode", currentAppMode);
        intent.putExtra("sourceObject", currentSource);
        startActivity(intent);
    }

    @Override
    public void setProgress(Integer progress, String message) {
        progressDialog.setMessage(message);
        progressDialog.setProgress(progress);
    }

    @Override
    public void showProgressDialog(String title) {

        progressDialog = new ProgressDialog(this);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.setTitle(title);
        progressDialog.setMessage("Bitte warten");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);

        progressDialog.show();
    }


    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    public void setProgressDialog(ProgressDialog progressDialog) {
        this.progressDialog = progressDialog;
    }
}


