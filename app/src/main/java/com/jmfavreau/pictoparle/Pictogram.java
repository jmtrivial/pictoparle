package com.jmfavreau.pictoparle;

class Pictogram {
    String txt;
    String audioFileName;
    String imageFileName;

    public Pictogram(String txt, String audioFileName, String imageFileName) {
        this.txt = txt;
        this.audioFileName = audioFileName;
        if (imageFileName.equals(""))
            this.imageFileName = "empty";
        else
            this.imageFileName = imageFileName;
    }

    public Boolean isEmpty() {
        return imageFileName.equals("") || imageFileName.equals("empty");
    }

    public static Pictogram empty() {
        return new Pictogram("", "", "empty");
    }
}
