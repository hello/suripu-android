package is.hello.sense.ui.fragments;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.ui.activities.appcompat.SenseActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class VideoPlayerActivity extends SenseActivity {
    private static final String EXTRA_URI = VideoPlayerActivity.class.getName() + ".EXTRA_URI";
    private static final String ARG_POSITION = "currentPosition";

    private VideoView videoView;
    private int tempPosition = 0;

    public static Bundle getArguments(@NonNull final Uri uri) {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_URI, uri);
        return bundle;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_video);

        final MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        this.videoView = (VideoView) findViewById(R.id.fragment_video_view);
        videoView.setMediaController(mediaController);

        final Uri uri = getIntent().getParcelableExtra(EXTRA_URI);
        videoView.setVideoURI(uri);

        final ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.fragment_video_loading);

        videoView.setOnPreparedListener((player) -> animatorFor(loadingIndicator).fadeOut(View.GONE).start());

        videoView.setOnErrorListener((player, what, extra) -> {
            if(isFinishing() || isDestroyed()){
                return true;
            }

            final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder();
            final StringRef errorMessage;
            if(extra == -1005 || extra == MediaPlayer.MEDIA_ERROR_IO){
                errorMessage = StringRef.from(R.string.error_internet_connection_generic_message);
                errorDialogBuilder.withTitle(R.string.video_playback_connectivity_error_title);
            } else {
                errorMessage = StringRef.from(R.string.video_playback_generic_error);
            }
            errorDialogBuilder.withMessage(errorMessage);
            errorDialogBuilder.withContextInfo(what + " " + extra);

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            return true;
        });

        videoView.setOnCompletionListener((player) -> finish());
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (videoView != null) {
            outState.putInt(ARG_POSITION, videoView.getCurrentPosition());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tempPosition = savedInstanceState.getInt(ARG_POSITION, 0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tempPosition = videoView.getCurrentPosition();
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.seekTo(tempPosition);
        videoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.setOnPreparedListener(null);
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null;
        }
    }
}
