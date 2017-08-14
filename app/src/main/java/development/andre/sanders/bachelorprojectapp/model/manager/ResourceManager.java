package development.andre.sanders.bachelorprojectapp.model.manager;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private final String TAG = "ResourceManager.class";

    //Singleton Instanz
    private static ResourceManager instance = null;

    //das aktuell für den AppModus angelegte Datenverzeichnis
    private File modeDirectory;

    //Map aller Source-Objekte
    private Map<String, Source> modeResources = new HashMap<>();

    private DownloadListener downloadListener;

    private Source currentSource;




    //Referenz
    public static ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }


    /**
     * @param activity initiiert den Singleton. Nicht wirklich objektorientiert aber praktisch für
     *                 diesen Zweck.
     */
    public void init(Activity activity, String appMode, DownloadListener listener) {

        if(modeResources!= null && !modeResources.isEmpty())
            resetData();


        ContextWrapper contextWrapper = new ContextWrapper(activity.getApplicationContext());
        String fileDir = "data";
        File directory = contextWrapper.getDir(fileDir, Context.MODE_PRIVATE);
        this.downloadListener = listener;

        //Benutze Shared Preferences zum Speichern kleinerer Daten
        SharedPreferences prefs = activity.getSharedPreferences(fileDir, MODE_PRIVATE);

        //Lege Verzeichnis im internal Storage an
        modeDirectory = new File(directory, appMode);

        //Wenn Modus zum ersten mal gestartet --> Daten herunterladen
        if (prefs.getBoolean("firstRun" + appMode, true)) {

            prefs.edit().putBoolean("firstRun" + appMode, false).apply();

            //erstell Verzeichnis für diesen Modus im internen Speicher
            boolean status = modeDirectory.mkdir();

            //Lade alle daten herunter
            new DownloadResourceTask(appMode, downloadListener).execute();


        }

        //nicht der erste Start --_> Daten aus dem Speicher laden
        else {

            //load all the data from internal storage
            readModeDataFromStorage();


        }


    }

    public void resetData(){

        modeResources.clear();

    }

    /**
     * Diese Methode liest alle zum aktuellen Appmodus hinterlegten Daten ein.
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
    public boolean writeAllData() {


        boolean status = false;
        for (Object key : getModeResources().keySet()) {
            status = writeSourceToInternalStorage(getModeResources().get(key), key.toString());

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
        File polygonDataDestination = null;
        File bitmapDestination = null;


        Gson gson = new Gson();

        String jsonString = gson.toJson(object.getShapes());

        //create paths
        if (modeDirectory != null) {


            polygonDataDestination = new File(sourceDir, fileName + ".obj");
            bitmapDestination = new File(sourceDir, fileName + ".jpg");


            FileOutputStream fos = null;

            //Stream to write in File
            try {
                fos = new FileOutputStream(polygonDataDestination);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (fos == null)
                return false;

            OutputStreamWriter ow = new OutputStreamWriter(fos);
            try {
                ow.write(jsonString);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                ow.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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



    public final boolean writeMat(Mat matToStore, Object sourceKey) {


        File sourceDir = new File(modeDirectory, sourceKey.toString());
        if (!sourceDir.exists()) {
            return false;
        }
        else{
            MatStorageUtils helper = new MatStorageUtils(sourceDir);
            return helper.writeMat(matToStore);
        }

    }

    public final MatOfKeyPoint readFeatures(Object sourceKey) {
        File sourceDir = new File(modeDirectory, sourceKey.toString());
        if(!sourceDir.exists())
            return null;
        else{
            MatStorageUtils helper = new MatStorageUtils(sourceDir);
            return helper.readFeatures();
        }

    }

    public final Mat readDescriptors(Object sourceKey){
        File sourceDir = new File(modeDirectory, sourceKey.toString());
        if(!sourceDir.exists())
            return null;
        else{
            MatStorageUtils helper = new MatStorageUtils(sourceDir);
            return helper.readDescriptors();
        }
    }

    /**
     * @param activity    activity which wants to save the Image
     * @param finalBitmap Image to save
     */
    public static void saveImageToExternalStorage(Activity activity, Bitmap finalBitmap) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "algotest" + n + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(activity.getApplicationContext(), new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });

    }

    public void releaseAllData(){
        for(String key : modeResources.keySet()){
            modeResources.get(key).releaseAll();
        }
    }
    public Map<String, Source> getModeResources() {
        return modeResources;
    }

    public void setModeResources(Map<String, Source> modeResource) {
        this.modeResources = modeResource;
    }

    public File getSourceDir(String name) {
        File file = new File(modeDirectory, name);
        if (!file.exists())
            return null;
        else
            return file;

    }

    public Source getCurrentSource() {
        return currentSource;
    }

    public void setCurrentSource(Source currentSource) {
        this.currentSource = currentSource;
    }

    public void setCurrentSourceById(String id){
        this.currentSource = modeResources.get(id);
    }
}
