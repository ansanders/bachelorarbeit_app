package development.andre.sanders.bachelorprojectapp.model.manager;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import development.andre.sanders.bachelorprojectapp.model.callbacks.DownloadListener;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.tasks.DownloadResourceTask;
import development.andre.sanders.bachelorprojectapp.utils.MatStorageUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by andre on 05.07.17.
 * <p>
 * Der Resource Manager, umgesetzt durch das Singleton-Pattern fungiert als Schnittstelle zu den Ressourcen.
 * Hier werden alle benötigten Daten geordert und gespeichert. Der Manager bietet Funktionen zum lesen und schreiben der daten.
 */

public class ResourceManager {
    //Tag
    private static final String TAG = "ResourceManager.class";
    public static final String HOST_STATIC = "http://project2.informatik.uni-osnabrueck.de/ardetection/";

    //Singleton Instanz
    private static ResourceManager instance = null;

    //internal storage directory for the current appmode
    private File modeDirectory;

    //contains all source objects related to current appMode
    private Map<String, Source> modeResources = new HashMap<>();

    //callback for downloadTask
    private DownloadListener downloadListener;

    //current source object where the user is located
    private Source currentSource;

    //current appmode
    private String currentAppMode ="";

    //singleton istance
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }


    /**
     * @param activity init the singleton for this specific appmode
     */
    public void init(Activity activity, String appMode, DownloadListener listener) {

        final Context appContext = activity.getApplicationContext();
        listener.showProgressDialog("Lade alle Daten zu " + appMode);

        //load data if there is another appmode as before
        if(currentAppMode.isEmpty()|| !appMode.equals(currentAppMode))
        {
            //reset the mode relative data for the old appMode
            if (modeResources != null && !modeResources.isEmpty())
                resetData();

            //update appmode
            currentAppMode = appMode;

            //setup the data directory in internal storage for the app
            ContextWrapper contextWrapper = new ContextWrapper(activity.getApplicationContext());
            String fileDir = "data";
            File directory = contextWrapper.getDir(fileDir, Context.MODE_PRIVATE);


            this.downloadListener = listener;

            //shared preferences for small datas
            SharedPreferences prefs = activity.getSharedPreferences(fileDir, MODE_PRIVATE);

            //setup directory for specific appmode
            modeDirectory = new File(directory, currentAppMode);

            //if mode started the first time, download all relative data from server
            if (prefs.getBoolean("firstRun" + currentAppMode, true)) {


                prefs.edit().putBoolean("firstRun" + currentAppMode, false).apply();

                //create physical representation
                boolean status = modeDirectory.mkdir();

                AsyncTask<Void, Void, JSONObject> prepareTask = new AsyncTask<Void, Void, JSONObject>() {
                    @Override
                    protected JSONObject doInBackground(Void... params) {
                        return ResourceManager.getInstance().getListOfAvailableModeResources();
                    }
                    @Override
                    protected void onPostExecute (JSONObject result){
                        //load data from server
                        new DownloadResourceTask(appContext ,downloadListener, result).execute();
                    }
                }.execute();



            }

            //not the first start, then load all data from internal mode directory
            else {

                //load all the data from internal storage
                readModeDataFromStorage();


            }
        }
        else{
            //download already done before
            listener.onDownloadResult();
        }


    }

    //helper method to clear all data of the resourcemanager
    private void resetData() {

        currentSource = null;
        modeResources.clear();

    }

    /**
     * @return JSON object of URL's of the resources from Server
     */
    private JSONObject getListOfAvailableModeResources() {

        URL requestUrl = null;
        String response = "";
        try {
             requestUrl = new URL(ResourceManager.HOST_STATIC + "data/" + ResourceManager.getInstance().getCurrentAppMode() + "/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if(requestUrl!= null) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    response += line + "\n";
                }

                rd.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;

            }
            finally{
                if(connection!= null)
                connection.disconnect();
            }
            JSONObject modeData = null;
            try {
                modeData = new JSONObject(response);
            } catch (JSONException e) {
                Log.e(TAG, "Error receiving JSON LIST ");
            }
            return modeData;
        }
        else
            Log.e(TAG, "BROKEN REQUESTURL ");
            return null;
    }

    /**
     * Diese Methode liest alle zum aktuellen Appmodus hinterlegten Daten ein.
     * Gespeichert werden
     *
     * @return true if success
     */
    private boolean readModeDataFromStorage() {


        AsyncTask<Void, Integer, Void> readTask = new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                int count = 1;
                if (modeDirectory.exists()) {

                    if (modeResources.isEmpty()) {
                        File[] files = modeDirectory.listFiles();

                        double process = (double) 100 / (double) files.length;

                        int displayedProcess = (int) process;

                        for (File sourceDir : files) {
                            publishProgress(count * displayedProcess);
                            //lies source Object aus dem Speicher ein
                            readSourceFromInternalStorage(sourceDir);
                            count++;
                        }
                    }
                } else {
                    publishProgress(100);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                downloadListener.showDownloadStatus(values[0], "lade Dateien");

            }

            protected void onPostExecute(Void result) {
                downloadListener.onDownloadResult();
            }

        }.execute();


        return true;
    }

    /**
     * Diese Methode liest ein einzelnes Source-Objekt aus dem internen Speicher ein.
     *
     * @param sourceDir Jedes Source-objekt hat ein eigenes Verzeichnis aus dem gelesen werden muss.
     * @return true if success
     */
    private boolean readSourceFromInternalStorage(File sourceDir) {


        Source sourceObject = new Source();
        sourceObject.setSourceId(sourceDir.getName());


        List<Polygon> polygons;

        Gson gsonRead = new Gson();
        BufferedReader br = null;

        File polygonFile = new File(sourceDir, sourceDir.getName() + ".obj");
        File bitMapFile = new File(sourceDir, sourceDir.getName() + ".jpg");

        //lade polygoninformationen
        try {
            br = new BufferedReader(new FileReader(polygonFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        if (br == null)
            return false;

        polygons = gsonRead.fromJson(br, new TypeToken<List<Polygon>>() {
        }.getType());
        sourceObject.setSourceId(sourceDir.getName());
        sourceObject.setShapes(polygons);
        InputStream is = null;
        //load bitmap
        try {
            is = new FileInputStream(bitMapFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Bitmap bitmap = BitmapFactory.decodeStream(is);

        sourceObject.setOriginalImage(bitmap);

        //füge source objekt dem Resource Manager hinzu
        modeResources.put(sourceDir.getName(), sourceObject);

        return true;
    }


    /**
     * Diese Methode schreibt alle nötigen Daten dieses Modus in den Speicher.
     *
     * @return true if success
     */
    public boolean writeModeDataToStorage() {


        boolean status = false;
        for (String key : getModeResources().keySet()) {
            status = writeSourceToInternalStorage(getModeResources().get(key), key);

        }
        return status;
    }


    /**
     * @param object   Source-Objekt das gespeichert werden soll
     * @param fileName dateiname des objektes
     * @return true, if success
     */
    private boolean writeSourceToInternalStorage(Source object, String fileName) {

        //set directory for this Source-Object
        File sourceDir = new File(modeDirectory, fileName);
        if (!sourceDir.exists())
            sourceDir.mkdirs();

        //anzulegende datei
        File polygonDataDestination;
        File bitmapDestination = null;


        Gson gson = new Gson();

        String jsonString = gson.toJson(object.getShapes());


        //create paths
        if (modeDirectory != null) {

            polygonDataDestination = new File(sourceDir, fileName + ".obj");
            bitmapDestination = new File(sourceDir, fileName + ".jpg");
            writeStringToFile(jsonString, polygonDataDestination);

        }


        //schreibe bitmap in eine datei
        FileOutputStream bitMapOutPutStream = null;
        if (bitmapDestination == null)
            return false;

        try {
            bitMapOutPutStream = new FileOutputStream(bitmapDestination);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        (object.getOriginalImage()).compress(Bitmap.CompressFormat.JPEG, 100, bitMapOutPutStream);


        return true;
    }

    /**
     * @param matToStore Mat that should be stored in the filesystem / internal storage
     * @param sourceKey  source object it belongs to
     * @return true if success
     */
    public final boolean writeMat(Mat matToStore, String sourceKey) {


        File sourceDir = new File(modeDirectory, sourceKey);
        if (!sourceDir.exists()) {
            return false;
        } else {
            MatStorageUtils helper = new MatStorageUtils(sourceDir);

            if (matToStore instanceof MatOfKeyPoint) {

                File featureFile = new File(sourceDir, "features_" + sourceKey + ".json");
                if (!featureFile.exists()) {
                    try {
                        featureFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                return writeStringToFile(helper.featuresToJson(((MatOfKeyPoint) matToStore)), featureFile);
            } else {
                return helper.writeDescriptorMat(matToStore);
            }

        }

    }

    public final MatOfKeyPoint readFeatures(Object sourceKey) {
        File sourceDir = new File(modeDirectory, sourceKey.toString());
        if (!sourceDir.exists()) {
            Log.d(TAG, "FEHLER BEIM LESEN DER FEATURES");
            return null;

        } else {
            MatStorageUtils helper = new MatStorageUtils(sourceDir);
            File featureFile = new File(sourceDir, "features_" + sourceKey + ".json");
            if (!featureFile.exists())
                return null;
            else {
                String featureJson = readStringFromFile(featureFile);
                return helper.featuresFromJson(featureJson);

            }
        }

    }

    public final Mat readDescriptors(Object sourceKey) {
        File sourceDir = new File(modeDirectory, sourceKey.toString());
        if (!sourceDir.exists()) {
            Log.d(TAG, "FEHLER BEIM LESEN DER DESKRIPTOREN");

            return null;

        } else {
            MatStorageUtils helper = new MatStorageUtils(sourceDir);
            return helper.readDescriptors();
        }
    }

    /**
     *
     * @param file to read from
     * @return read String
     */
    private String readStringFromFile(File file) {

        FileReader fr = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (fr == null)
            return null;

        BufferedReader br = new BufferedReader(fr);
        try {
            StringBuilder sb = new StringBuilder();
            String line = null;
            try {
                line = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return sb.toString();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * @param string that should be stored
     * @param dest   File where it should be stored
     * @return true if success
     */
    private boolean writeStringToFile(String string, File dest) {

        //Stream to write in File
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(dest);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, "file not found " + dest.getName());
            return false;
        }


        OutputStreamWriter ow = new OutputStreamWriter(fos);
        try {
            ow.write(string);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "failed to write file " + dest.getName());

            return false;
        }
        try {
            ow.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to close streams");

            return false;
        }
        return true;
    }


    public void releaseAllData() {
        for (String key : modeResources.keySet()) {
            modeResources.get(key).releaseAll();
        }
    }

    public String getCurrentAppMode() {
        return currentAppMode;
    }


    public Map<String, Source> getModeResources() {
        return modeResources;
    }

    public Source getCurrentSource() {
        return currentSource;
    }


    public void setCurrentSourceById(String id) {
        this.currentSource = modeResources.get(id);
    }

}
