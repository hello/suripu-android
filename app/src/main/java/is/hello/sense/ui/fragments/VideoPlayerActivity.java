package is.hello.sense.ui.fragments;

import android.content.Intent;
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
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class VideoPlayerActivity extends SenseActivity {
    private static final String EXTRA_URI = VideoPlayerActivity.class.getName() + ".EXTRA_URI";

    private VideoView videoView;

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


        if (savedInstanceState != null) {
            videoView.seekTo(savedInstanceState.getInt("currentPosition", 0));
        }

        videoView.start();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("currentPosition", videoView.getCurrentPosition());
    }

    @Override
    protected void onPause() {
        super.onPause();

        videoView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.setOnPreparedListener(null);
        videoView.setOnErrorListener(null);
        videoView.setOnCompletionListener(null);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }
}
