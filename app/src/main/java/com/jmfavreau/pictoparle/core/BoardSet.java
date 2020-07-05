package com.jmfavreau.pictoparle.core;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;
import com.jmfavreau.pictoparle.ui.BoardManagerFragment;

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
    private float xdpmm;
    private float ydpmm;


    protected ArrayList<Board> boards;
    private int selected;

    private Board tmpBoard;

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

        tmpBoard = new Board(activity, parser, xdpmm, ydpmm, boardFileLoadingLocation);
        if (tmpBoard.isValid()) {
            if (hasBordByID(tmpBoard.id)) {
                Board b_old;
                try {
                    b_old = getBoardByID(tmpBoard.id);
                }
                catch (Exception e) { return false; }
                if (b_old.isCoreBoard()) {
                    Toast.makeText(activity, "Impossible de charger une planche qui possède le même identifiant qu'une planche fixe.", Toast.LENGTH_SHORT).show();
                    return false;
                }
                else {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    removeBoardById(tmpBoard.id);
                                    boards.add(tmpBoard);
                                    Toast.makeText(activity, "Planche " + tmpBoard.name + " chargée.", Toast.LENGTH_SHORT).show();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    BoardManagerFragment.clearRecentImport();
                                    //No button clicked
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage("Voulez-vous remplacer la planche \"" + b_old.name + "\" par la planche \"" + tmpBoard.name + "\"?").setPositiveButton("Remplacer", dialogClickListener)
                            .setNegativeButton("Annuler", dialogClickListener).show();
                    return true;
                }
            }
            else {
                boards.add(tmpBoard);
                Toast.makeText(activity, "Planche " + tmpBoard.name + " chargée.", Toast.LENGTH_SHORT).show();
                return true;
            }
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
                    if (id == selected) {
                        setSelectedDefault();
                    }
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
