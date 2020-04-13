package com.jmfavreau.pictoparle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.XmlResourceParser;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;



/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class PictoParleActivity extends AppCompatActivity {
    private ArrayList<Board> boards;

    private TreeMap<Integer, BoardView> views;

    private FrameLayout mainContainer;

    private TextToSpeech tts;

    /** true DPI cannot be get from the Android API. Thus it is a parameter of the application*/
    /* TODO: add a menu entry to change these values */
    private float xdpi = 149.5F;
    private float ydpi = 149.5F;
    private String lang;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set view container
        setContentView(R.layout.activity_fullscreen);
        fullScreen();
        mainContainer = findViewById(R.id.fullscreen_container);

        // select default language
        lang = "fr";

        ConsoleHandler console = new ConsoleHandler();

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

        // create one layout per board
        createLayouts();

        // select the first board
        try {
            setActiveBoard(getBoardByID(1));

        }
        catch (NoSuchFieldException e) {
            console.publish(new LogRecord(Level.SEVERE, "No board has been loaded."));
        }

        // set volume buttons to control this application volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // screen light is reduced as much as possible
        setScreenVisible(false);
    }

    private Board getBoardByID(int id) throws NoSuchFieldException {
        for(int i = 0; i != boards.size(); i++) {
            if (boards.get(i).id == id) {
                return boards.get(i);
            }
        }
        throw new NoSuchFieldException();
    }

    private void setActiveBoard(Board board) {
        if (views.containsKey(board.id)) {
            mainContainer.removeAllViews();
            mainContainer.addView(views.get(board.id));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void createLayouts() {
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

        ConsoleHandler console = new ConsoleHandler();

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
        setScreenVisible(false);
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

}
