package is.hello.sense.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;

public final class Player implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener {
    public static final int STATE_RECYCLED = -1;
    public static final int STATE_EMPTY = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_LOADED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    private int timesToLoop = 0;
    private int timesLooped = 0;

    @IntDef({
            STATE_RECYCLED,
            STATE_EMPTY,
            STATE_LOADING,
            STATE_LOADED,
            STATE_PLAYING,
            STATE_PAUSED,
    })
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
    @interface State {
    }

    public static String stateToString(@State int state) {
        switch (state) {
            case STATE_RECYCLED:
                return "STATE_RECYCLED";
            case STATE_EMPTY:
                return "STATE_EMPTY";
            case STATE_LOADING:
                return "STATE_LOADING";
            case STATE_LOADED:
                return "STATE_LOADED";
            case STATE_PLAYING:
                return "STATE_PLAYING";
            case STATE_PAUSED:
                return "STATE_PAUSED";
            default:
                return Integer.toString(state);
        }
    }

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

    private int targetVideoScalingMode = MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT;
    private boolean startWhenPrepared = false;
    private
    @State
    int state = STATE_EMPTY;


    //region Lifecycle

    public Player(@NonNull Context context,
                  @NonNull OnEventListener onEventListener,
                  @Nullable OnPulseListener onPulseListener) {
        this.context = context;
        this.onEventListener = onEventListener;

        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);

        if (onPulseListener != null) {
            this.timerHandler = new Handler(Looper.getMainLooper());
            this.timerPulse = new Runnable() {
                @Override
                public void run() {
                    if (timerRunning) {
                        onPulseListener.onPlaybackPulse(Player.this, mediaPlayer.getCurrentPosition());
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
        synchronized (mediaPlayer) {
            if (state != STATE_RECYCLED) {
                stop();
                mediaPlayer.reset();
                mediaPlayer.release();

                setState(STATE_RECYCLED);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (state != STATE_RECYCLED) {
            Logger.warn(getClass().getSimpleName(), "SoundPlayer was not recycled before finalization.");
        }
        recycle();
    }

    private void setState(@State int state) {
        Logger.debug(getClass().getSimpleName(), "setState(" + stateToString(state) + ")");
        this.state = state;
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
        setState(STATE_LOADED);
        onEventListener.onPlaybackReady(this);
        mediaPlayer.setVideoScalingMode(targetVideoScalingMode);
        if (startWhenPrepared) {
            startPlayback();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (what != CHANGING_TO_REMOTE_STREAM_ERROR) {
            unscheduleTimePulse();
            setState(STATE_EMPTY);
            this.startWhenPrepared = false;
            onEventListener.onPlaybackError(this, new PlaybackError(what, extra));
        }

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (!mp.isLooping()) {
            if (timesLooped >= timesToLoop) {
                stop();
                onEventListener.onPlaybackStopped(this, true);
            }else {
                mp.start();
                timesLooped++;
            }
        }
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
        setState(STATE_PLAYING);
    }

    private void stop() {
        if (state == STATE_PLAYING) {
            this.startWhenPrepared = false;
            unscheduleTimePulse();
            mediaPlayer.stop();
            setState(STATE_LOADED);
        }
    }

    public void setDataSource(@NonNull Uri source, boolean startWhenPrepared) {
        setDataSource(source, startWhenPrepared, 0);
    }

    public void setDataSource(@NonNull Uri source, boolean startWhenPrepared, int timesToLoop) {
        try {
            stop();
            unscheduleTimePulse();
            this.timesLooped = 0;
            this.timesToLoop = timesToLoop;

            // See <https://code.google.com/p/android/issues/detail?id=957>
            try {
                mediaPlayer.setDataSource(context, source);
            } catch (IllegalStateException e) {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(context, source);
            }

            setState(STATE_LOADING);
            this.startWhenPrepared = startWhenPrepared;
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            this.startWhenPrepared = false;
            setState(STATE_EMPTY);
            onEventListener.onPlaybackError(this, e);
        }
    }

    public void startPlayback() {
        if (state == STATE_LOADED) {
            start();
            onEventListener.onPlaybackStarted(this);
        } else if (state == STATE_LOADING) {
            this.startWhenPrepared = true;
        } else if (state == STATE_PAUSED) {
            start();
        }
    }

    public void pausePlayback() {
        if (state >= STATE_LOADING && state < STATE_PAUSED) {
            this.startWhenPrepared = false;
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            setState(STATE_PAUSED);
        }
    }

    public void stopPlayback() {
        stop();
        onEventListener.onPlaybackStopped(this, false);
    }

    //endregion


    //region Properties

    public
    @State
    int getState() {
        return state;
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

    public void setTimesToLoop(int times) {
        this.timesToLoop = times;
    }

    public void setLooping(boolean looping) {
        mediaPlayer.setLooping(looping);
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


    //region Video

    public void setVideoSurfaceAsync(@Nullable Surface surface) {
        // Setting the player surface takes just long enough to be
        // problematic if run on the main thread. The underlying
        // native implementation has a mutex guard, so this is safe.
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            // We have to guard against the MediaPlayer being recycled.
            synchronized (mediaPlayer) {
                if (getState() == STATE_RECYCLED) {
                    Logger.warn(getClass().getSimpleName(),
                                "setVideoSurfaceAsync(...) called after recycle()");
                    return;
                }

                // It's possible for the surface to be destroyed between
                // this task being posted, and it being run. We have to
                // guard against invalid surfaces to prevent crashes.
                if (surface != null && surface.isValid()) {
                    mediaPlayer.setSurface(surface);
                } else {
                    mediaPlayer.setSurface(null);
                }
            }
        });
    }

    public void setVideoScalingMode(int mode) {
        this.targetVideoScalingMode = mode;
        if (state == STATE_LOADED) {
            mediaPlayer.setVideoScalingMode(mode);
        }
    }

    //endregion


    @Override
    public String toString() {
        return "Player{" +
                "startWhenPrepared=" + startWhenPrepared +
                ", state=" + stateToString(state) +
                '}';
    }


    public interface OnEventListener {
        void onPlaybackReady(@NonNull Player player);

        void onPlaybackStarted(@NonNull Player player);

        void onPlaybackStopped(@NonNull Player player, boolean finished);

        void onPlaybackError(@NonNull Player player, @NonNull Throwable error);
    }

    public interface OnPulseListener {
        void onPlaybackPulse(@NonNull Player player, int position);
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
                    return StringRef.from("The sound cannot be played on your enoughBattery");

                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    return StringRef.from("Player timed out");
            }
        }
    }
}
