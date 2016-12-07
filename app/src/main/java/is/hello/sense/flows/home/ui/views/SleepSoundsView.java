package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.ui.widget.SpinnerImageView;
import is.hello.sense.util.NotTested;

@NotTested
@SuppressLint("ViewConstructor")
public class SleepSoundsView extends PresenterView {

    private final SpinnerImageView playButton;
    private final FrameLayout buttonLayout;
    private final ProgressBar progressBar;
    private final RecyclerView recyclerView;
    private final SleepSoundsAdapter adapter;
    private final OnClickListener onPlayClickListener;
    private final OnClickListener onStopClickListener;


    public SleepSoundsView(@NonNull final Activity activity,
                           @NonNull final SleepSoundsAdapter adapter,
                           @NonNull final OnClickListener onPlayClickListener,
                           @NonNull final OnClickListener onStopClickListener) {
        super(activity);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_sleep_sounds_recycler);
        this.progressBar = (ProgressBar) findViewById(R.id.view_sleep_sounds_progressbar);
        this.playButton = (SpinnerImageView) findViewById(R.id.view_sleep_sounds_playbutton);
        this.buttonLayout = (FrameLayout) findViewById(R.id.view_sleep_sounds_buttonLayout);
        this.adapter = adapter;

        //RecyclerView
        setUpStandardRecyclerViewDecorations(recyclerView,
                                             new LinearLayoutManager(activity));
        this.recyclerView.setAdapter(this.adapter);

        this.onPlayClickListener = onPlayClickListener;
        this.onStopClickListener = onStopClickListener;
    }


    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.view_sleep_sounds;
    }

    @Override
    public void releaseViews() {
        this.recyclerView.setAdapter(null);
        this.playButton.setOnClickListener(null);

    }
    //endregion

    //region methods
    public void setProgressBarVisible(final boolean visible) {
        this.progressBar.setVisibility(visible ? VISIBLE : GONE);
    }

    public boolean isShowingPlayer() {
        return this.adapter.isShowingPlayer();
    }

    public void setButtonVisible(final boolean visible) {
        if (visible) {
            this.buttonLayout.setVisibility(View.VISIBLE);
            this.playButton.setVisibility(View.VISIBLE);
        } else {
            this.buttonLayout.setVisibility(View.GONE);
            this.playButton.setVisibility(View.GONE);
        }
    }

    public void adapterBindStatus(@NonNull final SleepSoundStatus status) {
        this.adapter.bind(status);
    }

    public void adapterBindState(@NonNull final SleepSoundsState state) {
        this.adapter.bindData(state);
    }

    public void adapterSetState(@NonNull final SleepSoundsAdapter.AdapterState state) {
        this.adapter.setState(state, null);
        setButtonVisible(false);
    }

    public Sound getDisplayedSound() {
        return adapter.getDisplayedSound();
    }

    public Duration getDisplayedDuration() {
        return adapter.getDisplayedDuration();
    }

    public SleepSoundStatus.Volume getDisplayedVolume() {
        return adapter.getDisplayedVolume();
    }

    public void displayPlayButton() {
        displayButton(R.drawable.sound_play_icon,
                      onPlayClickListener,
                      true);
    }

    public void displayStopButton() {
        displayButton(R.drawable.sound_stop_icon,
                      onStopClickListener,
                      true);
    }

    public void displayLoadingButton() {
        displayButton(R.drawable.sound_loading_icon,
                      null,
                      false);
    }

    public void displayButton(final @DrawableRes int resource,
                              final @Nullable View.OnClickListener listener,
                              final boolean enabled) {
        buttonLayout.setVisibility(View.VISIBLE);
        playButton.setRotation(0);
        playButton.setImageResource(resource);
        playButton.setOnClickListener(listener);
        playButton.setEnabled(enabled);
        if (enabled) {
            playButton.stopSpinning();
        } else {
            playButton.startSpinning();
        }
    }

    public void notifyAdapter(){
        this.adapter.notifyDataSetChanged();
    }


    //endregion
}
