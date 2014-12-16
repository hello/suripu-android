package is.hello.sense.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.SenseActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class VideoPlayerActivity extends SenseActivity {
    private static final String EXTRA_URI = VideoPlayerActivity.class.getName() + ".EXTRA_URI";

    private VideoView videoView;
    private MediaController mediaController;

    public static Bundle getArguments(@NonNull Uri uri) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_URI, uri);
        return bundle;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_video);


        this.mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        this.videoView = (VideoView) findViewById(R.id.fragment_video_view);
        videoView.setMediaController(mediaController);

        Uri uri = getIntent().getParcelableExtra(EXTRA_URI);
        videoView.setVideoURI(uri);


        ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.fragment_video_loading);
        videoView.setOnPreparedListener((player) -> animate(loadingIndicator).fadeOut(View.GONE).start());

        videoView.setOnErrorListener((player, what, extra) -> {
            ErrorDialogFragment fragment = ErrorDialogFragment.newInstance(getString(R.string.video_playback_generic_error));
            fragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            return true;
        });
        videoView.setOnCompletionListener((player) -> finish());


        if (savedInstanceState != null) {
            videoView.seekTo(savedInstanceState.getInt("currentPosition", 0));
        }

        videoView.start();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("currentPosition", videoView.getCurrentPosition());
    }

    @Override
    protected void onPause() {
        super.onPause();

        videoView.pause();
    }
}
