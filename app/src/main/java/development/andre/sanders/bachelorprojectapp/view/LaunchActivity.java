package development.andre.sanders.bachelorprojectapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import development.andre.sanders.bachelorprojectapp.R;

/**
 * Created by andre on 28.08.17.
 *
 * This activity is the launching one of the app.
 * First all permissions need to be asked.
 * After that it lets the user scan a qrCode and give the result (appmode and sourceobject)
 * to the LoadingActivity to load all relative data.
 *
 */

public class LaunchActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {


    //Tag to identify activity in logger
    private static final String TAG = "LaunchingActivity.class";

    //identifier code für permission request
    public static final int MULTIPLE_PERMISSIONS = 10;

    //All permissions our app needs
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    //drawer layout
    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private String[] drawerOptions;

    //Button to start the qr scanning
    private Button scanButton;
    //Integrator to get the qr result in our activity
    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seletect_source_activity);
        scanButton = (Button) findViewById(R.id.buttonScan);
        qrScan = new IntentIntegrator(this);
    }

    /**
     * Here the app asks for all the permissions.
     */
    @Override
    protected void onStart() {
        super.onStart();

        checkAllPermissions();

    }

    /**
     * check all the permissions
     * @return true if all permissions are granted
     */
    public boolean checkAllPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);

            return false;
        }
        scanButton.setOnClickListener(this);
        return true;
    }


    //callback for all the permissionrequests
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case MULTIPLE_PERMISSIONS:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                    scanButton.setOnClickListener(this);

                } else {

                    Snackbar.make(this.findViewById(android.R.id.content), "Um alle Features von OpenCV genießen zu können, benötigen wir den Zugriff auf die Kamera und auf den Speicher",
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    checkAllPermissions();
                                }
                            })
                            .show();
                }
                break;


            default:
                break;

        }
    }

    //callback if scannerbutton is clicked
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonScan)
            qrScan.initiateScan();
    }

    //gets called when qr scanner finished scanning
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //if qrcode has nothing in it
            if (result.getContents() == null) {
                Toast.makeText(this, "QR-Code konnte nicht gelesen werden", Toast.LENGTH_LONG).show();
            } else {
                //if qr contains data
                String resultString = result.getContents();
                Log.d(TAG, "QR-Content: " + result.getContents());

                String urlPath = resultString.replaceFirst("/", "");
                int ind = urlPath.lastIndexOf("\"");
                if( ind>=0 )
                    urlPath = new StringBuilder(urlPath).replace(ind, ind+1,"\\\"").toString();


                String[] pathArray = urlPath.split("/");

                if (pathArray.length <= 0) {
                    Toast.makeText(this, "keine passende Query im QR-Code", Toast.LENGTH_LONG).show();
                    System.exit(-1);

                } else {
                    //here we have a success reading the qr code content and can extract the data
                    //to know what data to download

                    Intent intent = new Intent(this, LoadingActivity.class);
                    Toast.makeText(this, "Query gefunden : " + pathArray[0] +  pathArray[1] + " " + pathArray[2], Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Query gefunden : " + pathArray[1] + " " + pathArray[2]);
                    intent.putExtra("appMode", pathArray[1]);
                    intent.putExtra("sourceObject", pathArray[2]);
                    startActivity(intent);
                }

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * helper method to split a url in its query parameters
     * @param url to split
     * @return Map with name-value pairs of the params
     */
    private Map<String, String> splitQuery(URL url) {
        Map<String, String> nameValueMap = new LinkedHashMap<>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int pivot = pair.indexOf("=");
            try {
                nameValueMap.put(URLDecoder.decode(pair.substring(0, pivot), "UTF-8"), URLDecoder.decode(pair.substring(pivot + 1)));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return nameValueMap;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
