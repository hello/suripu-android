package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.Duration;
import is.hello.sense.api.model.v2.SleepSoundStatus;
import is.hello.sense.api.model.v2.SleepSoundsState;
import is.hello.sense.api.model.v2.Sound;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.SleepSoundsAdapter;
import is.hello.sense.util.NotTested;

@NotTested
@SuppressLint("ViewConstructor")
public class SleepSoundsView extends PresenterView {

    private final ProgressBar progressBar;
    private final RecyclerView recyclerView;
    private final SleepSoundsAdapter adapter;


    public SleepSoundsView(@NonNull final Activity activity,
                           @NonNull final SleepSoundsAdapter adapter) {
        super(activity);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_sleep_sounds_recycler);
        this.progressBar = (ProgressBar) findViewById(R.id.view_sleep_sounds_progressbar);
        this.adapter = adapter;

        //RecyclerView
        setUpStandardRecyclerViewDecorations(recyclerView,
                                             new LinearLayoutManager(activity));
        this.recyclerView.setAdapter(this.adapter);
    }

    @Override
    public Rect contentInset() {
        return new Rect(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.x12));
    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.view_sleep_sounds;
    }

    @Override
    public void releaseViews() {
        this.recyclerView.setAdapter(null);
    }
    //endregion

    //region methods
    public void setProgressBarVisible(final boolean visible) {
        this.progressBar.setVisibility(visible ? VISIBLE : GONE);
    }

    public boolean isShowingPlayer() {
        return this.adapter.isShowingPlayer();
    }

    public void adapterBindStatus(@NonNull final SleepSoundStatus status) {
        this.adapter.bind(status);
    }

    public void adapterBindState(@NonNull final SleepSoundsState state) {
        this.adapter.bindData(state);
    }

    public void adapterSetState(@NonNull final SleepSoundsAdapter.AdapterState state) {
        this.adapter.setState(state, null);
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

    public void notifyAdapter() {
        this.adapter.notifyDataSetChanged();
    }

    public void scrollUp() {
        this.recyclerView.smoothScrollToPosition(0);
    }

    //endregion
}
