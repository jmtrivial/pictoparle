package com.jmfavreau.pictoparle.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.jmfavreau.pictoparle.BoardDetector;
import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;
import com.jmfavreau.pictoparle.core.Board;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class BoardManagerFragment extends Fragment {

private BoardsAdapter boardsAdapter;
private PictoParleActivity activity;
private View view;



public static class BoardsAdapter extends ArrayAdapter<Board> {

    BoardsAdapter(Context context, ArrayList<Board> boards) {
        super(context, 0, boards);
    }

    @NotNull
    @Override
    public View getView(int position, View convertView, @NotNull ViewGroup parent) {
        final Board board = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.item_board_config, parent, false);
        }
        // Lookup view for data population
        TextView bdName = convertView.findViewById(R.id.bdName);
        // Populate the data into the template view using the data object
        assert board != null;
        bdName.setText(board.name);

        Button button = convertView.findViewById(R.id.remove_button);
        Switch sw = convertView.findViewById(R.id.active);
        if (board.coreBoard) {
            button.setVisibility(View.GONE);
        }
        else {
            button.setVisibility(View.VISIBLE);
        }

        sw.setOnCheckedChangeListener(null);
        sw.setChecked(board.isActive());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                board.setActive(isChecked);
            }
        });

        // Return the completed view to render on screen
        return convertView;

    }

}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PictoParleActivity) getActivity();

        view = LayoutInflater.from(getContext()).inflate(R.layout.board_config_list, container, false);


        boardsAdapter = new BoardManagerFragment.BoardsAdapter(getContext(), activity.boardSet.getBoards());
        ListView lv = view.findViewById(R.id.board_config_listview);
        lv.setAdapter(boardsAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        activity.boardDetector.setInactive();
        activity.setScreenVisible(true);
    }



}
