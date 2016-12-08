package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.adapters.SensorResponseAdapter;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public final class RoomConditionsView extends PresenterView {
    final RecyclerView recyclerView;
    final ProgressBar progressBar;

    public RoomConditionsView(@NonNull final Activity activity,
                              @NonNull final SensorResponseAdapter adapter) {
        super(activity);
         progressBar = (ProgressBar) findViewById(R.id.fragment_room_conditions_loading);
        recyclerView = (RecyclerView) findViewById(R.id.fragment_room_conditions_recycler);

        setUpStandardRecyclerViewDecorations(recyclerView, new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
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

    public void setSettingsButtonClickListener(@NonNull final OnClickListener listener) {
        //   Views.setSafeOnClickListener(settingsButton, listener);
    }
}
