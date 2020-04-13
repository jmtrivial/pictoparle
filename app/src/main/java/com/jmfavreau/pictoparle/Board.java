package com.jmfavreau.pictoparle;

import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

class Board {

    public int cellWidthPX;
    public int cellHeightPX;

    public String name;

    public int nbColumns;
    public int nbRows;

    public int id;
    private ArrayList<ArrayList<Pictogram>> cells;

    private float xdpi;
    private float ydpi;

    public Board(XmlResourceParser parser, float xdpi, float ydpi) throws IOException, XmlPullParserException {
        this.xdpi = xdpi;
        this.ydpi = ydpi;
        do {
                if (parser.getEventType() == XmlResourceParser.START_TAG) {
                    switch (parser.getName()) {
                        case "board":
                            this.name = parser.getAttributeValue(null, "name");

                            double cellWidth = Double.parseDouble(parser.getAttributeValue(null, "cellWidth"));
                            double cellHeight = Double.parseDouble(parser.getAttributeValue(null, "cellHeight"));

                            this.cellWidthPX = toPXX(cellWidth);
                            this.cellHeightPX = toPXY(cellHeight);

                            this.id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                            break;

                        case "cells":
                            this.cells = new ArrayList<>();
                            break;
                        case "row":
                            this.cells.add(new ArrayList<Pictogram>());
                            break;
                        case "pictogram":
                            String txt = parser.getAttributeValue(null, "txt");
                            String audio = parser.getAttributeValue(null, "audio");
                            String image = parser.getAttributeValue(null, "image");
                            this.cells.get(this.cells.size() - 1).add(new Pictogram(txt, audio, image));
                    }
                }
        } while (parser.next() != XmlResourceParser.END_DOCUMENT);

        nbRows = this.cells.size();
        nbColumns = 0;
        for(int i = 0; i != this.cells.size(); ++i) {
            int n = this.cells.get(i).size();
            if (n > nbColumns) nbColumns = n;
        }
    }

    private int toPXX(double value) {
        // convert mm to inch, then apply dpi ratio
        return (int) (value * 0.03937 * xdpi);
    }
    private int toPXY(double value) {
        // convert mm to inch, then apply dpi ratio
        return (int) (value * 0.03937 * ydpi);
    }

    Boolean isValid() {
        return (this.cells != null) &&
                (this.nbColumns > 0) && (this.nbRows > 0) &&
                (this.cellWidthPX > 0) && (this.cellHeightPX > 0);
    }

    public Pictogram getPictogram(int i, int j) {
        if (this.cells.size() <= i || this.cells.get(i).size() < j)
            return Pictogram.empty();
        else
            return this.cells.get(i).get(j);
    }
}
