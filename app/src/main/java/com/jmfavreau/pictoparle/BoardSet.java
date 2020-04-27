package com.jmfavreau.pictoparle;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

public class BoardSet {

    /**
     * true DPI cannot be get from the Android API. Thus it is a parameter of the application
     */
    /* TODO: add a menu entry to change these values */
    private float xdpmm;
    private float ydpmm;


    protected ArrayList<Board> boards;
    private int selected;

    private String lang;

    public BoardSet(Resources resources, String packagename, float xdpmm, float ydpmm) {
        this.xdpmm = xdpmm;
        this.ydpmm = ydpmm;

        // select default language
        lang = "fr";

        // load boards
        try {
            loadXMLResources(resources, packagename);
        } catch (IOException e) {
            Log.e("PictoParle", "IO Error while reading a board.");
        } catch (XmlPullParserException e) {
            Log.e("PictoParle", "XML Error while reading a board.");
        }

        /* the selected board is the first of the list */
        selected = boards.get(0).id;
    }

    private void loadXMLResources(Resources resources, String packagename) throws IOException, XmlPullParserException {
        boards = new ArrayList<>();

        // load boards
        XmlResourceParser xrp = resources.getXml(R.xml.boards);
        do {

            if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                if (xrp.getName().equals("boards")) {
                    String lang = xrp.getAttributeValue(null, "lang");
                    if (lang != null) {
                        this.lang = lang;
                    }
                } else if (xrp.getName().equals("board")) {
                    int identifier = resources.getIdentifier(
                            xrp.getAttributeValue(null, "name"),
                            "xml", packagename);
                    XmlResourceParser parser = resources.getXml(identifier);
                    Board b = new Board(parser, xdpmm, ydpmm);
                    if (b.isValid()) {
                        boards.add(b);
                    } else {
                        Log.e("PictoParle", "invalid board: " + b.name);
                    }
                }
            }
        } while (xrp.next() != XmlResourceParser.END_DOCUMENT);

        // the selected one is the first one
        selected = boards.get(0).id;
    }

    public String getLang() {
        return lang;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int i) {
        selected = i;
    }

    public Board getSelectedBoard() {
        try {
            return getBoardByID(getSelected());
        }
        catch (NoSuchFieldException e) {
            return null;
        }
    }

    public int size() {
        return boards.size();
    }

    public Board get(int i) {
        return boards.get(i);
    }

    public Board getBoardByID(int id) throws NoSuchFieldException {
        for (int i = 0; i != boards.size(); i++) {
            if (boards.get(i).id == id) {
                return boards.get(i);
            }
        }
        throw new NoSuchFieldException();
    }

    public ArrayList<Board> getBoards() {
        return boards;
    }

    public boolean containsBoard(int boardID) {
        for(int i = 0; i != boards.size(); i++) {
            if (boards.get(i).id == boardID)
                return true;
        }
        return false;
    }

    public void updateSizes(float xdpmm, float ydpmm) {
        this.xdpmm = xdpmm;
        this.ydpmm = ydpmm;
        for(int i = 0; i != boards.size(); i++) {
            boards.get(i).updateSizes(xdpmm, ydpmm);
        }
    }
}
