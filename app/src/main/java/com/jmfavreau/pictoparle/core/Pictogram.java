package com.jmfavreau.pictoparle.core;

import android.util.Log;

import java.io.File;

public class Pictogram {
    public String txt;
    public String audioFileName;
    public String imageFileName;

    private String directoryImage;
    private String directoryAudio;

    public Pictogram(String txt, String audioFileName, String imageFileName, String directory) {
        this.txt = txt;
        this.audioFileName = audioFileName;
        if (audioFileName != null && audioFileName.equals(""))
            this.audioFileName = null;
        this.imageFileName = imageFileName;
        if (imageFileName != "" && imageFileName.equals(""))
            this.imageFileName = null;
        this.directoryAudio = directory;
        this.directoryImage = directory;

        // if the image path is not defined, we will use the default image
        if (this.imageFileName == null || this.imageFileName.equals("")) {
            this.directoryImage = null;
        }
        // same with sounds
        if (this.audioFileName == null || this.audioFileName.equals("")) {
            this.directoryAudio = null;
        }
    }

    public boolean isImageFromDirectory() {
        return directoryImage != null;
    }

    public static Pictogram empty() {
        return new Pictogram("", "", "empty", null);
    }

    public boolean checkHasFilesInDirectory() {

        if (directoryImage != null && imageFileName != null && !imageFileName.equals("")) {
            File f = new File(getFullImagePathName());
            if (!f.exists()) {
                Log.w("PictoParle", "Unable to find " + imageFileName);
                return false;
            }
        }

        if (directoryAudio != null && audioFileName != null && !audioFileName.equals("")) {
            File f = new File(getFullAudioPathName());
            if (!f.exists()) {
                Log.w("PictoParle", "Unable to find " + audioFileName);
                return false;
            }
        }
        return true;
    }

    public String getFullImagePathName() {
        return directoryImage + "/pictograms/" + imageFileName;
    }

    public String getFullAudioPathName() {
        return directoryAudio + "/audio/" + audioFileName;
    }
}
