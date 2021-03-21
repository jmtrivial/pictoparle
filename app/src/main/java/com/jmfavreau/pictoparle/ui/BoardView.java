package com.jmfavreau.pictoparle.ui;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;


import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;
import com.jmfavreau.pictoparle.interactions.RobustGestureDetector;
import com.jmfavreau.pictoparle.core.Board;
import com.jmfavreau.pictoparle.core.BoardPanel;

import java.util.ArrayList;

class BoardView extends LinearLayout {


    private Context context;
    private ArrayList<ImageButton> closeButtons;

    private boolean horizontal;

    private ArrayList<PictoButton> buttons;

    private PictoParleActivity activity;


    // default constructor, will not be used
    public BoardView(Context context) {
        super(context);
    }

    public BoardView(Context context, Board board,
                     PictoParleActivity pictoParleActivity,
                     RobustGestureDetector.RobustGestureDetectorParams params) {
        super(context);

        if (board.orientation.equals("vertical")) {
            setOrientation(LinearLayout.VERTICAL);
            horizontal = false;
        }
        else {
            setOrientation(LinearLayout.HORIZONTAL);
            horizontal = true;
        }
        FrameLayout.LayoutParams lParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);

        setLayoutParams(lParams);

        this.activity = pictoParleActivity;

        this.context = context;

        buttons = new ArrayList<>();
        closeButtons = new ArrayList<>();

        int space = computeMainSpaceSize(board);

        for (int i = 0; i != board.panels.size(); ++i) {
            if (i != 0)
                addSpacer(space);
            addPanel(board.panels.get(i), params, board.panels.size() == 1);

        }
    }

    private void addSpacer(int space) {
        Space spacer = new Space(context);
        if (horizontal)
            spacer.setLayoutParams(new ActionBar.LayoutParams(space, ActionBar.LayoutParams.MATCH_PARENT));
        else
            spacer.setLayoutParams(new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, space));
        addView(spacer);
    }

    private int computeMainSpaceSize(Board board) {
        int pSize = 0;

        if (board.panels.size() <= 1) {
            return pSize;
        }

        for(int i = 0; i != board.panels.size(); ++i) {
            pSize += computeSize(board.panels.get(i));
        }

        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(size);

        if (horizontal)
            return (size.x - pSize) / (board.panels.size() - 1);
        else
            return (size.y - pSize) / (board.panels.size() - 1);
    }


    private void addPanel(BoardPanel panel,
                          RobustGestureDetector.RobustGestureDetectorParams params,
                          boolean single) {
        Point span = computeSpanSize(panel, single);

        LinearLayout pView = new LinearLayout(context);
        pView.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        pView.setLayoutParams(lParams);

        for(int i = panel.nbRows - 1; i >= 0; --i) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            pView.addView(row);
            for(int j = 0; j != panel.nbColumns; ++j) {
                if (j != 0) {
                    Space spacer = new Space(context);
                    spacer.setLayoutParams(new ActionBar.LayoutParams(span.x, panel.cellHeightPX));
                    row.addView(spacer);
                }

                try {
                    PictoButton picto = new PictoButton(context, activity.audioRenderer,
                            panel.getPictogram(i, j), panel.cellWidthPX, panel.cellHeightPX,
                            panel.cellPaddingXPX, panel.cellPaddingYPX,
                            params);
                    buttons.add(picto);
                    row.addView(picto);
                }
                catch (Exception e) {
                    Log.e("PictoParle", "cannot load a pictogram, ignore it");
                }
            }
            if (i != 0) {
                addHorizontalSpacer(pView, panel, span.x, span.y, i);
            }
        }

        if (panel.nbRows == 0 && panel.hasQuitButton()) {
            addHorizontalSpacer(pView, panel, span.x, span.y, -1);
        }


        addView(pView);

    }

    private void addHorizontalSpacer(LinearLayout pView, BoardPanel panel,
                                     int spanPX, int spanPY,
                                     int row) {
        LinearLayout spaceRow = new LinearLayout(context);
        spaceRow.setOrientation(LinearLayout.HORIZONTAL);
        pView.addView(spaceRow);

        Space spacer = new Space(context);
        spacer.setLayoutParams(new ActionBar.LayoutParams(panel.cellWidthPX, spanPY));
        spaceRow.addView(spacer);

        if (panel.hasQuitButton() && (row == panel.nbRows - 1 - panel.getRowPositionQuitButton())) {
            ImageButton closeButton = new ImageButton(context);
            closeButton.setImageResource(R.drawable.cross);
            closeButton.setAdjustViewBounds(true);
            closeButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int sizeX = panel.widthQuitButtonPX;
            int sizeY = panel.heightQuitButtonPX;
            if (sizeX <= 0) sizeX = spanPX;
            if (sizeY <= 0) sizeY = spanPY;
            closeButton.setLayoutParams(new ActionBar.LayoutParams(sizeX, sizeY));
            closeButton.setBackgroundColor(Color.WHITE);
            closeButton.setVisibility(View.INVISIBLE);
            closeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    activity.back();
                }
            });
            spaceRow.addView(closeButton);
            closeButtons.add(closeButton);
        }
    }


    private int computeSize(BoardPanel panel) {
        if (panel.nbRows == 0) {
            if (panel.hasQuitButton()) {
                if (horizontal) {
                    return panel.widthQuitButtonPX;
                } else {
                    return panel.heightQuitButtonPX;
                }
            }
            else
                return 0;
        }
        else {
            Point span = computeSpanSize(panel, false);
            if (horizontal) {
                return panel.nbColumns * (panel.cellWidthPX + span.x) - span.x;
            } else {
                return panel.nbRows * (panel.cellHeightPX + span.y) - span.y;
            }
        }
    }

    private Point computeSpanSize(BoardPanel panel, boolean single) {
        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getRealSize(size);

        if (panel.nbColumns == 0 || panel.nbRows == 0)
            return new Point(0, 0);

        int x = 0;
        if (panel.nbColumns > 1)
            x = (int) Math.floor(((float) size.x - panel.nbColumns * panel.cellWidthPX) / (panel.nbColumns - 1));
        int y = 0;
        if (panel.nbRows > 1)
            y = (int) Math.floor(((float) size.y - panel.nbRows * panel.cellHeightPX) / (panel.nbRows - 1));

        if (!single) {
            if (horizontal) { x = y; }
            else { y = x; }
        }
        return new Point(x, y);
    }


    public void setManual(boolean b) {
        for(int i = 0; i != closeButtons.size(); ++i) {
            if (b) {
                closeButtons.get(i).setVisibility(View.VISIBLE);
            } else {
                closeButtons.get(i).setVisibility(View.INVISIBLE);
            }
        }

    }


    public void initGestureDetectors() {
        for(int i = 0; i != buttons.size(); ++i)
            buttons.get(i).resetGestureDetector();
    }
}
