package development.andre.sanders.bachelorprojectapp.utils;

/**
 * Created by andre on 10.08.17.
 * Kleine Helferklasse für globale Konstanten
 */

public class Constants {

    public static final String SERVER_RESOURCE_DIR = "project2.informatik.uni-osnabrueck.de/ardetection/static";

    //APP MODUS
    public final static String STUDENT_MODE = "studentMode";
    public final static String MUSEUM_MODE = "museumMode";

    //POLYGON input type
    public final static int INPUT_TYPE_FACE = 0;
    public final static int INPUT_TYPE_CLOSED_SHAPES = 1;

    //DETEKTOR MODUS
    public final static int FEATURE_DETECTION_SIFT = 0;
    public final static int FEATURE_DETECTION_SURF = 1;
    public final static int FEATURE_DETECTION_ORB = 2;

    //DESKRIPTOR MODUS
    public final static int FEATURE_EXTRACTION_SIFT = 0;
    public final static int FEATURE_EXTRACTION_SURF = 1;
    public final static int FEATURE_EXTRACTION_ORB = 2;

    //UI
    public final static int MESSAGE_TYPE_ERROR =0;
    public final static int MESSAGE_TYPE_INFO = 1;

}
