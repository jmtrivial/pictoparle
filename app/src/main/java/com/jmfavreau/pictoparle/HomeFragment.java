package com.jmfavreau.pictoparle;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class HomeFragment extends Fragment implements BoardDetector.SimpleBoardListener {

    private PictoParleActivity activity;
    private View home;

    @Override
    public void onRemovedBoard() {
        // ignore event
    }

    @Override
    public void onNewHoverBoard(int boardID) {
        setTitle();
    }

    @Override
    public void onBoardDown() {
        Navigation.findNavController(home).navigate(R.id.view_board_from_home);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PictoParleActivity) getActivity();

        home = LayoutInflater.from(getContext()).inflate(R.layout.waiting_for_board, container, false);

        Button button = home.findViewById(R.id.select_in_list);

        // connect button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Navigation.findNavController(view).navigate(R.id.select_in_list);
            }
        });

        return home;
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.setScreenVisible(true);
        activity.setCurrentFragment(this);
        setTitle();
        activity.boardDetector.setActive();

    }

    public void setTitle() {
        Board board = activity.boardSet.getSelectedBoard();
        if (board != null) {
            // TODO: add a supplementary widget in the toolbar ?
            activity.getSupportActionBar().setTitle("PictoParle - planche " + board.name);
        }
    }


}
