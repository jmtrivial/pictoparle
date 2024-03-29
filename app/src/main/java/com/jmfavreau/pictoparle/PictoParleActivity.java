package com.jmfavreau.pictoparle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.jmfavreau.pictoparle.core.BoardSet;
import com.jmfavreau.pictoparle.interactions.AudioRenderer;
import com.jmfavreau.pictoparle.interactions.BoardDetector;
import com.jmfavreau.pictoparle.interactions.RobustGestureDetector;
import com.jmfavreau.pictoparle.ui.BoardFragment;
import com.jmfavreau.pictoparle.ui.BoardManagerFragment;

import java.io.IOException;


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
        implements BoardDetector.SimpleBoardListener, NavigationView.OnNavigationItemSelectedListener {

    public BoardSet boardSet;

    String lang;


    public BoardDetector boardDetector;
    private BoardDetector.SimpleBoardListener currentFragment;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    public boolean manualBoardOnScreen;
    private boolean active_dark_screen;
    private boolean active_board_detection;
    private float screenWidthMM;
    private float screenHeightMM;
    private long interval_covered;
    private long interval_uncovered;
    public RobustGestureDetector.RobustGestureDetectorParams params;
    public AudioRenderer audioRenderer;
    private int audio_verbosity;
    private boolean waitForNew;
    private boolean readExternal;
    public BoardManagerFragment boardManager;
    public BoardFragment boardFragment;
    private float xdpmm;
    private float ydpmm;

    // a function to show explanation when asking permission
    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    // a function to request permissions
    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    private final int REQUEST_PERMISSION_CAMERA = 1;
    private final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 2;

    // show a small message depending on the permission result
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission accordée.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission refusée.", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readExternal = true;
                    if (boardManager != null)
                        boardManager.addBoard();
                } else {
                    Toast.makeText(this, "Permission refusée.", Toast.LENGTH_SHORT).show();
                    readExternal = false;
                }
                fullScreen();
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // first of all, check permissions for camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                showExplanation("Permission requise", "Rationale",
                        Manifest.permission.CAMERA, REQUEST_PERMISSION_CAMERA);
            } else {
                requestPermission(Manifest.permission.CAMERA, REQUEST_PERMISSION_CAMERA);
            }
        }

        // set view container
        setContentView(R.layout.activity_fullscreen);

        // the current fragment is not defined
        currentFragment = null;

        // by default, the board is not activated manually
        manualBoardOnScreen = false;

        // load preferences
        loadPreferences();

        // create a board detector
        boardDetector = new BoardDetector(this,
                this,
                active_board_detection,
                interval_covered, interval_uncovered);

        // load boards using the preferences (pictogram size)
        loadBoards();

        // the default language is the language defined in the boards
        lang = boardSet.getLang();

        audioRenderer = new AudioRenderer(getApplicationContext(), lang);
        audioRenderer.setAudioVerbosity(audio_verbosity);

        drawerLayout = findViewById(R.id.fullscreen_container);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);


        // add content using the NavController process, associated to the toolbar
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).setDrawerLayout(drawerLayout).build();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationView navView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navView, navController);
        navView.setNavigationItemSelectedListener(this);


        // set volume buttons to control this application volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (!isScreenSizeDefined())
            navController.navigate(R.id.view_preferences);

    }

    public boolean isScreenSizeDefined() {
        return screenWidthMM > 0 && screenHeightMM > 0;
    }

    private void loadPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        active_dark_screen = preferences.getBoolean("darkscreen", true);

        active_board_detection = preferences.getBoolean("board_detection", true);



        interval_covered = Integer.parseInt(preferences.getString("interval_covered", "100"));
        interval_uncovered = Integer.parseInt(preferences.getString("interval_uncovered", "400"));

        params = new RobustGestureDetector.RobustGestureDetectorParams(
                Integer.parseInt(preferences.getString("double_tap_timeout", "600")),
                Integer.parseInt(preferences.getString("tap_timeout", "600")),
                Float.parseFloat(preferences.getString("double_tap_max_distance", "8.0")),
                Float.parseFloat(preferences.getString("tap_max_distance", "1.0")),
                Float.parseFloat(preferences.getString("threshold_y_other_touch", "20.0")));


        updateDPMM();
        params.setDPMM((xdpmm + ydpmm) / 2);


        audio_verbosity = Integer.parseInt(preferences.getString("audio_verbosity", "1"));

        String w = preferences.getString("screen_width_mm", null);
        String h = preferences.getString("screen_height_mm", null);

        if (w != null && !w.equals("null") && h != null && !h.equals("null")) {
            screenWidthMM = Float.parseFloat(w);
            screenHeightMM = Float.parseFloat(h);
        }
        else {
            // the size is not defined
            screenWidthMM = -1f;
            screenHeightMM = -1f;
        }
    }

    private void updateDPMM() {
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getRealSize(size);
        xdpmm = (float) size.x / screenWidthMM;
        ydpmm = (float) size.y / screenHeightMM;
    }

    public boolean getPreferenceIsActiveBoard(int id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean("isactive " + id, true);
    }
    public void setPreferenceIsActiveBoard(int id, boolean value) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isactive " + id, value);
        editor.commit();
        if ((id == boardSet.getSelected() && !value) || (value && !boardSet.getHasSelected())) {
            boardSet.setSelectedDefault();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.display_preferences) {
            navController.navigate(R.id.view_preferences);
        }
        else if (id == R.id.run_board_manual) {
            forceManualBoardDown();
        }
        else if (id == R.id.application_exit) {
            finish();
        }
        else if (id == R.id.manage_boards) {
            navController.navigate(R.id.view_manage_boards);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void forceManualBoardDown() {
        if (boardSet.getHasSelected()) {
            manualBoardOnScreen = true;
            onBoardDown();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    protected void onDestroy() {
        super.onDestroy();
        boardDetector.clear();
    }



    public void setScreenVisible(boolean visible) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            if ((!active_dark_screen) || visible) {
                params.screenBrightness = -1;
            } else {
                params.screenBrightness = 0;
            }
            getWindow().setAttributes(params);

    }


    @Override
    protected void onResume() {
        super.onResume();
        fullScreen();
        waitForNew = true;
        if (!boardDetector.isWaiting())
            boardDetector.start();
        audioRenderer.speak("Pictoparle est prêt", "Prêt", "");
        audioRenderer.setSilence(false);
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
        // go back to the last activity
        if (currentFragment != null)
            currentFragment.onRemovedBoard();
        else
            Log.d("PictoParle", "unable to detect the active fragment");
        waitForNew = true;
    }

    @Override
    public void onNewHoverBoard(int boardID) {
        if (boardSet.containsBoard(boardID)) {
            if (boardID != boardSet.getSelectedBoard().id || waitForNew) {
                waitForNew = false;
                // change view to board fragment
                boardSet.setSelected(boardID);
                // propagate update
                if (currentFragment != null)
                    currentFragment.onNewHoverBoard(boardID);
                // inform user
                String boardName = boardSet.getSelectedBoard().name;
                audioRenderer.speak("Pictoparle a détecté la planche \"" + boardName,
                        "Planche " + boardName + " détectée", boardSet.getSelectedBoard().name);
            }
            else
                Log.d("PictoParle", "detect already selected board: " + boardID);
        }
        else {
            audioRenderer.speak("Pictoparle ne connait pas cette planche (numéro " + boardID + ")", "Planche inconnue.");
        }
    }


    @Override
    public void onBoardDown() {
        if (!boardSet.getHasSelected()) {
            audioRenderer.speak("Pictoparle n'a pas eu le temps de reconnaître la planche.", "Pas de planche.");
        }
        else {
            // go back to the last activity
            if (currentFragment != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
                currentFragment.onBoardDown();
            }
            else
                Log.d("PictoParle", "unable to detect the active fragment");
        }
    }

    public void setCurrentFragment(BoardDetector.SimpleBoardListener fragment) {
        currentFragment = fragment;
    }


    @Override
    protected  void onPause() {
        boardDetector.setInactive();
        audioRenderer.speak("Pictoparle se met en pause", "Pause.","");
        super.onPause();
    }


    public void back() {
        Navigation.findNavController(findViewById(R.id.nav_host_fragment)).popBackStack();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            fullScreen();
    }

    public void setActiveDarkscreen(boolean newValue) {
        active_dark_screen = newValue;
    }

    public void setActiveBoardDetection(boolean activeBoardDetection) {
        active_board_detection = activeBoardDetection;
        boardDetector.forceInactive(!active_board_detection);
    }

    public void setScreenWidthMM(float screenWidthMM) {
        Log.d("PREFERENCES", "set screen width");
        this.screenWidthMM = screenWidthMM;
        updateScreenSize();
    }

    public void setScreenHeightMM(float screenHeightMM) {
        this.screenHeightMM = screenHeightMM;
        updateScreenSize();
    }

    public void setIntervalCovered(long intervalCovered) {
        this.interval_covered = intervalCovered;
        boardDetector.setIntervalCovered(intervalCovered);
    }
    public void setIntervalUncovered(long intervalUncovered) {
        this.interval_uncovered = intervalUncovered;
        boardDetector.setIntervalUncovered(intervalUncovered);
    }

    public void setDoubleTapTimeout(int double_tap_timeout) {
        this.params.doubleTapTimeout = double_tap_timeout;
    }
    public void setTapTimeout(int tap_timeout) {
        this.params.tapTimeout = tap_timeout;
    }

    public void setMaxTapDistance(Float value) {
        params.maxDistTapMM = value;
    }

    public void setMaxDoubleTapDistance(Float value) {
        params.maxDistDoubleTapMM = value;
    }

    public void setAudioVerbosity(int av) {
        audio_verbosity = av;
        audioRenderer.setAudioVerbosity(av);
    }

    public boolean hasReadFilePermissions() {

        // first of all, check permissions for camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            readExternal = false;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {
                showExplanation("Permission requise", "Rationale",
                        Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
            }
            return false;
        }
        else {
            readExternal = true;
            return true;
        }

    }

    private void updateScreenSize() {
        updateDPMM();
        boardSet.updateSizes(xdpmm, ydpmm);
        params.setDPMM((xdpmm + ydpmm) / 2);
        if (boardFragment != null)
            boardFragment.createViews();
    }

    public void loadBoards() {
        updateDPMM();
        // create boardSet from resources
        boardSet = new BoardSet(this, getResources(), getPackageName(), xdpmm, ydpmm);
    }

    public void boardFileSelector() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent,"Selectionner une planche"), 1);
        setResult(Activity.RESULT_OK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            Uri u = data.getData();
            try {
                boardManager.importBoard(u);
            }
            catch (IOException e) {
                Toast.makeText(this, "Le format du fichier n'est pas valide.", Toast.LENGTH_SHORT).show();
                boardManager.clearRecentImport();
            }
        }

    }

}
