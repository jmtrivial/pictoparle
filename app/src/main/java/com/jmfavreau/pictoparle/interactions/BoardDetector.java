package com.jmfavreau.pictoparle.interactions;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BoardDetector  {
    private final SimpleBoardListener boardListener;
    private boolean active;
    private boolean init;
    private boolean covered;
    private long interval_covered;
    private long interval_uncovered;
    private Camera camera;

    private SurfaceTexture surfaceTexture;
    private boolean canBeActive;
    private boolean errorOnCreation;
    private Context context;

    private Runnable onBoardDown;
    private Runnable onRemoveBoard;
    private Runnable onNewHoverBoard;
    private int idCodeDetected;

    private static final String ID_CODE = "ID_CODE";
    private static final int THRESHOLD_COVERED = 5;
    private DataMatrixReader reader;

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

    private CameraWorkerThread workerThread = null;
    private Handler uiHandler = new Handler();

    public boolean isWaiting() {
        return workerThread != null;
    }


    /** this class creates the camera in a separated thread, such that it will not
     * freeze the GUI, and gives a way to wait the choosen number of milliseconds before
     * processing the preview. */
    private class CameraWorkerThread extends HandlerThread {
        protected  Handler workerHandler;
        private Runnable runnerInactive;
        private Runnable runnerActive;
        private Runnable runnableOpenCam;
        private Runnable runnableStopCamera;


        CameraWorkerThread() {
            super("CameraHandlerThread");

            // prepare the runnables
            runnerInactive = new Runnable() {
                @Override
                public void run() {
                    if (camera != null)
                        camera.stopPreview();
                }
            };

            runnerActive = new Runnable() {
                @Override
                public void run() {
                    if (camera == null)
                        return;

                    // create a surface to get resulting images
                    if (surfaceTexture == null) {
                        surfaceTexture = new SurfaceTexture(10);
                    }
                    try {
                        camera.setPreviewTexture(surfaceTexture);
                    } catch (Exception e) {
                        Log.e("PictoParle", "Cannot create a surface texture: " + e.toString());
                        if (camera != null)
                            camera.stopPreview();
                        setErrorInActivation();
                        return;
                    }

                    // set parameters
                    Camera.Parameters param = camera.getParameters();

                    if (covered) {

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

                        // choose the smallest image
                        List<Camera.Size> prevResolutions = param.getSupportedPreviewSizes();
                        Collections.sort(prevResolutions, new Comparator<Camera.Size>() {
                            public int compare(final Camera.Size a, final Camera.Size b) {
                                return a.width * a.height - b.width * b.height;
                            }
                        });

                        param.setPreviewSize(prevResolutions.get(0).width, prevResolutions.get(0).height);

                        // set preview framerate
                        List<int[]> frameRates = param.getSupportedPreviewFpsRange();
                        int l_first = 0;
                        int minFps = (frameRates.get(l_first))[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
                        int maxFps = (frameRates.get(l_first))[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                        param.setPreviewFpsRange(minFps, maxFps);


                    }
                    else {
                        param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);

                        // set focus mode
                        if (param.getFocusMode() != null) {
                            List<String> focusModes = param.getSupportedFocusModes();
                            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO))
                                param.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                            else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
                                param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                            else
                                param.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                        }

                        // create a region corresponding to the center
                        List<Camera.Area> regions = new ArrayList<>();
                        Camera.Area center = new Camera.Area(new Rect(-200, -200, 200, 200), 1000);
                        regions.add(center);

                        // set this region as a focus area if possible
                        if (param.getMaxNumFocusAreas() > 0) {
                            param.setFocusAreas(regions);
                        }

                        // set this region as a metering for the brightness adjustment if possible
                        if (param.getMaxNumMeteringAreas() > 0) {
                            param.setMeteringAreas(regions);
                        }

                        // choose resolution
                        List<Camera.Size> prevResolutions = param.getSupportedPreviewSizes();
                        Collections.sort(prevResolutions, new Comparator<Camera.Size>() {
                            public int compare(final Camera.Size a, final Camera.Size b) {
                                return b.width * b.height - a.width * a.height;
                            }
                        });

                        param.setPreviewSize(prevResolutions.get(0).width, prevResolutions.get(0).height);
                    }

                    // set preview format
                    List<Integer> formats = param.getSupportedPreviewFormats();
                    if (formats != null) {
                        if (formats.contains(ImageFormat.NV16)) {
                            param.setPreviewFormat(ImageFormat.NV16);
                        } else if (formats.contains(ImageFormat.NV21)) {
                            param.setPreviewFormat(ImageFormat.NV21);
                        } else {
                            Log.w("PictoParle", "Cannot select a valid image format.");
                        }
                    }

                    try {
                        camera.setParameters(param);
                    }
                    catch (RuntimeException e) {
                        // might occur on Virtual environments, ignore it
                        Log.w("PictoParle", "unable to set camera parameters");
                    }

                    if (reader == null)
                        reader = new DataMatrixReader();

                    // set a preview callback to handle camera images
                    // TODO: use setPreviewCallbackWithBuffer()?
                    camera.setPreviewCallback(new Camera.PreviewCallback() {
                        // inspired from https://stackoverflow.com/a/39948596
                        @Override
                        public void onPreviewFrame(byte[] data, Camera cam) {
                            if (active) {
                                long releaseTime = SystemClock.currentThreadTimeMillis();
                                if (covered) releaseTime += interval_uncovered;
                                else releaseTime += interval_covered;

                                Camera.Size size = cam.getParameters().getPreviewSize();

                                int nbPixels = size.width * size.height;

                                // when resolution changed but we obtain a preview from the previous
                                // resolution, and it's not with the good resolution...
                                if (data.length <= nbPixels)
                                    return;

                                boolean cov = isCovered(data, nbPixels);

                                if (init) {
                                    if (cov) {
                                        uiHandler.post(onBoardDown);
                                    }
                                    init = false;
                                } else {
                                    if (cov != covered) {
                                        if (cov)
                                            uiHandler.post(onBoardDown);
                                        else
                                            uiHandler.post(onRemoveBoard);
                                    } else {
                                        if (!covered) {
                                            Integer idcode = decode(data, size);

                                            if (idcode != null && idcode >= 0) {
                                                idCodeDetected = idcode;
                                                uiHandler.post(onNewHoverBoard);
                                            }
                                            else if (idcode != null && idcode == -1) {
                                                // if a qrcode has been seen, but cannot be recognize
                                                // we try it immediately
                                                releaseTime = 0;
                                            }
                                        }
                                    }
                                }
                                try {
                                    // see https://stackoverflow.com/a/5837955
                                    long time = releaseTime - SystemClock.currentThreadTimeMillis();
                                    if (time > 0) {
                                        Thread.sleep(time);
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }


                        private Integer decode(byte[] data, Camera.Size size) {

                            // convert the image to a parsable version
                            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                                    data, size.width, size.height, 0, 0, size.width, size.height, false);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                            /* USED TO DEBUG FINE TUNING OF THE CAMERA {
                                YuvImage image = new YuvImage(data, ImageFormat.NV21,
                                        size.width, size.height, null);
                                Rect rectangle = new Rect();
                                rectangle.bottom = size.height;
                                rectangle.top = 0;
                                rectangle.left = 0;
                                rectangle.right = size.width;

                                String root = Environment.getExternalStorageDirectory().toString();
                                Calendar now = Calendar.getInstance();
                                File file = new File(root, "pictoparle-" + now.getTimeInMillis() + ".jpg");
                                if (file.exists ())
                                    file.delete ();
                                try {
                                    FileOutputStream out = new FileOutputStream(file);
                                    image.compressToJpeg(rectangle, 70, out);
                                    out.flush();
                                    out.close();

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


                            } */

                            // use zxing to read the QRCode
                            try {
                                Result result = reader.decode(bitmap);
                                String text = result.getText();
                                Log.d("PictoParle", "Found QRcode: " + text);
                                return Integer.parseInt(text);

                            } catch (NotFoundException e) {
                            } catch (ChecksumException e) {
                                Log.d("PictoParle", "checksum exception");
                                return -1;
                            } catch (FormatException e) {
                                Log.d("PictoParle", "format exception (checksum)");
                                return -1;
                            }

                            return null;
                        }

                        private boolean isCovered(byte[] data, int nbPixels) {
                            float val = 0;
                            int step = 5; // do not consider all pixels to speedup the process
                            for (int i = 0; i < nbPixels; i += step) {
                                val += data[i] & 0xFF;
                            }
                            val /= nbPixels;
                            val *= step;
                            return (val < THRESHOLD_COVERED);
                        }
                    });
                    camera.startPreview();
                }
            };

            runnableOpenCam = new Runnable() {
                @Override
                public void run() {

                    if (camera != null) {
                        camera.release();
                    }

                    camera = getCameraInstance();


                    notifyCameraOpened();
                }

            };

            runnableStopCamera = new Runnable() {

                @Override
                public void run() {
                    if (camera != null) {
                        camera.stopPreview();
                    }
                }
            };
        }


        synchronized void notifyCameraOpened() {
            notify();
        }

        @Override
        protected void onLooperPrepared() {
            workerHandler = new Handler();

            workerHandler.post(runnableOpenCam);
            workerHandler.post(runnerActive);
        }

        void setActive() {
            if (workerHandler != null)
                workerHandler.post(runnerActive);
        }


        void setInactive() {
            if (workerHandler != null)
                workerHandler.post(runnerInactive);
        }

        // TODO: remove that
        public void stopCamera() {
            workerHandler.post(runnableStopCamera);

        }
    }

    public void start() {

        workerThread = new CameraWorkerThread();
        workerThread.start();

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

        surfaceTexture = null;
        reader = null;

        // by default, the board is covered
        covered = true;

        onRemoveBoard = new Runnable() {
            @Override
            public void run() {
                if (covered) {
                    covered = false;
                    boardListener.onRemovedBoard();
                    // stop and start with the correct parameters
                    workerThread.setInactive();
                    workerThread.setActive();
                }
            }
        };

        onBoardDown = new Runnable() {
            @Override
            public void run() {
                if (!covered) {
                    covered = true;
                    boardListener.onBoardDown();
                    // stop and start with the correct parameters
                    workerThread.setInactive();
                    workerThread.setActive();
                }
            }
        };

        onNewHoverBoard = new Runnable() {
            @Override
            public void run() {
                if (!covered) {
                    boardListener.onNewHoverBoard(idCodeDetected);
                }
            }
        };


    }




    private void setErrorInActivation() {
        errorOnCreation = true;
        camera = null;
        active = false;
        Toast.makeText(context, "Impossible d'activer la caméra. La détection de planche n'est pas possible.", Toast.LENGTH_SHORT).show();
    }


    public void setInactive() {
        if (this.active) {
            workerThread.setInactive();
            Log.d("PictoParle", "set board detector inactive");
            this.active = false;
        }
    }

    public void setActive() {
        if (init || (!errorOnCreation && canBeActive)) {
            Log.d("PictoParle", "set board detector active");
            this.active = true;
            workerThread.setActive();
        }
    }


    public void clear() {
        if (camera != null) {
            workerThread.stopCamera();
            workerThread = null;
        }
    }


    public void forceInactive(boolean value) {
        // if value = false, the camera will be stopped by the next preview handling
        clear();
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
