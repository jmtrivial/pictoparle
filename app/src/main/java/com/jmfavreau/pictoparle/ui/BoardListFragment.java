package com.jmfavreau.pictoparle.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filterable;
import android.widget.ListView;
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

public class BoardListFragment extends Fragment implements BoardDetector.SimpleBoardListener {

    private BoardsAdapter boardsAdapter;
    private PictoParleActivity activity;
    private View view;
    private ArrayList<Board> activeBoards;

    @Override
    public void onRemovedBoard() {
        // ignore event
    }

    @Override
    public void onNewHoverBoard(int boardID) {
        // ignore event
    }

    @Override
    public void onBoardDown() {
        Navigation.findNavController(view).navigate(R.id.view_board_from_board_list);
    }


    public static class BoardsAdapter extends ArrayAdapter<Board> implements Filterable {

        BoardsAdapter(Context context, ArrayList<Board> boards) {
            super(context, 0, boards);
        }

        @NotNull
        @Override
        public View getView(int position, View convertView, @NotNull ViewGroup parent) {
            Board board = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.item_board, parent, false);
            }
            // Lookup view for data population
            TextView bdName = convertView.findViewById(R.id.bdName);
            // Populate the data into the template view using the data object
            assert board != null;
            bdName.setText(board.name);
            // Return the completed view to render on screen
            return convertView;

        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (PictoParleActivity) getActivity();

        view = LayoutInflater.from(getContext()).inflate(R.layout.board_selection_list, container, false);

        activeBoards = new ArrayList<Board>();

        boardsAdapter = new BoardsAdapter(getContext(), activeBoards);
        ListView lv = view.findViewById(R.id.board_listview);
        lv.setAdapter(boardsAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activity.boardSet.setSelected(boardsAdapter.getItem(position).id);
                String boardName = activity.boardSet.getSelectedBoard().name;
                activity.audioRenderer.speak("La planche \"" + boardName + "\" est active",
                        "Planche " + boardName + " active", activity.boardSet.getSelectedBoard().name);
                Navigation.findNavController(view).popBackStack();
            }
        });

        return view;
    }

    private void rebuildBoardList() {
        activeBoards.clear();
        for (int i = 0; i != activity.boardSet.getBoards().size(); ++i) {
            Board cur = activity.boardSet.getBoards().get(i);
            if (cur.isActive())
                activeBoards.add(cur);
        }
        boardsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        activity.audioRenderer.speak("Choix d'une planche", "");
        super.onResume();
        activity.boardDetector.setActive();
        activity.setScreenVisible(true);
        activity.setCurrentFragment(this);
        rebuildBoardList();
        setTitle();
    }

    public void setTitle() {
        activity.getSupportActionBar().setTitle("Liste des planches");
    }


}