package is.hello.sense.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public final class SoundPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {
    private static final int CHANGING_TO_REMOTE_STREAM_ERROR = -38;
    private static final int TIMER_PULSE = 1000;

    private final Context context;
    private final OnEventListener onEventListener;

    private final MediaPlayer mediaPlayer;
    private final Timer timePulse;
    private @Nullable TimerTask timePulseTask;

    private boolean isPaused = false;

    public SoundPlayer(@NonNull Context context, @NonNull OnEventListener onEventListener) {
        this.context = context;
        this.onEventListener = onEventListener;

        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        this.timePulse = new Timer();

        setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void recycle() {
        stop();
        mediaPlayer.reset();
        mediaPlayer.release();
    }


    //region Time Pulse

    private void scheduleTimePulse() {
        this.timePulseTask = new TimerTask() {
            @Override
            public void run() {
                onEventListener.onPlaybackPulse(SoundPlayer.this, mediaPlayer.getCurrentPosition());
            }
        };
        timePulse.scheduleAtFixedRate(timePulseTask, 0, TIMER_PULSE);
    }

    private void unscheduleTimePulse() {
        if (timePulseTask != null) {
            timePulseTask.cancel();
            this.timePulseTask = null;

            timePulse.purge();
        }
    }

    //endregion


    //region Callbacks

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (!isPaused) {
            start();
        }

        onEventListener.onPlaybackStarted(this);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what != CHANGING_TO_REMOTE_STREAM_ERROR) {
            mediaPlayer.reset();
            onEventListener.onPlaybackError(this, new PlaybackError(what, extra));
        }

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        onEventListener.onPlaybackStopped(this, true);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (timePulseTask != null) {
            timePulseTask.run();
        }
    }

    //endregion


    //region Controlling Playback

    private void start() {
        scheduleTimePulse();
        mediaPlayer.start();
    }

    private void stop() {
        unscheduleTimePulse();
        mediaPlayer.stop();
    }


    public void play(@NonNull Uri source) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, source);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            onEventListener.onPlaybackError(this, e);
        }
    }

    public void stopPlayback() {
        stop();
        mediaPlayer.reset();

        onEventListener.onPlaybackStopped(this, false);
    }


    public void togglePaused() {
        if (isPaused) {
            start();
            this.isPaused = false;
        } else {
            stop();
            this.isPaused = true;
        }
    }

    //endregion


    //region Properties

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public boolean isPaused() {
        return isPaused;
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public boolean seekTo(int msec) {
        try {
            mediaPlayer.seekTo(msec);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void setAudioStreamType(int streamType) {
        mediaPlayer.setAudioStreamType(streamType);
    }

    //endregion


    public interface OnEventListener {
        void onPlaybackStarted(@NonNull SoundPlayer player);
        void onPlaybackStopped(@NonNull SoundPlayer player, boolean finished);
        void onPlaybackError(@NonNull SoundPlayer player, @NonNull Throwable error);
        void onPlaybackPulse(@NonNull SoundPlayer player, int position);
    }

    public static class PlaybackError extends Exception {
        public final int what;
        public final int extra;

        public static String getStringForCode(int what) {
            switch (what) {
                default:
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    return "An unknown playback error has occurred";

                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    return "Player lost connection with server";

                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    return "Your device does not support progressive playback";

                case MediaPlayer.MEDIA_ERROR_IO:
                    return "Player encountered an IO issue";

                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    return "The sound is malformed";

                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    return "The sound cannot be played on your phone";

                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    return "Player timed out";
            }
        }

        public PlaybackError(int what, int extra) {
            super(getStringForCode(what) + "(" + extra + ")");

            this.what = what;
            this.extra = extra;
        }
    }
}
