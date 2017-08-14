package development.andre.sanders.bachelorprojectapp.model.tasks;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import development.andre.sanders.bachelorprojectapp.model.callbacks.DownloadListener;
import development.andre.sanders.bachelorprojectapp.model.data.Line;
import development.andre.sanders.bachelorprojectapp.model.data.Polygon;
import development.andre.sanders.bachelorprojectapp.model.data.Source;
import development.andre.sanders.bachelorprojectapp.model.manager.ResourceManager;
import development.andre.sanders.bachelorprojectapp.utils.CalcUtils;
import development.andre.sanders.bachelorprojectapp.utils.Constants;

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
    private String appMode;

    public DownloadResourceTask(String appMode, DownloadListener downloadListener) {
        super();

        this.appMode = appMode;
        this.downloadListener = downloadListener;

        switch (appMode) {
            case Constants.STUDENT_MODE:
                //prepare index routes - algomode
//        informationRoutes.put(2010, "http://www-lehre.inf.uos.de/~ainf/2010/foto/index.html");
//        informationRoutes.put(2011, "http://www-lehre.inf.uos.de/~ainf/2011/foto/index.html");
//        informationRoutes.put(2012, "http://www-lehre.inf.uos.de/~ainf/2012/foto/index.html");
//        informationRoutes.put(2013, "http://www-lehre.inf.uos.de/~ainf/2013/foto/index.html");
//        informationRoutes.put(2014, "http://www-lehre.inf.uos.de/~ainf/2014/foto/index.html");
                informationRoutes.put("2015", "http://www-lehre.inf.uos.de/~ainf/2015/foto/index.html");
                informationRoutes.put("2016", "http://www-lehre.inf.uos.de/~ainf/2016/foto/index.html");

//        //prepare imageroutes - algomode
//        imageRoutes.put(2010, "http://www-lehre.inf.uos.de/~ainf/2010/foto/algo-2010.jpg");
//        imageRoutes.put(2011, "http://www-lehre.inf.uos.de/~ainf/2011/foto/algo-2011.jpg");
//        imageRoutes.put(2012, "http://www-lehre.inf.uos.de/~ainf/2012/foto/algo-2012-lang.jpg");
//        imageRoutes.put(2013, "http://www-lehre.inf.uos.de/~ainf/2013/foto/algo-2013-lang.jpg");
//        imageRoutes.put(2014, "http://www-lehre.inf.uos.de/~ainf/2014/foto/algo-2014-small.jpg");
                imageRoutes.put("2015", "http://www-lehre.inf.uos.de/~ainf/2015/foto/algo-2015-small.jpg");
                imageRoutes.put("2016", "http://www-lehre.inf.uos.de/~ainf/2016/foto/algo_2016_web.jpg");
                break;

            case Constants.MUSEUM_MODE:
                //set routes for JSON-Files and Original Images
                informationRoutes.put("banksy", "http://project2.informatik.uni-osnabrueck.de/ardetection/static/informationMaps/banksy.json");
                imageRoutes.put("banksy", "http://project2.informatik.uni-osnabrueck.de/ardetection/static/images/banksy.jpg");
                break;

            default:
                break;

        }

    }

    //before doing the background stuff
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //setup the progress dialog
        //Display the progress dialog
        ;
    }

    @Override
    protected Void doInBackground(Void... params) {

        final ResourceManager resourceManager = ResourceManager.getInstance();
        try {

            synchronized (this) {

                this.wait(850);

                int count = 1;

                for (final String key : informationRoutes.keySet()) {

                    currentFile = key;


                    //informiere activity über den Prozess
                    double process = (double) 100 / (double) imageRoutes.size();

                    int displayedProcess = (int) process;
                    publishProgress(displayedProcess * count);


                    //Erstelle pro content ein neues Source objekt
                    final Source sourceObject = new Source();
                    sourceObject.setSourceId(currentFile);


                    //Download des originalbildes

                    try {
                        URL url = new URL(imageRoutes.get(key));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap myBitmap = BitmapFactory.decodeStream(input);
                        sourceObject.setOriginalImage(myBitmap);
                        connection.disconnect();
                        input.close();
                    } catch (IOException e) {
                        // Log exception
                        Log.e(TAG, "Bild konnte nicht richtig heruntergeladen werden");
                        return null;
                    }

                    if (appMode.equals(Constants.STUDENT_MODE)) {
                        Document doc = null;
                        try {
                            doc = Jsoup.connect(informationRoutes.get(key)).get();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (doc != null) {

                            String title = doc.title();


                            Elements polyElements = doc.select("area");


                            Integer studentId;
                            //sammle studenteninfos
                            for (Element e : polyElements) {
                                //Erzeuge pro Teilobjekt ein neues Polygon
                                Polygon shape = new Polygon();

                                //speichere studentid um passende Information zu finden
                                studentId = Integer.parseInt(e.attr("data-id"));

                                //finde Koordinaten des Mittelpunktes
                                int shapeX = Integer.parseInt(e.attr("data-x"));
                                int shapeY = Integer.parseInt(e.attr("data-y"));
                                shape.setxPos(shapeX);
                                shape.setyPos(shapeY);


                                //Finde "Eckpunkte" des Polygons
                                String coordinateString;

                                coordinateString = e.attr("coords");

                                String[] coords = coordinateString.split(",");

                                List<Point> points = new ArrayList<>();

                                //Speichere die Eckpunkte im PolygonObjekt
                                for (int i = 0; i < coords.length; i = i + 2) {
                                    int pointX, pointY;
                                    pointX = Integer.parseInt(coords[i]);
                                    pointY = Integer.parseInt(coords[i + 1]);
                                    points.add(new Point(pointX, pointY));

                                }
                                //Transformiere Eckpunkte in Daten und setze diese im shape objekt
                                for (int i = 0; i < points.size() - 1; i++) {
                                    shape.getEdges().add(new Line(points.get(i), points.get(i + 1)));
                                }
                                shape.getEdges().add(new Line(points.get(points.size() - 1), points.get(0)));

                                //selektiere passendes Polygon in html file
                                Element nameElem = doc.select("li[data-id=" + studentId + "]").first();

                                String firstName, lastName;
                                if (nameElem != null) {

                                    firstName = nameElem.select("span[class=first]").text();
                                    lastName = nameElem.select("span[class=last]").text();
                                } else {
                                    firstName = "nicht angegeben";
                                    lastName = "nicht angegeben";
                                }
                                //Informationen über das Teilobjekt
                                JSONObject student = new JSONObject();
                                try {
                                    student.put("firstname", firstName);
                                    student.put("lastname", lastName);
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }

                                //setze hinterlegte Infos für dieses Polygon
                                shape.setInformation(student);


                                //Füge das Polygon zum sourceObject hinzu
                                sourceObject.getShapes().add(shape);


                            }


                            //Füge das Bild samt Information zur Collection hinzu
                            resourceManager.getModeResources().put(key, sourceObject);


                        }


                    }
                    //falls App im Museumsmodus gestartet
                    else if (appMode.equals(Constants.MUSEUM_MODE)) {
                        JSONObject sourceJson = null;
                        try {
                            URL url = new URL(informationRoutes.get(key));
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(input));
                            String line;
                            StringBuilder responseStrBuilder = new StringBuilder();
                            while ((line = br.readLine()) != null) {

                                responseStrBuilder.append(line);
                            }


                            try {
                                //source repräsentation
                                sourceJson = new JSONObject(responseStrBuilder.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "JSON falsch formatiert");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "URL konnte nicht geöffnet werden");
                        }

                        if (sourceJson != null) {

                            try {



                                JSONArray objectArray = sourceJson.getJSONArray("objects");

                                //füge alle hinterlegten polygonInfos dem sourceObjekt hinzu
                                for (int i = 0; i < objectArray.length(); i++) {
                                    Polygon shape = new Polygon();

                                    JSONObject singleObject = objectArray.getJSONObject(i);
                                    JSONObject informationObject = new JSONObject();
                                    informationObject.put("name", singleObject.getString("objectId"));
                                    informationObject.put("information", singleObject.getString("info"));
                                    shape.setInformation(informationObject);
                                    JSONArray coords = singleObject.getJSONArray("coords");
                                    List<Point> points = new ArrayList<>();

                                    for (int n = 0; n < coords.length(); n = n + 2) {
                                        points.add(0, new Point(coords.getInt(n), coords.getInt(n + 1)));
                                    }
                                    Point center = CalcUtils.calculateCenterOfPolygon(points);
                                    shape.setxPos(center.x);
                                    shape.setyPos(center.y);
                                    for (int a = 0; a < points.size() - 1; a++) {
                                        shape.getEdges().add(new Line(points.get(i), points.get(i + 1)));
                                    }
                                    shape.getEdges().add(new Line(points.get(points.size() - 1), points.get(0)));
                                    sourceObject.getShapes().add(shape);

                                }

                                //Füge das Bild samt Information zur Collection hinzu
                                resourceManager.getModeResources().put(key, sourceObject);


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    count++;

                }

                //Speichern der daten
                resourceManager.writeAllData();
            }
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


}