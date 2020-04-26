package com.jmfavreau.pictoparle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class BoardDetector {
    private final SimpleBoardListener listener;
    private boolean active;
    private boolean init;
    private boolean covered;
    static final long interval_covered = 4000;
    static final long interval_uncovered = 1000;
    Camera camera;
    Camera.PictureCallback imageProcessing;
    Timer timer;
    private TimerTask task;
    private SurfaceTexture surfaceTexture;
    private int width;
    private int height;


    public BoardDetector(final SimpleBoardListener listener) {
        this.listener = listener;
        // by default, the board detector is active
        active = true;
        // at the beginning, we don't know if the screen is covered or not
        init = true;

        timer = new Timer();
        task = null;

        imageProcessing = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera cam) {

                if (active) {
                    boolean cov = isCovered(data);
                    if (init) {
                        if (cov)
                            listener.onBoardDown();
                        setCovered(cov);
                        init = false;
                    }
                    else {
                        if (cov != covered) {
                            if (cov)
                                listener.onBoardDown();
                            else
                                listener.onRemovedBoard();
                            setCovered(cov);
                        }
                        else {
                            if (!covered) {
                                Integer idcode = decode(data);
                                if (idcode != null) {
                                    listener.onNewHoverBoard(idcode);
                                }
                            }
                        }
                    }
                }
                else {
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }

            }

            private Integer decode(byte[] data) {
                // TODO: implement a QRCode detector and decoder
                return null;
            }

            private boolean isCovered(byte[] data) {
                int size = width * height;
                float val = 0;
                for(int i = 0; i < size; i++) {
                    val += data[i] & 0xFF;
                }
                val /= size;
                return (val < 10);
            }
        };


    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        try {

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    try {
                        Log.d("PictoParle", "Found camera");
                        return Camera.open(camIdx);
                    } catch (RuntimeException e) {
                        Log.e("PictoParle", "Camera failed to open: " + e.getLocalizedMessage());
                    }
                }
            }
            // if no camera has been found, but we have more than 1 camera, select
            // the second one
            if (cameraCount > 1) {
                return Camera.open(1);
            }
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return null; // returns null if camera is unavailable
    }

    private void initCamera() {
        Log.d("PictoParle", "init camera");

        if (camera != null && !active) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        if (camera == null) {
            camera = getCameraInstance();
        }
        if (camera != null) {
            // create a surface to get resulting images
            surfaceTexture = new SurfaceTexture(10);
            try {
                camera.setPreviewTexture(surfaceTexture);
            } catch (IOException e) {
                Log.e("PictoParle", "Cannot create a surface texture");
            }

            // set parameters
            Camera.Parameters param = camera.getParameters();

            //set color effects to none
            param.setColorEffect(Camera.Parameters.EFFECT_NONE);

            //set antibanding to none
            if (param.getAntibanding() != null) {
                param.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
            }

            // set white ballance
            if (param.getWhiteBalance() != null) {
                param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT);
            }

            //set flash
            if (param.getFlashMode() != null) {
                param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }

            //set zoom
            if (param.isZoomSupported()) {
                param.setZoom(0);
            }

            //set focus mode
            if (param.getFocusMode() != null) {
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            }

            List<Integer> formats = param.getSupportedPictureFormats();
            if (formats != null) {
                if (formats.contains(ImageFormat.NV16)) {
                    param.setPictureFormat(ImageFormat.NV16);
                }
                else if (formats.contains(ImageFormat.NV21)) {
                    param.setPictureFormat(ImageFormat.NV21);
                }
                else {
                    Log.w("PictoParle", "Cannot select a valid image format.");
                }
            }

            try {
                camera.setParameters(param);
            }
            catch (RuntimeException e) {
                // might occur on Virtual environments, ignore it
            }
            width = param.getPictureSize().width;
            height = param.getPictureSize().height;

        }

    }


    private void stopTimer() {
        if (task != null) {
            task.cancel();
            timer.purge();
        }
    }

    private void setCovered(boolean covered) {
        Log.d("Pictoparle", "Detecting coverage change: " + covered);
        this.covered = covered;
        // cancel the previous tasks
        stopTimer();

        long interval = interval_covered;
        if (!covered) interval = interval_uncovered;

        task = new TimerTask() {
            @Override
            public void run() {
                if (active)
                    checkCoverageAndQRCode();
            }
        };

        timer.schedule(task, interval, interval);

    }

    private void checkCoverageAndQRCode() {
        if (camera != null) {
            camera.startPreview();
            camera.takePicture(null, null, imageProcessing);
        }
        else {
            Log.d("PictoParle", "No camera available");
        }
    }

    public void setInactive() {
        if (this.active) {
            Log.d("PictoParle", "set board detector inactive");
            this.active = false;
            stopTimer();
        }
    }

    public void setActive() {
        if (init || !this.active) {
            Log.d("PictoParle", "set board detector active");
            initCamera();
            this.active = true;
            if (init) {
                // first run to decide if it's covered or not
                checkCoverageAndQRCode();
            }
            else {
                setCovered(covered);
            }
        }
    }

    public void clear() {
        Log.d("PictoParle", "clear board detector");
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        camera = null;
    }

    public boolean isReady() {
        return !init;
    }

    public interface SimpleBoardListener {
        void onRemovedBoard();

        void onNewHoverBoard(int boardID);

        void onBoardDown();
    }
}
