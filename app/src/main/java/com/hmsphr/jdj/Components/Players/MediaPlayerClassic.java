package com.hmsphr.jdj.Components.Players;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
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
    private Uri contentUri;
    private int playerPosition;

    private VideoView videoView;
    private MediaPlayer player;
    private ImageView audioShutter;
    private FrameLayout loadShutter;

    private String mode;

    public static final int STATE_STOP = 0;
    public static final int STATE_LOAD = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_PLAY = 3;
    private int playerState = STATE_STOP;

    private FrameLayout replayShutter;
    private FrameLayout replayOverlay;
    private ImageButton replayBtn;
    private boolean replayEnable = false;


    public MediaPlayerClassic(Activity ctx, VideoView vview, ImageView aview, FrameLayout shutter) {
        context = ctx;
        videoView = vview;
        playerPosition = 0;
        audioShutter = aview;
        loadShutter = shutter;

        loadShutter.setVisibility(View.VISIBLE);

        // Video player events
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                player = mp;
                player.setLooping(false);
                if (playerState > STATE_STOP) {
                    playerState = STATE_READY;
                    Log.d("jdj-VideoClassic", "Player READY");
                }

                mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START)
                            loadShutter.setVisibility(View.GONE);
                        return true;
                    }
                });

                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        videoView.start();
                        Log.d("jdj-VideoClassic", "Player SEEK at" + playerPosition);
                    }
                });

                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        playerState = STATE_STOP;
                        if (replayEnable) replayShutter.setVisibility(View.VISIBLE);
                        Log.d("jdj-VideoClassic", "Player END");
                    }
                });
            }

        });
    }

    public void play() {
        if (replayEnable) replayShutter.setVisibility(View.GONE);
        if (playerState == STATE_PLAY) player.seekTo(0);
        else play(contentUri.toString());
    }

    public void play(String url) {
        play(url, 0);
    }

    public void play(String url, long atTime) {
        stop();

        contentUri = Uri.parse(url);

        Log.d("jdj-VideoClassic", "Player LOADING");
        playerState = STATE_LOAD;
        videoView.setVideoURI(contentUri);
        if (mode != null && mode.equals("audio"))
            audioShutter.setVisibility(View.VISIBLE);

        // Sync with atTime
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (playerState == STATE_STOP) return;

                else if (playerState >= STATE_READY) Log.d("jdj-VideoClassic", "Player PLAY - SYNC");
                else Log.d("jdj-VideoClassic", "Player PLAY - OUT OF SYNC.. (not ready yet)");
                videoView.start();
                playerState = STATE_PLAY;

            }
        }, Math.max(0, atTime - SystemClock.elapsedRealtime()));
    }

    public void resume(){
        if (player != null) {
            videoView.seekTo(playerPosition);
            context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Log.d("jdj-VideoClassic", "Player RESUME");
        }
    }

    public void pause(){
        if (player != null) {
            playerPosition = videoView.getCurrentPosition();
            videoView.pause();
            Log.d("jdj-VideoClassic", "Player PAUSED at "+playerPosition);
        }
    }

    public void stop(){
        playerState = STATE_STOP;
        videoView.stopPlayback();
        loadShutter.setVisibility(View.VISIBLE);
        audioShutter.setVisibility(View.GONE);
        if (replayEnable) replayShutter.setVisibility(View.GONE);
        playerPosition = 0;
        player = null;
        Log.d("jdj-VideoClassic", "Player STOP");
    }

    public void hide(){
        pause();
    }

    public void setMode(String m) {
        mode = m;
    }

    public void setReplay(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {
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

        if (replayShutter != null) replayEnable = true;
    }
}
