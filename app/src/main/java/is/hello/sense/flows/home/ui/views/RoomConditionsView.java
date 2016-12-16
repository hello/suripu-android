package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.adapters.SensorResponseAdapter;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.SenseBar;

@SuppressLint("ViewConstructor")
public final class RoomConditionsView extends PresenterView {
    private final RecyclerView recyclerView;
    private final ProgressBar progressBar;
    private final SenseBar senseBar;

    public RoomConditionsView(@NonNull final Activity activity,
                              @NonNull final SensorResponseAdapter adapter) {
        super(activity);
        progressBar = (ProgressBar) findViewById(R.id.fragment_room_conditions_loading);
        recyclerView = (RecyclerView) findViewById(R.id.fragment_room_conditions_recycler);
        this.senseBar = (SenseBar) findViewById(R.id.fragment_room_conditions_sense_bar);
        setUpStandardRecyclerViewDecorations(recyclerView, new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        senseBar.setText(R.string.title_room_conditions);
        senseBar.showLeftImage(false);
        senseBar.setRightImage(R.drawable.icon_settings_24);
        senseBar.alignTextLeft();
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_room_conditions;
    }

    @Override
    public final void releaseViews() {
        recyclerView.setAdapter(null);
    }

    public void showProgress(final boolean show) {
        progressBar.setVisibility(show ? VISIBLE : INVISIBLE);
    }

    public void setSettingsButtonClickListener(@Nullable final OnClickListener listener) {
        senseBar.setRightImageOnClickListener(listener);
    }

    public void scrollUp() {
        recyclerView.smoothScrollToPosition(0);
    }
}
