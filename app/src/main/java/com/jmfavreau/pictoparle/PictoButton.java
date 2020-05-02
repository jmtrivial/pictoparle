package com.jmfavreau.pictoparle;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;

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
                       @NotNull Pictogram pictogram, @NotNull Board board,
                       RobustGestureDetector.RobustGestureDetectorParams params) {
        super(context);

        this.pictogram = pictogram;
        this.audioRenderer = audioRenderer;

        // set image
        if (!pictogram.imageFileName.equals("")) {
            int id = getResources().getIdentifier(pictogram.imageFileName, "drawable", getContext().getPackageName());
            Drawable image = getResources().getDrawable(id);
            setBackground(image);
        }
        setLayoutParams(new ActionBar.LayoutParams(board.cellWidthPX, board.cellHeightPX));

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
