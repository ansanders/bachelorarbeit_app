package development.andre.sanders.bachelorprojectapp.model.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import development.andre.sanders.bachelorprojectapp.model.callbacks.DownloadListener;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;
import development.andre.sanders.bachelorprojectapp.utils.CalcUtils;

/**
 * Created by andre on 05.07.17.
 * <p>
 * Je nach Modus, werden die benötigten Daten (Bilder und Polygon-Infos) von den Quellen
 * heruntergeladen.
 * Im Falle des Algorithmenmodus werden Polygoninformationen aus den öffentlichen Html-dokumenten gelesen,
 * geparsed und in interne Polygondaten überführt.
 */

public class DownloadResourceTask extends AsyncTask<Void, Integer, Void> {

    private static final String TAG = "DownloadResourceTask";
    private String currentFile;
    //Maps mit URL's der zu holenden Ressourcen. Müssen den selben Schlüssel haben!
    private Map<String, String> informationRoutes = new HashMap<>();
    private Map<String, String> imageRoutes = new HashMap<>();

    //listener um die activity zu informieren
    private DownloadListener downloadListener;
    private Context appContext;
    private JSONObject availableData = null;

    public DownloadResourceTask(Context context, DownloadListener downloadListener, JSONObject availableData) {
        super();
        this.availableData = availableData;
        this.downloadListener = downloadListener;
        this.appContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {

        final ResourceManager resourceManager = ResourceManager.getInstance();
        try {

            int sourcesCnt = 0;
            Thread.sleep(1000);

            //pointer to current mode path on server
            String currentModeURL = ResourceManager.HOST_STATIC + ResourceManager.getInstance().getCurrentAppMode() + "/";
            Iterator<String> keyIterator = availableData.keys();
            double scalingX = 0, scalingY = 0;

            //Iterate through all source_ids and get the URL where JSON and Image is located
            while (keyIterator.hasNext()) {
                String currentSourceId = keyIterator.next();
                currentFile = currentSourceId;
                //Build a new internal Source Object
                Source sourceObject = new Source();
                sourceObject.setSourceId(currentSourceId);

                double process = (double) 100 / ((double) availableData.length());
                int displayedProcess = (int) process;
                Log.d(TAG, "TEEEEEST: " + displayedProcess * sourcesCnt);
                //publish progress to ui
                publishProgress(displayedProcess * sourcesCnt);

                try {
                    JSONObject sourceData = availableData.getJSONObject(currentSourceId);

                    String jsonFilePath = sourceData.getString("source_json_path");
                    String imageFilePath = sourceData.getString("source_image_path");

                    HttpURLConnection bitmapConnection = null;
                    InputStream bitmapInput = null;
                    //Download bitmap for this source
                    try {
                        //get connection to Server where the image is located
                        URL bitmapURL = new URL(ResourceManager.HOST_STATIC + imageFilePath);
                        bitmapConnection = (HttpURLConnection) bitmapURL.openConnection();
                        bitmapConnection.setDoInput(true);
                        bitmapConnection.connect();
                        bitmapInput = bitmapConnection.getInputStream();
                        Bitmap myBitmap = BitmapFactory.decodeStream(bitmapInput);

                        //Factor to scale the points too after scaling image
                        scalingX = (double) Source.getReducedSourceWidth() / (double) myBitmap.getWidth();
                        scalingY = (double) Source.getReducedSourceHeight() / (double) myBitmap.getHeight();
                        //scale image resolution to get a faster matching process
                        Bitmap resizedBitmap = getResizedBitmap(myBitmap, Source.getReducedSourceWidth(), Source.getReducedSourceHeight());
                        Log.d(TAG, "Bild wurde reduziert auf " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());
                        sourceObject.setOriginalImage(resizedBitmap);

                    } catch (IOException ie) {
                        ie.printStackTrace();
                    } finally {
                        if (bitmapConnection != null && bitmapInput != null) {
                            bitmapConnection.disconnect();
                            try {
                                bitmapInput.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                    //download JSON information for this source
                    JSONObject sourceJson = null;
                    URL jsonURL = new URL(ResourceManager.HOST_STATIC + jsonFilePath);
                    Log.d(TAG, "hole json von : " + jsonURL.toString());
                    HttpURLConnection jsonConnection = (HttpURLConnection) jsonURL.openConnection();
                    jsonConnection.setDoInput(true);
                    jsonConnection.connect();
                    InputStream jsonInput = jsonConnection.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(jsonInput));
                    String line;
                    StringBuilder responseStrBuilder = new StringBuilder();
                    while ((line = br.readLine()) != null) {

                        responseStrBuilder.append(line);
                    }


                    jsonConnection.disconnect();
                    jsonInput.close();


                    //get the whole .json file for this sourceobject
                    try {
                        //source repräsentation
                        sourceJson = new JSONObject(responseStrBuilder.toString());
                        Log.d(TAG, sourceJson.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "JSON falsch formatiert");
                    }
                    if (sourceJson != null) {
                        JSONObject uploadJson = new JSONObject();
                        try {

                            //set internal data from Json File
                            sourceObject.setTitle(sourceJson.getString("title"));
                            sourceObject.setPublished(sourceJson.getInt("published"));
                            sourceObject.setArtistName(sourceJson.getString("artist_name"));

                            JSONArray objectArray = sourceJson.getJSONArray("objects");

                            //get all polygons in json file
                            for (int i = 0; i < objectArray.length(); i++) {
                                Polygon shape = new Polygon();

                                JSONObject singleObject = objectArray.getJSONObject(i);
                                JSONObject informationObject = new JSONObject();
                                informationObject.put("name", singleObject.getString("object_id"));
                                informationObject.put("information", singleObject.getString("info"));
                                shape.setInformation(informationObject);
                                JSONArray coords = singleObject.getJSONArray("corners");
                                List<Point> corners = new ArrayList<>();

                                for (int n = 0; n < coords.length(); n = n + 2) {
                                    corners.add(new Point(coords.getDouble(n) * scalingX, coords.getDouble(n + 1) * scalingY));
                                }
                                shape.setCorners(corners);
                                Point center = CalcUtils.calculateCenterOfPolygon(corners);
                                shape.setxPos(center.x);
                                shape.setyPos(center.y);
//
                                sourceObject.getShapes().add(shape);

                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else
                        throw new RuntimeException("JSON from url " + currentModeURL + currentSourceId + " does not exist");


                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

                //Füge das Bild samt Information zur Collection hinzu
                Log.d(TAG, "füge resource dem Speicher hinzu: " + sourceObject.getSourceId());
                resourceManager.getModeResources().put(currentSourceId, sourceObject);
                ++sourcesCnt;

            }
            resourceManager.writeModeDataToStorage();


        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        return null;
    }

    //Update the progress
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        //informiere listener über aktuellen status
        downloadListener.showDownloadStatus(values[0], "lade Daten zu " + currentFile);

    }

    //after executing the code in the thread
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        //Informiere den listener
        downloadListener.onDownloadResult();

    }

    /**
     * getResizedBitmap method is used to Resized the Image according to custom width and height
     *
     * @param image
     * @param newHeight (new desired height)
     * @param newWidth  (new desired Width)
     * @return image (new resized image)
     */
    private static Bitmap getResizedBitmap(Bitmap image, int newWidth, int newHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // recreate the new Bitmap
        return Bitmap.createBitmap(image, 0, 0, width, height,
                matrix, false);
    }


}