package com.jmfavreau.pictoparle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.Locale;


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

    protected BoardSet boardSet;

    String lang;

    protected TextToSpeech tts;

    protected BoardDetector boardDetector;
    private BoardDetector.SimpleBoardListener currentFragment;
    private NavController navController;
    private DrawerLayout drawerLayout;
    private AppBarConfiguration appBarConfiguration;
    protected boolean manual;


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
        // TODO on va utiliser https://developer.android.com/reference/androidx/navigation/fragment/package-summary


        boardSet = new BoardSet(getResources(), getPackageName());
        lang = boardSet.getLang();

        currentFragment = null;

        boardDetector = new BoardDetector(this);

        boardDetector.setActive();
        manual = false;

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

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.run_board_manual) {
            forceManualBoardDown();
        }
        else if (id == R.id.application_exit) {
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void forceManualBoardDown() {
        manual = true;
        onBoardDown();
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
        if (visible) {
            params.screenBrightness = -1;
        }
        else {
            params.screenBrightness = 0;
        }
        getWindow().setAttributes(params);

    }


    @Override
    protected void onResume() {
        super.onResume();
        fullScreen();
        tts.speak("Pictoparle est prêt",  TextToSpeech.QUEUE_FLUSH, null);
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

    }

    @Override
    public void onNewHoverBoard(int boardID) {
        if (boardSet.containsBoard(boardID)) {
            // TODO: change view to board fragment
        }
        else {
            tts.speak("Pictoparle ne connait pas cette planche (numéro " + boardID + ")",
                    TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    @Override
    public void onBoardDown() {
        if (boardSet.getSelected() == -1) {
            tts.speak("Pictoparle n'a pas eu le temps de reconnaître la planche.",
                    TextToSpeech.QUEUE_FLUSH, null);
        }
        else {
            // go back to the last activity
            if (currentFragment != null)
                currentFragment.onBoardDown();
            else
                Log.d("PictoParle", "unable to detect the active fragment");
        }
    }

    public void setCurrentFragment(BoardDetector.SimpleBoardListener fragment) {
        currentFragment = fragment;
    }


    @Override
    protected  void onPause() {
        super.onPause();
        boardDetector.setInactive();
        tts.speak("Pictoparle se met en pause",  TextToSpeech.QUEUE_FLUSH, null);
    }


    public void back() {
        Navigation.findNavController(findViewById(R.id.nav_host_fragment)).popBackStack();
    }
}
