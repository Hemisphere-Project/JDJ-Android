package com.hmsphr.jdj.Components.Players;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

/**
 * Created by mgr on 23/09/15.
 */
public interface PlayerCompat {

    public void load(String url);

    public void play();

    public void resume();

    public void pause();

    public void stop();

    public void hide();

    public void setAudioShutter(ImageView audioS);
    public void showAudioShutter();
    public void hideAudioShutter();

    public void setReplayMenu(FrameLayout replayV, FrameLayout replayO, ImageButton replayB);
    public void enableReplay();
    public void disableReplay();
}
