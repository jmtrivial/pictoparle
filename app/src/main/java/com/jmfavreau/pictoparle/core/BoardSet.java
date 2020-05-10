package com.jmfavreau.pictoparle.core;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class BoardSet {

    private final PictoParleActivity activity;
    /**
     * true DPI cannot be get from the Android API. Thus it is a parameter of the application
     */
    /* TODO: add a menu entry to change these values */
    private float xdpmm;
    private float ydpmm;


    protected ArrayList<Board> boards;
    private int selected;

    private boolean hasSelected;

    private String lang;

    public BoardSet(PictoParleActivity activity, Resources resources, String packagename, float xdpmm, float ydpmm) {
        this.xdpmm = xdpmm;
        this.ydpmm = ydpmm;
        this.activity = activity;

        // select default language
        lang = "fr";

        // by default, no board is selected
        hasSelected = false;

        // load boards from assets
        try {
            loadXMLResources(resources, packagename);
        } catch (IOException e) {
            Log.e("PictoParle", "IO Error while reading a board.");
        } catch (XmlPullParserException e) {
            Log.e("PictoParle", "XML Error while reading a board.");
        }

        // load boards from files
        File boardDir = new File(getBoardDirectory());
        if (boardDir.isDirectory())
            for (File child : boardDir.listFiles()) {
                if (child.isDirectory()) {
                    try {
                        loadFromDir(child.toString());
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


    }

    public boolean setSelectedDefault() {
        /* the selected board is the first of the list */
        for(int i = 0; i != boards.size(); ++i)
            if (boards.get(i).isActive()) {
                selected = boards.get(i).id;
                hasSelected = true;
                return true;
            }
        hasSelected = false;
        return false;
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
                    Board b = new Board(activity, parser, xdpmm, ydpmm);
                    if (b.isValid()) {
                        boards.add(b);
                    } else {
                        Log.e("PictoParle", "invalid board: " + b.name);
                    }
                }
            }
        } while (xrp.next() != XmlResourceParser.END_DOCUMENT);

        // the selected one is the first one
        setSelectedDefault();
    }

    public String getLang() {
        return lang;
    }

    public boolean getHasSelected() {
        return hasSelected;
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int i) {
        selected = i;
        hasSelected = true;
    }

    public Board getSelectedBoard() {
        if (!hasSelected)
            return null;
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

    public boolean hasBordByID(int id) {
        for (int i = 0; i != boards.size(); i++) {
            if (boards.get(i).id == id) {
                return true;
            }
        }
        return false;
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

    public boolean loadFromDir(String boardFileLoadingLocation) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(new InputStreamReader(new FileInputStream(new File(boardFileLoadingLocation + "/board.xml"))));

        Board b = new Board(activity, parser, xdpmm, ydpmm, boardFileLoadingLocation);
        if (b.isValid() && !hasBordByID(b.id)) {
            boards.add(b);
            return true;
        }
        return false;
    }

    public String getBoardDirectory() {
        return activity.getApplicationContext().getFilesDir().toString() + "/" + "boards/";
    }

    public boolean removeBoardById(int id) {
        for(int i = 0; i != boards.size(); i++) {
            if (boards.get(i).id == id) {
                if (!boards.get(i).isCoreBoard()) {
                    boards.remove(i);
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
