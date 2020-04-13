package com.jmfavreau.pictoparle;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.speech.tts.TextToSpeech;
import android.view.View;

import org.jetbrains.annotations.NotNull;

class PictoButton extends View {
    private Pictogram pictogram;

    private TextToSpeech tts;

    // default constructor, will not be used
    public PictoButton(Context context) {
        super(context);
    }

    public PictoButton(Context context, @NotNull TextToSpeech tts, @NotNull Pictogram pictogram, @NotNull Board board) {
        super(context);

        this.pictogram = pictogram;
        this.tts = tts;

        // set image
        if (!pictogram.imageFileName.equals("")) {
            int id = getResources().getIdentifier(pictogram.imageFileName, "drawable", getContext().getPackageName());
            Drawable image = getResources().getDrawable(id);
            setBackground(image);
        }
        setLayoutParams(new ActionBar.LayoutParams(board.cellWidthPX, board.cellHeightPX));

    }

    public void playSound() {
        if (pictogram != null && !pictogram.isEmpty()) {
            // play sound using text-to-speech
            tts.speak(pictogram.txt, TextToSpeech.QUEUE_FLUSH, null);

            // TODO: if an audio file is available, play it rather than using text-to-speech
        }
    }

    public void getRectOnScreen(Rect region) {
        int[] l = new int[2];
        getLocationOnScreen(l);
        int x = l[0];
        int y = l[1];
        int w = getWidth();
        int h = getHeight();
        region.set(x, y, x + w, y + h);
    }
}
