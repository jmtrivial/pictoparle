package com.jmfavreau.pictoparle;

import android.content.res.XmlResourceParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

class Board {


    public String name;

    public String orientation;

    public int id;

    public ArrayList<BoardPanel> panels;



    public Board(XmlResourceParser parser, float xdpmm, float ydpmm) throws IOException, XmlPullParserException {
        panels = new ArrayList<>();
        do {
                if (parser.getEventType() == XmlResourceParser.START_TAG) {
                    switch (parser.getName()) {
                        case "board":
                            this.name = parser.getAttributeValue(null, "name");
                            this.id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                            this.orientation = parser.getAttributeValue(null, "orientation");
                            break;

                        case "cells":
                            this.panels.add(new BoardPanel(parser, xdpmm, ydpmm));
                            break;
                    }
                }
        } while (parser.next() != XmlResourceParser.END_DOCUMENT);

    }



    Boolean isValid() {
        for(int i = 0; i != panels.size(); ++i)
            if (!panels.get(i).isValid())
                return false;
        return true;
    }



    public void updateSizes(float xdpmm, float ydpmm) {
        for(int i = 0; i != panels.size(); ++i)
            panels.get(i).updateSizes(xdpmm, ydpmm);
    }

}
