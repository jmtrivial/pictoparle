package com.jmfavreau.pictoparle;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;

import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;


import java.util.ArrayList;

class BoardView extends LinearLayout {
    private ImageButton closeButton;
    private Board board;

    private ArrayList<PictoButton> buttons;

    private PictoParleActivity activity;

    private int horizontalSpanPX;
    private int verticalSpanPX;

    // default constructor, will not be used
    public BoardView(Context context) {
        super(context);
    }

    public BoardView(Context context, Board board,
                     PictoParleActivity pictoParleActivity,
                     RobustGestureDetector.RobustGestureDetectorParams params) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        this.board = board;
        this.activity = pictoParleActivity;
        computeSpans();

        //setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

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
                PictoButton picto = new PictoButton(context, activity.audioRenderer,
                        board.getPictogram(i, j), board, params);
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

                if (i == board.nbRows - 1) {
                    closeButton = new ImageButton(context);
                    closeButton.setImageResource(R.drawable.cross);
                    closeButton.setAdjustViewBounds(true);
                    closeButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    closeButton.setLayoutParams(new ActionBar.LayoutParams(horizontalSpanPX, verticalSpanPX));
                    closeButton.setBackgroundColor(Color.WHITE);
                    closeButton.setVisibility(View.INVISIBLE);
                    closeButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            activity.back();
                        }
                    });
                    spaceRow.addView(closeButton);
                }

            }
        }


    }


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


    public void setManual(boolean b) {
        if (b) {
            closeButton.setVisibility(View.VISIBLE);
        }
        else {
            closeButton.setVisibility(View.INVISIBLE);
        }

    }


}
