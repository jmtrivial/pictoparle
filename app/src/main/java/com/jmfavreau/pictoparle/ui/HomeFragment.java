package com.jmfavreau.pictoparle.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.jmfavreau.pictoparle.interactions.BoardDetector;
import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;
import com.jmfavreau.pictoparle.core.Board;

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

        ImageButton button = home.findViewById(R.id.select_in_list);

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
        activity.findViewById(R.id.add_button).setVisibility(View.GONE);
        activity.findViewById(R.id.edit_button).setVisibility(View.GONE);
    }

    public void setTitle() {
        Board board = activity.boardSet.getSelectedBoard();
        if (board != null) {
            activity.getSupportActionBar().setTitle("PictoParle - planche " + board.name);
        }
        else {
            activity.getSupportActionBar().setTitle("PictoParle - pas de planche");
        }
    }




}
