package development.andre.sanders.bachelorprojectapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import development.andre.sanders.bachelorprojectapp.R;
import development.andre.sanders.bachelorprojectapp.view.customfragments.ExpandableListAdapter;

/**
 * Created by andre on 02.07.17.
 * <p>
 * Erste Activity, die bei Appstart angezeigt wird. Hier werden die Berechtigungen erfragt
 * und auf weitere Optionen verwiesen.
 */

public class StartActivity extends AppCompatActivity {

    //variablen und Konstanten
    private static final String TAG = "StartActivity.class";

    //identifier code für permission request
    public static final int MULTIPLE_PERMISSIONS = 10;

    //Alle Berechtigungen, die wir brauchen
    String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };


    /**
     * @param savedInstanceState OnCreate-Method. Hier wird die View erstellt..
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // listview
        ExpandableListView expListView = (ExpandableListView) findViewById(R.id.expListView);

        //alle mögliche Appmodi
        final List<String> listDataHeader = new ArrayList<>();
        listDataHeader.add("Wachsbleiche");
        listDataHeader.add("Museum");


        final HashMap<String, List<String>> childList = new HashMap<>();
        //alle möglichen Source-Objekte hinzufügen
        List<String> wachsbleicheChild = new ArrayList<>();
        wachsbleicheChild.add("2016");
        wachsbleicheChild.add("2015");
        wachsbleicheChild.add("2014");


        childList.put(listDataHeader.get(0), wachsbleicheChild);

        List<String> museumChild = new ArrayList<>();
        museumChild.add("banksy");

        //Hier die Gemälde rein

        childList.put(listDataHeader.get(1), museumChild);


        ExpandableListAdapter listAdapter = new ExpandableListAdapter(this, listDataHeader, childList);
        expListView.setAdapter(listAdapter);
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                Intent intent;
                intent = new Intent(getApplicationContext(), LoadingActivity.class);
                String appMode = "";
                if (listDataHeader.get(groupPosition).equals("Wachsbleiche"))
                    appMode = "studentMode";

                else if (listDataHeader.get(groupPosition).equalsIgnoreCase("Museum"))
                    appMode = "museumMode";


                String sourceObjId;
                sourceObjId = childList.get(listDataHeader.get(groupPosition)).get(
                        childPosition);

                Toast.makeText(
                        getApplicationContext(),
                        listDataHeader.get(groupPosition)
                                + " : "
                                + childList.get(
                                listDataHeader.get(groupPosition)).get(
                                childPosition), Toast.LENGTH_SHORT)
                        .show();
                intent.putExtra("appMode", appMode);
                intent.putExtra("sourceObject", sourceObjId);
                startActivity(intent);


                return false;
            }
        });


    }


    /**
     * Hier wird nach den Berechtigungen gefragt.
     */
    @Override
    protected void onStart() {
        super.onStart();

        checkAllPermissions();


    }

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

        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case MULTIPLE_PERMISSIONS:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);


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
}
