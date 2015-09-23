package com.hmsphr.jdj.Class;

import android.net.Uri;
import android.view.View;

/**
 * Created by mgr on 23/09/15.
 */
public interface PlayerCompat {

    public void play(String url);

    public void resume();

    public void pause();

    public void stop();

    public void hide();

}
