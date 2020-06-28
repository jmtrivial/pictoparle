package com.jmfavreau.pictoparle.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jmfavreau.pictoparle.PictoParleActivity;
import com.jmfavreau.pictoparle.R;
import com.jmfavreau.pictoparle.core.Board;
import com.jmfavreau.pictoparle.core.BoardSet;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BoardManagerFragment extends Fragment {

    private BoardsAdapter boardsAdapter;
    private PictoParleActivity activity;
    private View view;
    private String boardFileLoadingLocation;


    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
    public void clearRecentImport() {
        deleteRecursive(new File(boardFileLoadingLocation));
    }


    public static class BoardsAdapter extends ArrayAdapter<Board> {

        private final BoardSet boardSet;

        BoardsAdapter(Context context, BoardSet boardSet) {
        super(context, 0, boardSet.getBoards());
        this.boardSet = boardSet;
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
            bdName.setText(Html.fromHtml("<b>" + board.name + "</b> (id " + board.id + ")"));

            Button button = convertView.findViewById(R.id.remove_button);
            Switch sw = convertView.findViewById(R.id.active);
            if (board.isCoreBoard()) {
                button.setVisibility(View.GONE);
            }
            else {
                button.setVisibility(View.VISIBLE);
            }
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (boardSet.removeBoardById(board.id)) {
                                deleteRecursive(new File(board.directory));
                            }
                            notifyDataSetChanged();
                        }
                    }
            );

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


        boardsAdapter = new BoardManagerFragment.BoardsAdapter(getContext(), activity.boardSet);
        ListView lv = view.findViewById(R.id.board_config_listview);
        lv.setAdapter(boardsAdapter);

        activity.findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBoard();
            }
        });
        activity.boardManager = this;

        return view;
    }

    public void addBoard() {
        activity.audioRenderer.setSilence(true);
        if (activity.hasReadFilePermissions()) {
            activity.boardFileSelector();
        }
    }



    public void importBoard(Uri u) throws IOException {
        // create target directory
        File folder = new File(activity.boardSet.getBoardDirectory());
        boardFileLoadingLocation = folder.getAbsolutePath() + "/" + UUID.randomUUID().toString() + "/";
        File bdir = new File(boardFileLoadingLocation);
        bdir.mkdirs();

        // unzip board
        InputStream fin = null;
        fin = activity.getApplicationContext().getContentResolver().openInputStream(u);

        ZipInputStream zin = new ZipInputStream(fin);
        ZipEntry ze = null;
        while ((ze = zin.getNextEntry()) != null) {

            if(ze.isDirectory()) {
                dirChecker(ze.getName());
            } else {
                FileOutputStream fout = new FileOutputStream(boardFileLoadingLocation + ze.getName());
                BufferedOutputStream bufout = new BufferedOutputStream(fout);
                byte[] buffer = new byte[1024];
                int read = 0;
                while ((read = zin.read(buffer)) != -1) {
                    bufout.write(buffer, 0, read);
                }
                bufout.close();
                zin.closeEntry();
                fout.close();
            }

        }
        zin.close();
        zin.close();

        // load board
        try {
            if (!activity.boardSet.loadFromDir(boardFileLoadingLocation)) {
                clearRecentImport();
            }
        }
        catch (Exception e) {
            clearRecentImport();
        }

        boardFileLoadingLocation = null;
    }

    private void dirChecker(String dir) {
        File f = new File(boardFileLoadingLocation + dir);

        if(!f.isDirectory()) {
            f.mkdirs();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        activity.boardDetector.setInactive();
        activity.setScreenVisible(true);
        setTitle();
        activity.setCurrentFragment(null);
        activity.findViewById(R.id.add_button).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.edit_button).setVisibility(View.GONE);
    }

    public void setTitle() {
        activity.getSupportActionBar().setTitle("Gestion des planches");
    }

}
