package com.hmsphr.jdj.Components.Players;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
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
import com.google.android.exoplayer.metadata.GeobMetadata;
import com.google.android.exoplayer.metadata.PrivMetadata;
import com.google.android.exoplayer.metadata.TxxxMetadata;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.Util;
import com.hmsphr.jdj.Components.Players.ExoPlayer.EventLogger;
import com.hmsphr.jdj.Components.Players.ExoPlayer.ExPlayer;
import com.hmsphr.jdj.Components.Players.ExoPlayer.ExPlayer.RendererBuilder;
import com.hmsphr.jdj.Components.Players.ExoPlayer.ExtractorRendererBuilder;
import com.hmsphr.jdj.Components.Players.ExoPlayer.HlsRendererBuilder;

import java.util.List;
import java.util.Map;

public class MediaPlayerExo implements PlayerCompat, AudioCapabilitiesReceiver.Listener,
        ExPlayer.Listener, ExPlayer.CaptionListener, ExPlayer.Id3MetadataListener {

    private AspectRatioFrameLayout videoFrame;
    private SurfaceView surfaceView;
    private SubtitleLayout subtitleLayout;

    private ExPlayer player;
    private boolean playerNeedsPrepare;
    private long playerPosition;
    private EventLogger eventLogger;
    private boolean audioRegistered = false;

    private ImageView audioShutter;

    private FrameLayout replayShutter;
    private FrameLayout replayOverlay;
    private ImageButton replayBtn;
    private boolean replayEnable = false;

    private Activity context;
    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    private Uri contentUri;
    private int contentType;

    public static final int TYPE_HLS = 2;
    public static final int TYPE_OTHER = 3;


    public MediaPlayerExo(Activity ctx, AspectRatioFrameLayout frameV, SurfaceView surfaceV, SubtitleLayout subsV) {
        context = ctx;
        videoFrame = frameV;
        surfaceView = surfaceV;
        subtitleLayout = subsV;
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context, this);
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
        contentUri = Uri.parse(url);
        if (url.endsWith("m3u8")) contentType = TYPE_HLS;
        else contentType = TYPE_OTHER;
    }

    public void play()
    {
        stop();
        if (replayEnable) replayShutter.setVisibility(View.GONE);
        resume();
    }

    public void resume()
    {
        audioCapabilitiesReceiver.register();
        audioRegistered = true;
        preparePlayer();
        configureSubtitleView();
        videoFrame.setVisibility(View.VISIBLE);
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void pause()
    {
        releasePlayer();
        if (audioRegistered)
        {
            audioCapabilitiesReceiver.unregister();
            audioRegistered = false;
        }
    }

    public void stop() {
        hide();
        playerPosition = 0;
        Log.v("mgrlog", "Player stopped..");
    }

    public void hide() {
        videoFrame.setVisibility(View.GONE);
        pause();
    }

    // AudioCapabilitiesReceiver.Listener methods
    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
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

    private void preparePlayer() {
        if (player == null) {
            player = new ExPlayer(getRendererBuilder());
            player.addListener(this);
            player.setCaptionListener(this);
            player.setMetadataListener(this);
            player.seekTo(playerPosition);
            Log.v("mgrlog", "seek to position: " + playerPosition);
            playerNeedsPrepare = true;
            // mediaController.setMediaPlayer(player.getPlayerControl());
            // mediaController.setEnabled(true);
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    public void onPlaybackEnd() {
        Log.i("jdj-MoviePlayer", ("Media did reach end "));
        if (replayEnable) replayShutter.setVisibility(View.VISIBLE);
    }

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {

        // LOOP
        //if (playbackState == ExoPlayer.STATE_ENDED) player.seekTo(0);
        if (playbackState == ExoPlayer.STATE_ENDED) onPlaybackEnd();

        String text = "playWhenReady=" + playWhenReady + ", playbackState=";
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
        Log.i("jdj-MoviePlayer", ("Video Player "+text));
    }

    @Override
    public void onError(Exception e) {
        playerNeedsPrepare = true;
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
        videoFrame.setAspectRatio(
                height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
    }

    // DemoPlayer.CaptionListener implementation

    @Override
    public void onCues(List<Cue> cues) {
        subtitleLayout.setCues(cues);
    }

    // DemoPlayer.MetadataListener implementation

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
                Log.i("jdj-MoviePlayer", String.format("ID3 TimedMetadata %s: description=%s, value=%s",
                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
                Log.i("jdj-MoviePlayer", String.format("ID3 TimedMetadata %s: owner=%s",
                        PrivMetadata.TYPE, privMetadata.owner));
            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
                Log.i("jdj-MoviePlayer", String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
                        geobMetadata.description));
            } else {
                Log.i("jdj-MoviePlayer", String.format("ID3 TimedMetadata %s", entry.getKey()));
            }
        }
    }

    private void configureSubtitleView() {
        CaptionStyleCompat captionStyle;
        float captionFontScale;
        if (Util.SDK_INT >= 19) {
            captionStyle = getUserCaptionStyleV19();
            captionFontScale = getUserCaptionFontScaleV19();
        } else {
            captionStyle = CaptionStyleCompat.DEFAULT;
            captionFontScale = 1.0f;
        }
        subtitleLayout.setStyle(captionStyle);
        subtitleLayout.setFontScale(captionFontScale);
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) context.getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

}
