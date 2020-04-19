package com.jmfavreau.pictoparle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;


/*
    PictoParle activity

    * TODO: check for permissions, and ask user if the camera is not allowed
    * TODO: board selection should not be in fullscreen mode
    * TODO: if the application is switched in background, turn off the camera
    * TODO: when a finger is on the screen, remove the covered detection.

    see https://developer.android.com/training/permissions/requesting.html
 */

public class PictoParleActivity
        extends AppCompatActivity
        implements BoardDetector.SimpleBoardListener {
    protected ArrayList<Board> boards;

    private BoardsAdapter boardsAdapter;
    private boolean isWaitingBoard;

    public class BoardsAdapter extends ArrayAdapter<Board> {

        public BoardsAdapter(Context context, ArrayList<Board> boards) {
            super(context, 0, boards);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Board board = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.item_board, parent, false);
            }
            // Lookup view for data population
            TextView bdName = (TextView) convertView.findViewById(R.id.bdName);
            // Populate the data into the template view using the data object
            bdName.setText(board.name);
            // Return the completed view to render on screen
            return convertView;

        }

    }

    private TreeMap<Integer, BoardView> views;

    private FrameLayout mainContainer;

    private TextToSpeech tts;

    private BoardDetector boardDetector;

    /** true DPI cannot be get from the Android API. Thus it is a parameter of the application*/
    /* TODO: add a menu entry to change these values */
    private float xdpi = 149.5F;
    private float ydpi = 149.5F;
    private String lang;
    private ConsoleHandler console;

    private View waitingForBoard;
    private View boardList;
    private int selected;
    private boolean isActiveBoard;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: send a message to the user, asking to give permissions
            Log.w("PictoParle", "Permissions not granted");
            // Permission is not granted
        }

        // set view container
        setContentView(R.layout.activity_fullscreen);
        fullScreen();
        mainContainer = findViewById(R.id.fullscreen_container);

        // select default language
        lang = "fr";

        isActiveBoard = false;
        isWaitingBoard = false;


        console = new ConsoleHandler();

        // load boards
        try {
            loadXMLResources();
        }
        catch (IOException e) {
            console.publish(new LogRecord(Level.SEVERE, "IO Error while reading a board."));
        }
        catch (XmlPullParserException e) {
            console.publish(new LogRecord(Level.SEVERE, "XML Error while reading a board."));
        }

        /* the selected board is the first of the list */
        selected = boards.get(0).id;

        boardDetector = new BoardDetector(this);


        // set text-to-speech method with the good language
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    if (lang.equals("fr"))
                        tts.setLanguage(Locale.FRANCE);
                    else
                        tts.setLanguage(Locale.UK);
                }
            }
        });

        // create all the views of this activity
        buildViews();

        // set volume buttons to control this application volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // set a waiting message (no board has been detected)
        setActiveWaitingMessage();
    }

    protected void onDestroy() {
        super.onDestroy();
        boardDetector.clear();
    }

    protected  void onPause() {
        super.onPause();
        boardDetector.setInactive();
    }


    private void buildViews() {
        // create the board views
        createBoardViews();

        LayoutInflater factory = LayoutInflater.from(this);

        // create the waiting view
        waitingForBoard = factory.inflate(R.layout.waiting_for_board, null);

        // create the view for board choosing
        boardList = factory.inflate(R.layout.board_selection_list, null);

        // add all the existing boards in the list
        boardsAdapter = new BoardsAdapter(this, boards);
        ListView lv = boardList.findViewById(R.id.board_listview);
        lv.setAdapter(boardsAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setSelected(boardsAdapter.getItem(position).id);
                setActiveWaitingMessage();
            }
        });

    }

    private Board getBoardByID(int id) throws NoSuchFieldException {
        for(int i = 0; i != boards.size(); i++) {
            if (boards.get(i).id == id) {
                return boards.get(i);
            }
        }
        throw new NoSuchFieldException();
    }

    private void setActiveWaitingMessage() {
        TextView sel = waitingForBoard.findViewById(R.id.selected_board);
        String name = new String();
        try {
            name = getBoardByID(selected).name;
        } catch (NoSuchFieldException e) {
            name = "Aucune planche sélectionnée";
            selected = -1;
        }
        sel.setText("Planche sélectionnée : " + name);
        setScreenVisible(true);
        mainContainer.removeAllViews();
        mainContainer.addView(waitingForBoard);
        boardDetector.setActive();
        isActiveBoard = false;
        isWaitingBoard = true;
    }

    private void setActiveBoardList() {
        setScreenVisible(true);
        mainContainer.removeAllViews();
        mainContainer.addView(boardList);
        isActiveBoard = false;
        isWaitingBoard = false;
    }


    private void setActiveBoard(Board board) {
        if (views.containsKey(board.id)) {
            Log.d("PictoParle", "set active board: " + board.name);
            setScreenVisible(false);
            mainContainer.removeAllViews();
            BoardView bv = views.get(board.id);
            mainContainer.addView(bv);
            bv.notifyActive();
            boardDetector.setActive();
            isActiveBoard = true;
            isWaitingBoard = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void createBoardViews() {
        views = new TreeMap<>();
        for(int i = 0; i != boards.size(); i++) {
            views.put(this.boards.get(i).id, new BoardView(this.getApplicationContext(),
                    this.boards.get(i), this.tts, this));
        }
    }

    public void setScreenVisible(boolean visible) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (visible) {
            params.screenBrightness = -1;
        }
        else {
            params.screenBrightness = 0;
        }
        getWindow().setAttributes(params);

    }

    private void loadXMLResources() throws IOException, XmlPullParserException {
        boards = new ArrayList<>();

        // load boards
        XmlResourceParser xrp = getResources().getXml(R.xml.boards);
        do {

            if (xrp.getEventType() == XmlResourceParser.START_TAG) {
                if (xrp.getName().equals("boards")) {
                    String lang = xrp.getAttributeValue(null, "lang");
                    if (lang != null) {
                        this.lang = lang;
                    }
                }
                else if (xrp.getName().equals("board")) {
                    int identifier = this.getResources().getIdentifier(
                            xrp.getAttributeValue(null, "name"),
                            "xml", getPackageName());
                    XmlResourceParser parser = this.getResources().getXml(identifier);
                    Board b = new Board(parser, xdpi, ydpi);
                    if (b.isValid()) {
                        boards.add(b);
                    }
                    else {
                        console.publish(new LogRecord(Level.WARNING, "invalid board: " + b.name));
                    }
                }
            }
        } while (xrp.next() != XmlResourceParser.END_DOCUMENT);

    }

    @Override
    protected void onResume() {
        super.onResume();
        fullScreen();
        if (isActiveBoard) {
            setScreenVisible(false);
        }
        if ((isActiveBoard || isWaitingBoard) && boardDetector.isReady()) {
            boardDetector.setActive();
        }
    }

    protected void setSelected(int position) {
        selected = position;
        views.get(position).notifySelected();
        boardDetector.setActive();
    }



    private void fullScreen() {
        View mContentView = findViewById(R.id.fullscreen_container);

        int fullscreenOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        if (Build.VERSION.SDK_INT >= 19)
                fullscreenOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        mContentView.setSystemUiVisibility(fullscreenOptions);

    }

    @Override
    public void onRemovedBoard() {
        tts.speak("Choix d'une planche",  TextToSpeech.QUEUE_FLUSH, null);
        setActiveWaitingMessage();
    }

    @Override
    public void onNewHoverBoard(int boardID) {
        if (views.containsKey(boardID)) {
            setSelected(boardID);
        }
        else {
            tts.speak("Je ne connais pas cette planche (numéro " + boardID + ")",
                    TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    @Override
    public void onBoardDown() {
        if (selected == -1) {
            tts.speak("Je n'ai pas eu le temps de reconnaître la planche.",
                    TextToSpeech.QUEUE_FLUSH, null);
        }
        else {
            // select the selected board
            try {
                setActiveBoard(getBoardByID(selected));
            } catch (NoSuchFieldException e) {
                tts.speak("Je ne connais pas cette planche (numéro " + selected + ")",
                        TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    public void onClickSelectBoard(View view) {
        setActiveBoardList();
    }

    public void onClickCloseBoardList(View view) {
        setActiveWaitingMessage();
    }
}
