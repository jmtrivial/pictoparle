package com.jmfavreau.pictoparle.core;

import android.app.Activity;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.jmfavreau.pictoparle.PictoParleActivity;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class Board {


    public String name;

    public String orientation;

    public int id;

    public ArrayList<BoardPanel> panels;


    public String directory;

    private boolean active;
    private PictoParleActivity activity;

    public Board(PictoParleActivity activity, XmlPullParser parser,
                 float xdpmm, float ydpmm) throws IOException, XmlPullParserException {
        this.activity = activity;
        this.directory = null;
        loadFromParser(parser, xdpmm, xdpmm);
        active = activity.getPreferenceIsActiveBoard(id);
    }

    public Board(PictoParleActivity activity, XmlPullParser parser,
                 float xdpmm, float ydpmm,
                 String directory) throws IOException, XmlPullParserException {
        this.activity = activity;
        this.directory = directory;
        loadFromParser(parser, xdpmm, xdpmm);
        active = activity.getPreferenceIsActiveBoard(id);

    }

    public void loadFromParser(XmlPullParser parser,
                               float xdpmm, float ydpmm)  throws IOException, XmlPullParserException {
        panels = new ArrayList<>();
        do {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    switch (parser.getName()) {
                        case "board":
                            this.name = parser.getAttributeValue(null, "name");
                            this.id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                            this.orientation = parser.getAttributeValue(null, "orientation");
                            break;

                        case "cells":
                            this.panels.add(new BoardPanel(parser, xdpmm, ydpmm,
                                    activity.getBaseContext(), this.directory));
                            break;
                    }
                }
        } while (parser.next() != XmlPullParser.END_DOCUMENT);
    }



    public Boolean isValid() {
        for(int i = 0; i != panels.size(); ++i)
            if (!panels.get(i).isValid())
                return false;
        return true;
    }

    public boolean isCoreBoard() {
        return directory == null;
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
