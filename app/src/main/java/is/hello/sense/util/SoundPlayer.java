package is.hello.sense.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class SoundPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener {
    public static final int PLAYBACK_STREAM_TYPE = AudioManager.STREAM_MUSIC;

    private static final int CHANGING_TO_REMOTE_STREAM_ERROR = -38;
    private static final int TIMER_PULSE = 1000;

    private final Context context;
    private final OnEventListener onEventListener;

    private final AudioManager audioManager;
    private final MediaPlayer mediaPlayer;
    private final Handler timerHandler;
    private final Runnable timerPulse;
    private boolean timerRunning = false;

    private boolean loading = false;
    private boolean paused = false;
    private boolean recycled = false;


    //region Lifecycle

    public SoundPlayer(@NonNull Context context, @NonNull OnEventListener onEventListener, boolean wantsPulse) {
        this.context = context;
        this.onEventListener = onEventListener;

        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        if (wantsPulse) {
            this.timerHandler = new Handler(Looper.getMainLooper());
            this.timerPulse = new Runnable() {
                @Override
                public void run() {
                    if (timerRunning) {
                        onEventListener.onPlaybackPulse(SoundPlayer.this, mediaPlayer.getCurrentPosition());
                        timerHandler.postDelayed(this, TIMER_PULSE);
                    }
                }
            };
        } else {
            this.timerHandler = null;
            this.timerPulse = null;
        }

        setAudioStreamType(PLAYBACK_STREAM_TYPE);
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
        if (timerHandler != null) {
            this.timerRunning = true;
            timerHandler.postDelayed(timerPulse, TIMER_PULSE);
        }
    }

    private void unscheduleTimePulse() {
        if (timerHandler != null) {
            this.timerRunning = false;
            timerHandler.removeCallbacks(timerPulse);
        }
    }

    //endregion


    //region Callbacks

    @Override
    public void onPrepared(MediaPlayer mp) {
        this.loading = false;

        if (!paused) {
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
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerPulse);
            timerPulse.run();
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

    private void reset() {
        unscheduleTimePulse();
        this.loading = false;
        this.paused = false;
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

            this.loading = true;
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            onEventListener.onPlaybackError(this, e);
        }
    }

    public void stopPlayback() {
        stop();
        reset();

        onEventListener.onPlaybackStopped(this, false);
    }


    public void togglePaused() {
        if (paused) {
            start();
            this.paused = false;
        } else {
            unscheduleTimePulse();
            mediaPlayer.pause();
            this.paused = true;
        }
    }

    //endregion


    //region Properties

    public boolean isLoading() {
        return loading;
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public boolean isPaused() {
        return paused;
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public boolean seekTo(int position) {
        try {
            mediaPlayer.seekTo(position);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public void setAudioStreamType(int streamType) {
        mediaPlayer.setAudioStreamType(streamType);
    }

    //endregion


    //region Sound

    public void setStreamVolume(int index, int flags) {
        audioManager.setStreamVolume(PLAYBACK_STREAM_TYPE, index, flags);
    }

    public int getMaxStreamVolume() {
        return audioManager.getStreamMaxVolume(PLAYBACK_STREAM_TYPE);
    }

    public int getRecommendedStreamVolume() {
        // AudioManager#isWiredHeadsetOn() does not appear to actually
        // be deprecated, it seems they changed what it means at some
        // point in the past and attached a deprecation warning to it.

        //noinspection deprecation
        if (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn()) {
            return getMaxStreamVolume() / 2;
        } else {
            return getMaxStreamVolume();
        }
    }

    public int getStreamVolume() {
        return audioManager.getStreamVolume(PLAYBACK_STREAM_TYPE);
    }

    public boolean isStreamVolumeAdjustable() {
        //noinspection SimplifiableIfStatement
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return !audioManager.isVolumeFixed();
        } else {
            return true;
        }
    }

    //endregion


    public interface OnEventListener {
        void onPlaybackStarted(@NonNull SoundPlayer player);
        void onPlaybackStopped(@NonNull SoundPlayer player, boolean finished);
        void onPlaybackError(@NonNull SoundPlayer player, @NonNull Throwable error);
        void onPlaybackPulse(@NonNull SoundPlayer player, int position);
    }

    public static class PlaybackError extends Exception implements Errors.Reporting {
        public final int what;
        public final int extra;

        public static String codeToString(int what) {
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    return "MEDIA_ERROR_UNKNOWN";

                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    return "MEDIA_ERROR_SERVER_DIED";

                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    return "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK";

                case MediaPlayer.MEDIA_ERROR_IO:
                    return "MEDIA_ERROR_IO";

                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    return "MEDIA_ERROR_MALFORMED";

                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    return "MEDIA_ERROR_UNSUPPORTED";

                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    return "MEDIA_ERROR_TIMED_OUT";

                default:
                    return "UNKNOWN: " + what;
            }
        }

        public PlaybackError(int what, int extra) {
            super(codeToString(what));

            this.what = what;
            this.extra = extra;
        }

        @Nullable
        @Override
        public String getContextInfo() {
            return codeToString(what) + " (" + Integer.toString(extra) + ")";
        }

        @NonNull
        @Override
        public StringRef getDisplayMessage() {
            switch (what) {
                default:
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    return StringRef.from("An unknown playback error has occurred");

                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    return StringRef.from("Player lost connection with server");

                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    return StringRef.from("Your device does not support progressive playback");

                case MediaPlayer.MEDIA_ERROR_IO:
                    return StringRef.from("Player encountered an IO issue");

                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    return StringRef.from("The sound is malformed");

                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    return StringRef.from("The sound cannot be played on your phone");

                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    return StringRef.from("Player timed out");
            }
        }
    }
}
