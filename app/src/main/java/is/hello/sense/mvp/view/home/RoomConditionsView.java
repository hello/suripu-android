package is.hello.sense.mvp.view.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.mvp.view.home.roomconditions.SensorResponseAdapter;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;

@SuppressLint("ViewConstructor")
public final class RoomConditionsView extends PresenterView {
    final RecyclerView recyclerView;

    public RoomConditionsView(@NonNull final Activity activity,
                              @NonNull final SensorResponseAdapter adapter) {
        super(activity);
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.fragment_room_conditions_refresh_container);
        swipeRefreshLayout.setEnabled(false);
        recyclerView = (RecyclerView) findViewById(R.id.fragment_room_conditions_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = context.getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));

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
}
