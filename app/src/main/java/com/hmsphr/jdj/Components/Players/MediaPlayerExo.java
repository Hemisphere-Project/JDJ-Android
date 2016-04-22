package com.hmsphr.jdj.Components.Players;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.util.Util;
import com.hmsphr.jdj.Activities.VideoActivity;
import com.hmsphr.jdj.Class.MediaActivity;
import com.hmsphr.jdj.Components.Players.ExoPlayer.EventLogger;
import com.hmsphr.jdj.Components.Players.ExoPlayer.ExPlayer;
import com.hmsphr.jdj.Components.Players.ExoPlayer.ExPlayer.RendererBuilder;
import com.hmsphr.jdj.Components.Players.ExoPlayer.ExtractorRendererBuilder;
import com.hmsphr.jdj.Components.Players.ExoPlayer.HlsRendererBuilder;

import java.util.List;
import java.util.Map;

public class MediaPlayerExo implements PlayerCompat, AudioCapabilitiesReceiver.Listener, ExPlayer.Listener {

    private AspectRatioFrameLayout videoFrame;
    private SurfaceView surfaceView;
    private ImageView audioShutter;
    private FrameLayout loadShutter;

    private ExPlayer player;
    private boolean playerNeedsPrepare;
    private long playerPosition;
    private boolean audioRegistered = false;

    private String mode;

    public static final int STATE_STOP = 0;
    public static final int STATE_LOAD = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_PLAY = 3;
    private int playerState = STATE_STOP;

    private EventLogger eventLogger;
    private boolean exoLog = false;

    private FrameLayout replayShutter;
    private FrameLayout replayOverlay;
    private ImageButton replayBtn;
    private boolean replayEnable = false;
    private int playerErrors = 0;

    private VideoActivity context;
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    private Uri contentUri;
    private int contentType;

    public static final int TYPE_HLS = 2;
    public static final int TYPE_OTHER = 3;
    private static int NOW = -1;

    private long baganAtTime = NOW;
    private long atTimeUse = 0;


    public MediaPlayerExo(VideoActivity ctx, AspectRatioFrameLayout frameV, SurfaceView surfaceV, ImageView aview, FrameLayout shutter) {
        context = ctx;
        videoFrame = frameV;
        surfaceView = surfaceV;
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context, this);
        audioShutter = aview;
        loadShutter = shutter;

        loadShutter.setVisibility(View.VISIBLE);
    }

    // Replay last file
    public void play() {
        if (replayEnable) replayShutter.setVisibility(View.GONE);
        if (playerState == STATE_PLAY) player.seekTo(0);
        else play(contentUri.toString());
    }

    // Play file now
    public void play(String url) {
        play(url, NOW);
    }

    // PLAY
    public void play(String url, long atTime) {
        stop();

        contentUri = Uri.parse(url);
        if (url.endsWith("m3u8")) contentType = TYPE_HLS;
        else contentType = TYPE_OTHER;

        playerErrors = 0;
        baganAtTime = atTime;
        launchPlayer(atTime);
        grabAudio();
    }

    public void resume() {
        if (playerPosition > 0) {
            launchPlayer(NOW);
            grabAudio();
            Log.d("jdj-ExoPlayer", "Player RESUME");
        }
    }

    public void pause() {
        releaseAudio();
        releasePlayer(true);
        Log.d("jdj-ExoPlayer", "Player PAUSED at " + playerPosition);
    }

    public void stop() {
        releaseAudio();
        releasePlayer(false);
        Log.d("jdj-ExoPlayer", "Player STOPPED");
    }

    public void setMode(String m) {
        mode = m;
    }

    public void setReplay(FrameLayout replayV, FrameLayout replayO, ImageButton replayB) {
        replayShutter = replayV;
        replayOverlay = replayO;
        replayBtn = replayB;
        replayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });
        if (replayShutter != null) replayEnable = true;
    }

    // AudioCapabilitiesReceiver.Listener methods
    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    // Internal methods
    private RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
        switch (contentType) {
            case TYPE_HLS:
                return new HlsRendererBuilder(context, userAgent, contentUri.toString(), audioCapabilities);
            case TYPE_OTHER:
                return new ExtractorRendererBuilder(context, userAgent, contentUri);
            default:
                throw new IllegalStateException("Unsupported type: " + contentType);
        }
    }

    private void launchPlayer(long atTime) {
        Log.d("jdj-ExoPlayer", "Player LOADING at "+atTime);
        playerState = STATE_LOAD;

        if (mode != null && mode.equals("audio")) {
            audioShutter.setVisibility(View.VISIBLE);

            // seek to position to keep sync
            if (baganAtTime > 0 && atTime < SystemClock.elapsedRealtime()) {
                atTime = SystemClock.elapsedRealtime()+1500; // already late so we postpone the start
                playerPosition = atTime - baganAtTime;       // start at position shifted from baganAtTime
            }
        }

        if (player == null) {
            player = new ExPlayer(getRendererBuilder());
            player.addListener(this);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            if (exoLog) {
                eventLogger = new EventLogger();
                eventLogger.startSession();
                player.addListener(eventLogger);
                player.setInfoListener(eventLogger);
                player.setInternalErrorListener(eventLogger);
            }
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(false);
        atTimeUse = atTime;

        // Sync with atTime
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (playerState == STATE_STOP) return;

                else if (playerState >= STATE_READY) {
                    Log.d("jdj-ExoPlayer", "Player PLAY - SYNC");
                    loadShutter.setVisibility(View.GONE);
                }
                else Log.d("jdj-ExoPlayer", "Player PLAY - OUT OF SYNC.. (not ready yet)");

                player.setPlayWhenReady(true);
                playerState = STATE_PLAY;
                Log.d("jdj-ExoPlayer", " Timer shot shift: "+(SystemClock.elapsedRealtime()-atTimeUse));

            }
        }, Math.max(0, atTime - SystemClock.elapsedRealtime()));
    }

    private void releasePlayer(boolean savePosition) {
        playerState = STATE_STOP;
        //Log.d("jdj-ExoPlayer", "Player STOP");
        playerPosition = 0;
        if (player != null) {
            if (savePosition) playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
            if (exoLog) {
                eventLogger.endSession();
                eventLogger = null;
            }
        }
        if (!savePosition) {
            loadShutter.setVisibility(View.VISIBLE);
            audioShutter.setVisibility(View.GONE);
            baganAtTime = NOW;
        }
        if (replayEnable) replayShutter.setVisibility(View.GONE);
    }

    public void onPlaybackEnd() {
        Log.d("jdj-ExoPlayer", "Player END");
        context.onVideoEnd();
        playerState = STATE_STOP;
        loadShutter.setVisibility(View.GONE);
        if (replayEnable) replayShutter.setVisibility(View.VISIBLE);
        // LOOP
        // player.seekTo(0);
    }

    public void onPlaybackReady() {
        Log.d("jdj-ExoPlayer", "Player READY");
        if (playerState == STATE_LOAD) playerState = STATE_READY;
        else if (playerState == STATE_PLAY) loadShutter.setVisibility(View.GONE);
    }

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {

        // PLAYER LOADED
        if (playbackState == ExoPlayer.STATE_READY) onPlaybackReady();

        // PLAYER END
        if (playbackState == ExoPlayer.STATE_ENDED) onPlaybackEnd();


/*        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
        switch(playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                text += "buffering";
                break;
            case ExoPlayer.STATE_ENDED:
                text += "ended";
                break;
            case ExoPlayer.STATE_IDLE:
                text += "idle";
                break;
            case ExoPlayer.STATE_PREPARING:
                text += "preparing";
                break;
            case ExoPlayer.STATE_READY:
                text += "ready";
                break;
            default:
                text += "unknown";
                break;
        }
        Log.i("jdj-MoviePlayer", ("Video Player "+text));*/
    }

    public void grabAudio() {
        if (!audioRegistered) {
            audioCapabilitiesReceiver.register();
            audioRegistered = true;
        }
    }

    public void releaseAudio() {
        if (audioRegistered) {
            audioCapabilitiesReceiver.unregister();
            audioRegistered = false;
        }
    }

    @Override
    public void onError(Exception e) {
        Log.d("jdj-ExoPlayer", "Player ERROR "+playerErrors);
        playerErrors++;
        if (playerErrors >= 2) {
            context.onVideoFreeze();
        }
        else {
            playerNeedsPrepare = true;
            grabAudio();
            launchPlayer(NOW);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
        videoFrame.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

}
