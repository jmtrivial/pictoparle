package com.jmfavreau.pictoparle.ui;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


import com.jmfavreau.pictoparle.AudioRenderer;
import com.jmfavreau.pictoparle.RobustGestureDetector;
import com.jmfavreau.pictoparle.core.Pictogram;

import org.jetbrains.annotations.NotNull;


class PictoButton extends View {
    private Pictogram pictogram;

    private RobustGestureDetector gestureDetector;


    private AudioRenderer audioRenderer;

    // default constructor, will not be used
    public PictoButton(Context context) {
        super(context);
    }

    public PictoButton(Context context, @NotNull AudioRenderer audioRenderer,
                       @NotNull Pictogram pictogram, int width, int height,
                       RobustGestureDetector.RobustGestureDetectorParams params) {
        super(context);

        this.pictogram = pictogram;
        this.audioRenderer = audioRenderer;

        String imageFileName = pictogram.imageFileName;
        if (imageFileName.equals(""))
            imageFileName = "empty";


        // set image
        if (pictogram.isFromDirectory()) {
            String pathName = pictogram.getFullImagePathName();
            Drawable image = Drawable.createFromPath(pathName);
            if (image != null)
                setBackground(image);
            else
                Log.w("PictoParle", "Unable to load " + pathName);
        }
        else {
            int id = getResources().getIdentifier(imageFileName, "drawable", getContext().getPackageName());
            Drawable image = getResources().getDrawable(id);
            setBackground(image);
        }
        setLayoutParams(new ActionBar.LayoutParams(width, height));

        gestureDetector = new RobustGestureDetector(context, new PictoButton.GestureListener(), params);

    }

    public void playSound() {
        if (pictogram != null && !pictogram.isEmpty()) {
            // play sound using text-to-speech
            audioRenderer.speak(pictogram.txt);

            // TODO: if an audio file is available, play it rather than using text-to-speech
        }
    }

    public void getRectOnScreen(Rect region) {
        int[] l = new int[2];
        getLocationOnScreen(l);
        int x = l[0];
        int y = l[1];
        int w = getWidth();
        int h = getHeight();
        region.set(x, y, x + w, y + h);
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }

    //private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    class GestureListener extends RobustGestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // detect if it is inside a button
            int x = (int) e.getX(e.getActionIndex());
            int y = (int) e.getY(e.getActionIndex());
            playSound();
            return true;
        }

        @Override
        public boolean onLongPress(MotionEvent e) {
            // will be used for the menu button
            return false;
        }

    }


}
