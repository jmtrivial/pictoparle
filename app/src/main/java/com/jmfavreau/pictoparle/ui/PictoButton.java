package com.jmfavreau.pictoparle.ui;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


import com.jmfavreau.pictoparle.interactions.AudioRenderer;
import com.jmfavreau.pictoparle.interactions.RobustGestureDetector;
import com.jmfavreau.pictoparle.core.Pictogram;

import org.jetbrains.annotations.NotNull;



class PictoButton extends LinearLayout {
    private Pictogram pictogram;
    private RobustGestureDetector gestureDetector;

    private String audioFile;
    private AudioRenderer audioRenderer;

    // default constructor, will not be used
    public PictoButton(Context context) {
        super(context);
    }

    public PictoButton(Context context, @NotNull AudioRenderer audioRenderer,
                       @NotNull Pictogram pictogram, int width, int height,
                       int paddingX, int paddingY,
                       RobustGestureDetector.RobustGestureDetectorParams params) {
        super(context);

        this.pictogram = pictogram;
        this.audioRenderer = audioRenderer;

        if (pictogram.audioFileName != null && pictogram.audioFileName != "") {
            audioFile = pictogram.getFullAudioPathName();
        }
        else
            audioFile = null;


        String imageFileName = pictogram.imageFileName;
        if (imageFileName == null || imageFileName.equals(""))
            imageFileName = "empty";

        View imageView = new View(context);
        int x = 0;
        int y = 0;
        // set image
        if (pictogram.isImageFromDirectory()) {
            String pathName = pictogram.getFullImagePathName();
            Drawable image = Drawable.createFromPath(pathName);

            if (image != null) {
                imageView.setBackground(image);
                x = image.getIntrinsicWidth();
                y = image.getIntrinsicHeight();
            }
            else
                Log.w("PictoParle", "Unable to load " + pathName);
        }
        else {
            int id = getResources().getIdentifier(imageFileName, "drawable", getContext().getPackageName());
            Drawable image = getResources().getDrawable(id);
            if (image != null) {
                imageView.setBackground(image);
                x = image.getIntrinsicWidth();
                y = image.getIntrinsicHeight();
            }
        }

        if (x != 0 && y != 0) {
            RelativeLayout.LayoutParams parameter = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            int[] paddings = computePaddings(width, height, paddingX, paddingY, x, y);
            parameter.setMargins(paddings[0], paddings[1], paddings[0], paddings[1]);
            imageView.setLayoutParams(parameter);
            this.addView(imageView);
        }

        setLayoutParams(new ActionBar.LayoutParams(width, height));

        gestureDetector = new RobustGestureDetector(context, new PictoButton.GestureListener(), params);

    }

    private int[] computePaddings(int width, int height, int paddingX, int paddingY, int x, int y) {
        int targetWidth = width - 2 * paddingX;
        int targetHeight = height - 2 * paddingY;

        float ratio1 = (float) (targetWidth) / x;
        float ratio2 = (float) (targetHeight) / y;

        float finalWidth;
        float finalHeight;
        if (ratio1 < ratio2) {
            finalWidth = targetWidth;
            finalHeight = y * ratio1;
        }
        else {
            finalWidth = x * ratio2;
            finalHeight = targetHeight;

        }
        int[] result = new int[2];
        result[0] = Math.round((width - finalWidth) / 2);
        result[1] = Math.round((height - finalHeight) / 2);
        return result;
    }

    public void playSound() {
        if (pictogram != null) {
            // if an audio file is available, play it
            if (audioFile != null) {
                audioRenderer.playSound(audioFile);
            }
            else {
                // if not, use text-to-speech
                audioRenderer.speak(pictogram.txt);
            }
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
