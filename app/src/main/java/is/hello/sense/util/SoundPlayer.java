package is.hello.sense.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.IOException;

public final class SoundPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {
    private static final int CHANGING_TO_REMOTE_STREAM_ERROR = -38;
    private static final int TIMER_PULSE = 1000;

    private final Context context;
    private final OnEventListener onEventListener;

    private final MediaPlayer mediaPlayer;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerPulse;
    private boolean timerRunning = false;

    private boolean isPaused = false;
    private boolean recycled = false;


    //region Lifecycle

    public SoundPlayer(@NonNull Context context, @NonNull OnEventListener onEventListener) {
        this.context = context;
        this.onEventListener = onEventListener;

        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        this.timerPulse = new Runnable() {
            @Override
            public void run() {
                if (timerRunning) {
                    onEventListener.onPlaybackPulse(SoundPlayer.this, mediaPlayer.getCurrentPosition());
                    timerHandler.postDelayed(this, TIMER_PULSE);
                }
            }
        };

        setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void recycle() {
        if (!recycled) {
            stop();
            mediaPlayer.reset();
            mediaPlayer.release();

            this.recycled = true;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (!recycled) {
            Logger.warn(getClass().getSimpleName(), "SoundPlayer was not recycled before finalization.");
        }
        recycle();
    }

    //endregion


    //region Time Pulse

    private void scheduleTimePulse() {
        this.timerRunning = true;
        timerHandler.postDelayed(timerPulse, TIMER_PULSE);
    }

    private void unscheduleTimePulse() {
        this.timerRunning = false;
        timerHandler.removeCallbacks(timerPulse);
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
            reset();
            onEventListener.onPlaybackError(this, new PlaybackError(what, extra));
        }

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stop();
        reset();

        onEventListener.onPlaybackStopped(this, true);
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        timerHandler.removeCallbacks(timerPulse);
        timerPulse.run();
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

    private void reset() {
        unscheduleTimePulse();
        this.isPaused = false;
    }


    public void play(@NonNull Uri source) {
        try {
            stop();
            reset();

            // See <https://code.google.com/p/android/issues/detail?id=957>
            try {
                mediaPlayer.setDataSource(context, source);
            } catch (IllegalStateException e) {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(context, source);
            }
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            onEventListener.onPlaybackError(this, e);
        }
    }

    public void stopPlayback() {
        stop();
        reset();

        onEventListener.onPlaybackStopped(this, false);
    }


    public void togglePaused() {
        if (isPaused) {
            start();
            this.isPaused = false;
        } else {
            unscheduleTimePulse();
            mediaPlayer.pause();
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
