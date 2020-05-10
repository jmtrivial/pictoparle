package com.jmfavreau.pictoparle.core;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class Pictogram {
    public String txt;
    public String audioFileName;
    public String imageFileName;

    private String directory;

    public Pictogram(String txt, String audioFileName, String imageFileName, String directory) {
        this.txt = txt;
        this.audioFileName = audioFileName;
        this.imageFileName = imageFileName;
        this.directory = directory;

        // if the image path is not defined, we will use the default image
        if (this.imageFileName.equals("")) {
            this.directory = null;
        }
    }

    public boolean isFromDirectory() {
        return directory != null;
    }

    public Boolean isEmpty() {
        return imageFileName.equals("") || imageFileName.equals("empty");
    }

    public static Pictogram empty() {
        return new Pictogram("", "", "empty", null);
    }

    public boolean checkHasFilesInDirectory(Context context) {
        if (directory == null)
            return true;
        if (!imageFileName.equals("")) {
            File f = new File(getFullImagePathName());
            if (!f.exists()) {
                Log.w("PictoParle", "Unable to find " + imageFileName);
                return false;
            }
        }

        // TODO: add sound check

        return true;
    }

    public String getFullImagePathName() {
        return directory + "/pictograms/" + imageFileName;
    }
}
