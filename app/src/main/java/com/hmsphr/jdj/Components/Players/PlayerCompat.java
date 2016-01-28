package com.hmsphr.jdj.Components.Players;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

/**
 * Created by mgr on 23/09/15.
 */
public interface PlayerCompat {

    public void play();
    public void play(String s);
    public void play(String s, long atTime);

    public void resume();
    public void pause();
    public void stop();
    public void hide();

    public void setMode(String m);
    public void setReplay(FrameLayout replayV, FrameLayout replayO, ImageButton replayB);
}
