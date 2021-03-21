package com.jmfavreau.pictoparle.interactions;


import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implement a gesture detector for double-tap, that ignore the other fingers
 *
 *
 */
public class RobustGestureDetector {
    private final SimpleOnGestureListener gestureListener;
    private final RobustGestureDetectorParams params;
    private Context context;

    // reset all the robust detectors
    public void resetAllRobustGestureDetectors() {
        if (register != null)
            register.resetAll();
    }

    private class RobustGestureDetectorRegister {

        ArrayList<RobustGestureDetector> detectors;

        public RobustGestureDetectorRegister() {
            detectors = new ArrayList<>();
        }

        public void addDetector(RobustGestureDetector detector) {
            detectors.add(detector);
        }


        public void resetAll() {
            for(int i = 0; i != detectors.size(); ++i)
                detectors.get(i).resetInternal();
        }

        public float getMinRawYTouch() {
            float min = -1;
            for(int i = 0; i != detectors.size(); ++i) {
                float y = detectors.get(i).getMinRawYTouch();
                if (min < 0 || (y >= 0 && y < min))
                    min = y;
            }
            return min;
        }
    }

    private float getMinRawYTouch() {
        float min = -1;
        for (DownEvent e: openTaps.values()) {
            if (min < 0 || e.rawY < min)
                min = e.rawY;
        }
        return min;
    }

    private void resetInternal() {
        shortTaps.clear();
        openTaps.clear();
    }

    private static RobustGestureDetectorRegister register = null;


    private class ShortTap {
        /* end timestamp of single tap  */
        public long time;
        /* X coordinate of the single tap */
        public float tapX;
        /* Y coordinate of the single tap */
        public float tapY;

        public ShortTap(long time, float tapX, float tapY) {
            this.time = time;
            this.tapX = tapX;
            this.tapY = tapY;
        }

        public float distance2(float x, float y) {
            return (x - tapX) * (x - tapX) + (y - tapY) * (y - tapY);
        }
    }

    private class DownEvent {
        /* time when this event has been detected */
        public long downTime;
        /* X coordinate of the down event */
        public float downX;
        /* Y coordinate of the down event */
        public float downY;
        /* X coordinate of the down event (raw coordinate) */
        public float rawX;
        /* Y coordinate of the down event (raw coordinate) */
        public float rawY;
        /* true if the finger that did this event
          do not share a time on the screen with another finger during its journey. */
        public boolean single;

        /* if this open event is the possible opening
        * of a double tap, remember it */
        private boolean isDouble;

        public DownEvent(long downTime,
                         float downX, float downY,
                         float rawX, float rawY,
                         boolean single) {
            this.downTime = downTime;
            this.downX = downX;
            this.downY = downY;
            this.rawX = rawX;
            this.rawY = rawY;
            this.isDouble = false;
            this.single = single;
        }

        public float distance2(float x, float y) {
            return (x - downX) * (x - downX) + (y - downY) * (y - downY);
        }

        public boolean isDoubleTap() {
            return isDouble;
        }
        public void isDoubleTap(Boolean v) {
            this.isDouble = true;
        }

    }

    private ArrayList<ShortTap> shortTaps;

    private Map<Integer, DownEvent> openTaps;


    public RobustGestureDetector(Context context,
                                 SimpleOnGestureListener gestureListener,
                                 RobustGestureDetectorParams params) {
        this.context = context;
        this.gestureListener = gestureListener;

        this.shortTaps = new ArrayList<>();
        this.openTaps = new HashMap<>();

        /* parameters for taps */
        this.params = params;

        // register the current detector to the
        if (register == null) {
            register = new RobustGestureDetectorRegister();
        }
        register.addDetector(this);
    }


    public void downAction(MotionEvent event, int id) {
        // when we detect a finger (first or not) going down


        float x = event.getX(id);
        float y = event.getY(id);

        float rawX = event.getRawX();
        float rawY = event.getRawY();

        long time = System.currentTimeMillis();

        // we create the corresponding down event
        DownEvent de = new DownEvent(time, x, y, rawX, rawY, this.openTaps.size() == 0);

        // we check if it exists a corresponding event in the stored single taps.
        int idBest = -1;
        float dist = -1;
        for(int i = 0; i != shortTaps.size(); ) {
            float d = shortTaps.get(i).distance2(x, y);
            if (time < shortTaps.get(i).time + this.params.doubleTapTimeout) {
                if ((d <= this.params.getMaxSquaredDistanceDoubleTap()) && ((dist == -1) || (dist > d))) {
                    dist = d;
                    idBest = i;
                }
                ++i;
            }
            else {
                shortTaps.remove(i);
            }
        }

        if (idBest != -1) {
            // if yes, we associate the previous tap as the first step
            // of this double tap
            de.isDoubleTap(true);
            shortTaps.remove(idBest);
        }

        for (DownEvent e: openTaps.values()) {
                e.single = false;
        }

        // we add this event to the open taps
        this.openTaps.put(event.getPointerId(id), de);

    }

    public void upAction(MotionEvent event, int id) {

        float x = event.getX(id);
        float y = event.getY(id);

        float rawX = event.getRawX();
        float rawY = event.getRawY();

        long time = System.currentTimeMillis();
        int pointerID = event.getPointerId(id);

        // find the corresponding down event
        DownEvent down = openTaps.get(pointerID);
        if (down != null) {
            long diffTime = time - down.downTime;
            float d = down.distance2(x, y);
            // first check if it is a tap (duration and location)
            if (diffTime < this.params.tapTimeout && d <= params.getMaxSquaredDistanceTap()) {
                ShortTap tap = new ShortTap(time, x, y);

                // check if it is a double tap
                if (down.isDoubleTap()) {
                    if (rawY - register.getMinRawYTouch() <= params.thresholdYOtherTouch) {
                        gestureListener.onDoubleTap(event);
                    }
                    else {
                        Log.d("RobustGestureDetector", "Detecting a possible double tap with a palm");
                    }
                }
                else {
                    // it is not a double tap, thus we add this event as a tap
                    shortTaps.add(tap);
                }
            }

            // finally remove the beginning
            openTaps.remove(pointerID);
        }


    }


    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: downAction(event, 0); break;
            case MotionEvent.ACTION_MOVE: /* nothing to do (we only consider basic moves) */ break;
            case MotionEvent.ACTION_POINTER_DOWN: downAction(event, event.getActionIndex()); break;
            case MotionEvent.ACTION_UP: upAction(event, 0); break;
            case MotionEvent.ACTION_POINTER_UP: upAction(event, event.getActionIndex()); break;
            case MotionEvent.ACTION_OUTSIDE: /* nothing to do (we only have a single view in the screen) */ break;
            case MotionEvent.ACTION_CANCEL:
                // if we detect a cancel action
                // we remove all the stored starting events and open double taps
                // and detected single taps.
                shortTaps.clear();
                openTaps.clear();
                break;
        }

        return true;

    }


    static public class SimpleOnGestureListener {

        public boolean onDown(MotionEvent e) {
            return false;
        }

        public boolean onLongPress(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

    }

    static public class RobustGestureDetectorParams {
        public int doubleTapTimeout; /* max time between two taps */
        public int tapTimeout;
        public float maxDistDoubleTapMM; /* max distance between two taps in a double tap (unit: millimeter) */
        public float maxDistTapMM; /* max distance between down and up (unit: millimeter) */
        public float thresholdYOtherTouch; /* maximum distance in y direction from other touch before this other is considered as a
                                            disrupting touch (in case the palm is touching the screen)*/
        private float dpmm;

        public RobustGestureDetectorParams(int doubleTapTimeout,
                                           int tapTimeout,
                                           float maxDistDoubleTapMM,
                                           float maxDistTapMM,
                                           float thresholdYOtherTouch) {
            this.doubleTapTimeout = doubleTapTimeout;
            this.tapTimeout = tapTimeout;
            this.maxDistDoubleTapMM = maxDistDoubleTapMM;
            this.maxDistTapMM = maxDistTapMM;
            this.dpmm = 1.0F;
            this.thresholdYOtherTouch = thresholdYOtherTouch;
        }

        public void setDPMM(float dpmm) {
            this.dpmm = dpmm;
        }

        public int getMaxSquaredDistanceDoubleTap() {
            return (int)(maxDistDoubleTapMM * maxDistDoubleTapMM * dpmm * dpmm);
        }

        public int getMaxSquaredDistanceTap() {
            return (int)(maxDistTapMM * maxDistTapMM * dpmm * dpmm);
        }
    }
}
