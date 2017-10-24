package development.andre.sanders.bachelorprojectapp.view;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by andre on 04.07.17.
 */

public class CustomJavaCameraView extends JavaCameraView {

    private final String TAG = "MyCamera";

    private Camera.Size previewSize;

    private double currentScalingFactor =0;

    static {

        Log.d("MEINE_CAMERA.class", "class created");

    }

    private float mDist = 0;

    public CustomJavaCameraView(Context context, int cameraId) {
        super(context, cameraId);

    }

    public CustomJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }


    /**
     * Determine the space between the first two fingers
     */
    public float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    public void setFocusMode(Context item, String type) {

        Camera.Parameters params = mCamera.getParameters();
        mCamera.cancelAutoFocus();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean b, Camera camera) {
            }
        });

        List<String> FocusModes = params.getSupportedFocusModes();

        switch (type) {
            case "Auto":
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    Toast.makeText(item, "Auto Mode activated", Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(item, "Auto Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
            case "ContiniousVideo":
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                else
                    Toast.makeText(item, "Continuous Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
            case "EDOF":

                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
                else
                    Toast.makeText(item, "EDOF Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
            case "Fixed":
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                else
                    Toast.makeText(item, "Fixed Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
            case "Infinity":
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                else
                    Toast.makeText(item, "Infinity Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
            case "Macro":
                if (FocusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                else
                    Toast.makeText(item, "Macro Mode is not supported", Toast.LENGTH_SHORT).show();
                break;
        }

        mCamera.setParameters(params);
    }


    public boolean setResolution() {

        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();

            previewSize = detectSize(params);
            Log.d(TAG, "detected preview size=" + previewSize.width + "x" + previewSize.height);
            params.setPreviewSize(previewSize.width, previewSize.height);

            // NV21 is supported by all Android cameras and isn't too difficult to work with
            // in OpenCV, so use it on all platforms
            params.setPreviewFormat(ImageFormat.NV21);

            // enable video recording hint for better performance if allowed
            if (shouldEnableRecordingHint()) {
                params.setRecordingHint(true);
            }

            // set focus mode to one of the following options, in descending priority:
            // 1. continuous picture - aggressive refocusing for still shots
            // 2. continuous video - slow refocusing for watchable videos
            List focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                Log.d(TAG, "enabling continuous picture focus mode");
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                Log.d(TAG, "enabling continuous video focus mode");
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else {
                Log.w(TAG, "no continuous focus mode found");
            }

            mCamera.setParameters(params);
//        Camera.Parameters params = mCamera.getParameters();
//        List<Camera.Size> resList = mCamera.getParameters().getSupportedPictureSizes();
//        int listNum = 1;// 0 is the maximum resolution
//        int width = resList.get(listNum).width;
//        int height = resList.get(listNum).height;
//        params.setPictureSize(width, height);
//        mCamera.setParameters(params);
            return true;
        } else {
            return false;
        }


    }



    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }


    public Camera.Parameters getParameters() {
        return mCamera.getParameters();
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void processTouch(MotionEvent event) {

    }

    public float getmDist() {
        return mDist;
    }

    public void setmDist(float mDist) {
        this.mDist = mDist;
    }

    private Camera.Size detectSize(Camera.Parameters params) {
        Camera.Size biggest = null;
        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            if (biggest == null || (biggest.width < size.width && biggest.height < size.height)) {
                biggest = size;
            }
        }
        return biggest;
    }

    private boolean shouldEnableRecordingHint() {
        if (android.os.Build.MODEL.equals("GT-I9100")) {
            // galaxy S2 has problems with setRecordingHint
            return false;
        }

        return true;
    }

    public void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();

        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            //zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            //zoom out
            if (zoom > 0)
                zoom--;
        }
        Log.d(TAG, "current zoom: " + zoom + " mDistance: " + newDist );
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }


    public void handleFocus(MotionEvent event, Camera.Parameters params) {
        int pointerId = event.getPointerId(0);
        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        List<String> supportedFocusModes = params.getSupportedFocusModes();
        if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    // currently set to auto-focus on single touch

                }
            });
        }
    }





}