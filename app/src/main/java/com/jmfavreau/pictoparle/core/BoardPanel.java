package com.jmfavreau.pictoparle.core;

import android.content.Context;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class BoardPanel {
    private double heightQuitButton;
    private double widthQuitButton;

    public int heightQuitButtonPX;
    public int widthQuitButtonPX;

    private int rowPositionQuitButton;


    private double cellWidth;
    private double cellHeight;
    public int cellWidthPX;
    public int cellHeightPX;

    public int nbColumns;
    public int nbRows;

    private float xdpmm;
    private float ydpmm;
    private String directory;
    private boolean hasFiles;

    private ArrayList<ArrayList<Pictogram>> cells;



    public BoardPanel(XmlPullParser parser, float xdpmm, float ydpmm,
                      Context context,
                      String directory) throws IOException, XmlPullParserException {
        this.directory = directory;
        loadFromParser(parser, xdpmm, ydpmm);
        if (directory == null)
            hasFiles = true;
        else
            checkHasFilesInDirectory(context);
    }


    private void checkHasFilesInDirectory(Context context) {
        hasFiles = true;
        for(int i = 0; i != this.cells.size(); ++i) {
            for(int j = 0; j != this.cells.get(i).size(); ++j) {
                if (!this.cells.get(i).get(j).checkHasFilesInDirectory(context)) {
                    hasFiles = false;
                    break;
                }
            }
        }
    }

    private void loadFromParser(XmlPullParser parser, float xdpmm, float ydpmm) throws IOException, XmlPullParserException {
        this.cells = new ArrayList<>();

        // by default, there is no qui button
        this.rowPositionQuitButton = -1;
        // by default, its width and height are "automatic"
        this.widthQuitButton = -1.0;
        this.heightQuitButton = -1.0;
        cellWidth = 0;
        cellHeight = 0;



        do {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                    case "cells":
                        String v = parser.getAttributeValue(null, "cellWidth");
                        if (v != null)  cellWidth = Double.parseDouble(v);
                        v = parser.getAttributeValue(null, "cellHeight");
                        if (v != null) cellHeight = Double.parseDouble(v);
                        break;
                    case "row":
                        this.cells.add(new ArrayList<Pictogram>());
                        break;
                    case "pictogram":
                        String txt = parser.getAttributeValue(null, "txt");
                        String audio = parser.getAttributeValue(null, "audio");
                        String image = parser.getAttributeValue(null, "image");
                        this.cells.get(this.cells.size() - 1).add(new Pictogram(txt, audio, image, directory));
                        break;
                    case "quit":
                        this.rowPositionQuitButton = this.cells.size();
                        String width = parser.getAttributeValue(null, "width");
                        String height = parser.getAttributeValue(null, "height");
                        if (width != null && !width.equals("auto")) {
                            this.widthQuitButton = Float.parseFloat(width);
                        }
                        if (height != null && !height.equals("auto")) {
                            this.heightQuitButton = Float.parseFloat(height);
                        }
                        break;

                }
            }
            else if (parser.getEventType() == XmlPullParser.END_TAG) {
                // end of cell description
                if (parser.getName().equals("cells"))
                    break;
            }
        } while(parser.next() != XmlPullParser.END_DOCUMENT);

        nbRows = this.cells.size();
        nbColumns = 0;
        for(int i = 0; i != this.cells.size(); ++i) {
            int n = this.cells.get(i).size();
            if (n > nbColumns) nbColumns = n;
        }

        updateSizes(xdpmm, ydpmm);
    }

    public void updateSizes(float xdpmm, float ydpmm) {
        this.xdpmm = xdpmm;
        this.ydpmm = ydpmm;
        this.cellWidthPX = toPXX(cellWidth);
        this.cellHeightPX = toPXY(cellHeight);
        this.heightQuitButtonPX = toPXX(heightQuitButton);
        this.widthQuitButtonPX = toPXX(widthQuitButton);
    }

    public boolean isValid() {
        return hasFiles && (this.cells != null) &&
                (((this.nbColumns > 0) && (this.nbRows > 0) &&
                (this.cellWidthPX > 0) && (this.cellHeightPX > 0)) ||
                        (this.hasQuitButton() &&
                                this.widthQuitButtonPX > 0 &&
                                this.heightQuitButtonPX > 0));
    }

    public boolean hasQuitButton() {
        return rowPositionQuitButton != -1;
    }

    public int getRowPositionQuitButton() {
        return rowPositionQuitButton;
    }

    private int toPXX(double value) {
        // convert mm to dot
        return (int) (value * xdpmm);
    }
    private int toPXY(double value) {
        // convert mm to dot
        return (int) (value * ydpmm);
    }

    public Pictogram getPictogram(int i, int j) {
        if (this.cells.size() <= i || this.cells.get(i).size() < j)
            return Pictogram.empty();
        else
            return this.cells.get(i).get(j);
    }
}
