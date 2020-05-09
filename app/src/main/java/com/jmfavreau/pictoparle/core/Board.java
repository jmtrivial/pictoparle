package com.jmfavreau.pictoparle.core;

import android.app.Activity;
import android.content.res.XmlResourceParser;

import com.jmfavreau.pictoparle.PictoParleActivity;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class Board {


    public String name;

    public String orientation;

    public int id;

    public ArrayList<BoardPanel> panels;

    public boolean coreBoard;

    private boolean active;
    private PictoParleActivity activity;

    public Board(PictoParleActivity activity, XmlResourceParser parser, float xdpmm, float ydpmm, boolean coreBoard) throws IOException, XmlPullParserException {
        panels = new ArrayList<>();
        this.activity = activity;
        this.coreBoard = coreBoard;
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

        active = activity.getPreferenceIsActiveBoard(id);
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean a) {
        active = a;
        activity.setPreferenceIsActiveBoard(id, a);
    }
}
