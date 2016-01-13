package com.hmsphr.jdj.Components.Players;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    private ImageView audioShutter;

    private FrameLayout replayShutter;
    private FrameLayout replayOverlay;
    private ImageButton replayBtn;
    private boolean replayEnable = false;


    public MediaPlayerClassic(Activity ctx, VideoView vview) {
        context = ctx;
        videoView = vview;
        playerPosition = 0;

        // Video player events
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                player = mp;
                player.setLooping(false);
                Log.d("VideoClassic", "Player loaded.. ");

                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        videoView.start();
                        Log.d("VideoClassic", "Seek complete.. start at " + playerPosition);
                    }
                });

                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        if (replayEnable) replayShutter.setVisibility(View.VISIBLE);
                    }
                });
            }

        });
    }

    public void setAudioShutter(ImageView audioS) {
        audioShutter = audioS;
    }

    public void showAudioShutter() {
        if (audioShutter != null) audioShutter.setVisibility(View.VISIBLE);
    }

    public void hideAudioShutter() {
        if (audioShutter != null) audioShutter.setVisibility(View.GONE);
    }

    public void setReplayMenu(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {
        replayShutter = replayV;
        replayOverlay = replayO;
        replayBtn = replayB;

        // Replay Overlay Alpha
        AlphaAnimation animation = new AlphaAnimation(0.5f, 0.5f);
        animation.setDuration(0);
        animation.setFillAfter(true);
        replayOverlay.startAnimation(animation);

        // Replay Shutter action
        replayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        replayEnable = true;
    }

    public void enableReplay() {
        if (replayShutter != null) replayEnable = true;
    }

    public void disableReplay() {
        if (replayShutter != null) replayShutter.setVisibility(View.GONE);
        replayEnable = false;
    }

    public void load(String url) {
        contentUri = url;
    }

    public void play() {
        stop();
        if (replayEnable) replayShutter.setVisibility(View.GONE);
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
