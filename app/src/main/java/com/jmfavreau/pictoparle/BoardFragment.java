package com.jmfavreau.pictoparle;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.TreeMap;

public class BoardFragment extends Fragment implements BoardDetector.SimpleBoardListener {

    private TreeMap<Integer, BoardView> views;

    PictoParleActivity activity;

    FrameLayout view;
    private BoardView boardView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PictoParleActivity) getActivity();

        // create all the views of this activity
        views = new TreeMap<>();
        for(int i = 0; i != activity.boardSet.size(); i++) {
            views.put(activity.boardSet.get(i).id, new BoardView(activity,
                    activity.boardSet.get(i), activity,
                    activity.params));
        }

        view = new FrameLayout(getContext());
        view.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        return view;
    }

    @Override
    public void onResume() {
        String boardName = activity.boardSet.getSelectedBoard().name;
        activity.audioRenderer.speak("Planche \"" + boardName, boardName);
        activity.getSupportActionBar().hide();
        super.onResume();
        activity.setScreenVisible(false);
        activity.boardDetector.setActive();
        setActiveBoard();
    }

    @Override
    public void onPause() {
        super.onPause();
        activity.getSupportActionBar().show();
    }



    private void setActiveBoard() {
        int selected = activity.boardSet.getSelected();
        view.removeAllViews();
        boardView = views.get(selected);
        if (activity.manualBoardOnScreen) {
            boardView.setManual(true);
        }
        else {
            boardView.setManual(false);
        }
        view.addView(boardView);
        activity.setCurrentFragment(this);
    }


    @Override
    public void onRemovedBoard() {
        // return to the previous fragment
        Navigation.findNavController(view).popBackStack();
    }

    @Override
    public void onNewHoverBoard(int boardID) {
        // ignore event
    }

    @Override
    public void onBoardDown() {
        // hide close button
        boardView.setManual(false);
        activity.manualBoardOnScreen = false;
    }
}
