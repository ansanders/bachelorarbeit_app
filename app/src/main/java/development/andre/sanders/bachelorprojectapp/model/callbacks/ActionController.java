package development.andre.sanders.bachelorprojectapp.model.callbacks;

import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.opencv.android.CameraBridgeViewBase;

import development.andre.sanders.bachelorprojectapp.view.CustomJavaCameraView;

/**
 * Created by andre on 11.08.17.
 *
 * Controller, der den Userinput händelt (z.B touchs, clicks etc)
 */

public class ActionController implements View.OnTouchListener {

    private CustomJavaCameraView camera;
    public ActionController(CustomJavaCameraView camera){
        this.camera = camera;
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(camera==null)
            return false;

        Log.i("OpenCvActivity", "entered onTouch " + event.getPointerCount());
        Camera.Parameters params = camera.getParameters();
        int action = event.getAction();

        /*handle zoom when more than one finger detected*/
        if (event.getPointerCount() > 1) {
            // handle multi-touch events
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                camera.setmDist(camera.getFingerSpacing(event));
            } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                camera.getCamera().cancelAutoFocus();
                camera.handleZoom(event, params);

            }
        } else {

            // handle single touch events

            camera.handleFocus(event, params);


        }

        return true;
    }
}
