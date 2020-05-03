package com.jmfavreau.pictoparle;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class BoardDetector {
    private final SimpleBoardListener boardListener;
    private boolean active;
    private boolean init;
    private boolean covered;
    private long interval_covered;
    private long interval_uncovered;
    private Camera camera;

    private SurfaceTexture surfaceTexture;
    private int width;
    private int height;
    private boolean canBeActive;
    private boolean errorOnCreation;
    private Context context;

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
                        return null;
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

    private CameraHandlerThread mThread = null;

    public void start() {
        if (camera == null) {
            openCamera();
        }
    }

    /** this class creates the camera in a separated thread, such that it will not
     * freeze the GUI, and gives a way to wait the choosen number of milliseconds before
     * processing the preview. */
    private class CameraHandlerThread extends HandlerThread implements Handler.Callback {
        protected  Handler mHandler;
        private static final String ON_BOARD_DOWN = "ON_BOARD_DOWN";
        private static final String ON_REMOVE_BOARD = "ON_REMOVE_BOARD";
        private static final String ON_NEW_HOVER_BOARD = "ON_NEW_HOVER_BOARD";
        private static final String MSG_KEY = "MSG";
        private static final String ID_CODE = "ID_CODE";




        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            camera = null;
            mHandler = new Handler(getLooper(), this);
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.d("PictoParle", "on reçoit un message du thread qui calcule");
            Bundle bundle = msg.getData();
            if (bundle.getString(MSG_KEY) == ON_BOARD_DOWN) {
                if (!covered) {
                    covered = true;
                    boardListener.onBoardDown();
                    // stop preview
                    camera.stopPreview();
                    // and start with the correct parameters
                    setParamsStartPreview();
                }
            }
            else if (bundle.getString(MSG_KEY) == ON_REMOVE_BOARD) {
                if (covered) {
                    covered = false;
                    boardListener.onRemovedBoard();
                    // stop preview
                    camera.stopPreview();
                    // and start with the correct parameters
                    setParamsStartPreview();
                }
            }
            else if (bundle.getString(MSG_KEY) == ON_NEW_HOVER_BOARD) {
                if (!covered) {
                    int idCode = bundle.getInt(ID_CODE);
                    boardListener.onNewHoverBoard(idCode);
                }
            }
            return false;
        }



        synchronized void notifyCameraOpened() {
            notify();
        }

        // inspired from https://stackoverflow.com/a/19154438
        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    camera = getCameraInstance();

                    if (camera != null) {
                        camera.setPreviewCallback(new Camera.PreviewCallback() {
                            // inspired from https://stackoverflow.com/a/39948596
                            @Override
                            public void onPreviewFrame(byte[] data, Camera camera) {
                                long releaseTime = SystemClock.currentThreadTimeMillis();
                                if (covered) releaseTime += interval_covered;
                                else releaseTime += interval_uncovered;

                                boolean cov = isCovered(data);
                                if (init) {
                                    if (cov) {
                                        sendMessageDown();
                                    }
                                    init = false;
                                } else {
                                    if (cov != covered) {
                                        if (cov)
                                            sendMessageDown();
                                        else
                                            sendMessageRemovedBoard();
                                        Log.d("Pictoparle", "Detecting coverage change: " + covered);
                                    } else {
                                        if (!covered) {
                                            Integer idcode = decode(data);
                                            if (idcode != null) {
                                                sendMessageNewHoverBoard(idcode);
                                            }
                                        }
                                    }
                                }
                                try {
                                    // see https://stackoverflow.com/a/5837955
                                    Thread.sleep(releaseTime - SystemClock.currentThreadTimeMillis());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            private void sendMessageNewHoverBoard(Integer idCode) {
                                Message msg = mHandler.obtainMessage();
                                Bundle bundle = new Bundle();
                                bundle.putString(MSG_KEY, ON_REMOVE_BOARD);
                                bundle.putInt(ID_CODE, idCode);
                                msg.setData(bundle);
                                mHandler.sendMessage(msg);
                            }

                            private void sendMessageRemovedBoard() {
                                Message msg = mHandler.obtainMessage();
                                Bundle bundle = new Bundle();
                                bundle.putString(MSG_KEY, ON_REMOVE_BOARD);
                                msg.setData(bundle);
                                mHandler.sendMessage(msg);
                            }

                            private void sendMessageDown() {
                                Message msg = mHandler.obtainMessage();
                                Bundle bundle = new Bundle();
                                bundle.putString(MSG_KEY, ON_BOARD_DOWN);
                                msg.setData(bundle);
                                mHandler.sendMessage(msg);
                            }

                            private Integer decode(byte[] data) {
                                // TODO: implement a QRCode detector and decoder
                                return null;
                            }

                            private boolean isCovered(byte[] data) {
                                int size = width * height;
                                float val = 0;
                                for (int i = 0; i < size; i++) {
                                    val += data[i] & 0xFF;
                                }
                                val /= size;
                                return (val < 10);
                            }
                        });
                    }

                    notifyCameraOpened();
                }

            });
            try {
                wait();
            }
            catch (InterruptedException e) {
                Log.w("PictoParle", "wait was interrupted in camera thread");
                errorOnCreation = true;
                camera = null;
            }
        }



    }

    private void openCamera() {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }


    public BoardDetector(final SimpleBoardListener listener,
                         Context context,
                         boolean canBeActive,
                         long interval_covered,
                         long interval_uncovered) {
        this.boardListener = listener;
        // by default, the board detector is active
        active = true;
        // at the beginning, we don't know if the screen is covered or not
        init = true;
        // the user can cancel the board detection
        this.canBeActive = canBeActive;
        // an error can occur on creation. In this case, we will never use the camera
        errorOnCreation = false;
        this.context = context;

        this.interval_covered = interval_covered;
        this.interval_uncovered = interval_uncovered;

        // by default, the board is covered
        covered = true;


    }




    private void setErrorInActivation() {
        errorOnCreation = true;
        camera = null;
        active = false;
        Toast.makeText(context, "Impossible d'activer la caméra. La détection de planche n'est pas possible.", Toast.LENGTH_SHORT).show();
    }


    public void setInactive() {
        if (this.active) {
            Log.d("PictoParle", "set board detector inactive");
            this.active = false;
            camera.stopPreview();
        }
    }

    public void setActive() {
        if (!errorOnCreation && canBeActive && (init || !this.active)) {
            Log.d("PictoParle", "set board detector active");
            this.active = true;
            setParamsStartPreview();
        }
    }

    private void setParamsStartPreview() {
        // create a surface to get resulting images
        try {
            surfaceTexture = new SurfaceTexture(10);
            camera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
            Log.e("PictoParle", "Cannot create a surface texture");
            setErrorInActivation();
            return;
        }

        // set parameters
        Camera.Parameters param = camera.getParameters();

        //set color effects to none
        param.setColorEffect(Camera.Parameters.EFFECT_NONE);

        //set antibanding to none
        if (param.getAntibanding() != null) {
            param.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
        }

        // set white balance
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

        // set preview framerate
        List<int[]> frameRates = param.getSupportedPreviewFpsRange();
        int l_first = 0;
        int minFps = (frameRates.get(l_first))[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
        int maxFps = (frameRates.get(l_first))[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
        param.setPreviewFpsRange(minFps, maxFps);

        // TODO: change preview size

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
        width = param.getPreviewSize().width;
        height = param.getPreviewSize().height;

        camera.startPreview();
    }

    public void clear() {
        Log.d("PictoParle", "clear board detector");
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        camera = null;
    }


    public void forceInactive(boolean value) {
        if (value)
            setInactive();
        canBeActive = !value;
    }

    public void setIntervalCovered(long intervalCovered) {
        this.interval_covered = intervalCovered;
    }
    public void setIntervalUncovered(long intervalUncovered) {
        this.interval_uncovered = intervalUncovered;
    }


    public interface SimpleBoardListener {
        void onRemovedBoard();

        void onNewHoverBoard(int boardID);

        void onBoardDown();
    }
}
