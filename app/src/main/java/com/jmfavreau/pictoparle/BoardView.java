package com.jmfavreau.pictoparle;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.RequiresApi;


import java.util.ArrayList;

class BoardView extends LinearLayout {
    private Board board;

    //private GestureDetector gestureDetector;
    private RobustGestureDetector gestureDetector;

    private float slideThreshold;

    private ArrayList<PictoButton> buttons;

    private PictoParleActivity activity;

    private int horizontalSpanPX;
    private int verticalSpanPX;

    // default constructor, will not be used
    public BoardView(Context context) {
        super(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public BoardView(Context context, Board board, TextToSpeech tts, PictoParleActivity pictoParleActivity) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        this.board = board;
        this.activity = pictoParleActivity;
        computeSpans();


        buttons = new ArrayList<>();
        for(int i = board.nbRows - 1; i >= 0; --i) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            addView(row);
            for(int j = 0; j != board.nbColumns; ++j) {
                if (j != 0) {
                    Space spacer = new Space(context);
                    spacer.setLayoutParams(new ActionBar.LayoutParams(horizontalSpanPX, board.cellHeightPX));
                    row.addView(spacer);
                }
                PictoButton picto = new PictoButton(context, tts, board.getPictogram(i, j), board);
                buttons.add(picto);
                row.addView(picto);
            }
            if (i != 0) {
                LinearLayout spaceRow = new LinearLayout(context);
                spaceRow.setOrientation(LinearLayout.HORIZONTAL);
                addView(spaceRow);

                Space spacer = new Space(context);
                spacer.setLayoutParams(new ActionBar.LayoutParams(board.cellWidthPX, verticalSpanPX));
                spaceRow.addView(spacer);
            }
        }

        // slide threshold correspond to the diagonal of a pictogram
        slideThreshold = board.cellHeightPX * board.cellHeightPX + board.cellWidthPX + board.cellWidthPX;
        //gestureDetector = new GestureDetector(context, new GestureListener());
        gestureDetector = new RobustGestureDetector(context, new GestureListener());



        // TODO: add here a button to open a menu
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void computeSpans() {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(size);

        verticalSpanPX = (int) Math.floor(((float) size.y - board.nbRows * board.cellHeightPX) / (board.nbRows - 1));
        horizontalSpanPX = (int) Math.floor(((float) size.x - board.nbColumns * board.cellWidthPX) / (board.nbColumns - 1));
        if (verticalSpanPX < 0)
            verticalSpanPX = 0;
        if (horizontalSpanPX < 0)
            horizontalSpanPX = 0;
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
            PictoButton picto = getPictogramAtXY(x, y);
            if (picto != null)
                picto.playSound();
            return true;
        }

        @Override
        public boolean onLargeMoveSingleFinger(MotionEvent e, float prevX, float prevY) {
            boolean result = false;
            float diffX = e.getX(e.getActionIndex()) - prevX;
            float diffY = e.getY(e.getActionIndex()) - prevY;
            float distance2 = (diffX * diffX + diffY * diffY);
            if (distance2 > slideThreshold) {
                if (Math.abs(diffX) < Math.abs(diffY)) {
                    if (diffY < 0) {
                        activity.setScreenVisible(true);
                    } else {
                        activity.setScreenVisible(false);
                    }
                    result = true;
                }
                else {
                    // send application to back
                    boolean sentAppToBackground = activity.moveTaskToBack(true);

                    if(!sentAppToBackground){
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_MAIN);
                        i.addCategory(Intent.CATEGORY_HOME);
                        activity.startActivity(i);
                    }

                }
            }
            return result;
        }

        @Override
        public boolean onLongPress(MotionEvent e) {
            // will be used for the menu button
            return false;
        }

    }

    private PictoButton getPictogramAtXY(int x, int y) {
        Rect region = new Rect();
        for(int i = 0; i != buttons.size(); ++i) {
            buttons.get(i).getRectOnScreen(region);
            if (region.contains(x, y)) {
                return buttons.get(i);
            }
        }
        return null;
    }

}
