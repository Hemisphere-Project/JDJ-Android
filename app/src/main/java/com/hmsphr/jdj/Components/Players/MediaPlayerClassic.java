package com.hmsphr.jdj.Components.Players;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.VideoView;

/**
 * Created by mgr on 26/10/15.
 */
public class MediaPlayerClassic implements PlayerCompat {

    private Activity context;
    private String contentUri;
    private int playerPosition;

    private VideoView videoView;
    private MediaPlayer player;


    public MediaPlayerClassic(Activity ctx, VideoView vview) {
        context = ctx;
        videoView = vview;
        playerPosition = 0;

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                player = mp;
                player.setLooping(true);
                Log.d("VideoClassic", "Player loaded.. ");

                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        videoView.start();
                        Log.d("VideoClassic", "Seek complete.. start at " + playerPosition);
                    }
                });
            }

        });
    }

    public void load(String url) {
        contentUri = url;
    }

    public void play() {
        stop();
        videoView.setVideoPath(contentUri);
        videoView.start();
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Log.d("VideoClassic", "Video playing..");
    }

    public void resume(){
        if (player != null) {
            videoView.seekTo(playerPosition);
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Log.d("VideoClassic", "Video resuming..");
        }
    }

    public void pause(){
        if (player != null) {
            playerPosition = videoView.getCurrentPosition();
            videoView.pause();
            Log.d("VideoClassic", "Video paused.. at "+playerPosition);
        }
    }

    public void stop(){
        videoView.stopPlayback();
        playerPosition = 0;
        player = null;
        Log.d("VideoClassic", "Video stopped..");
    }

    public void hide(){
        pause();
    }

}
