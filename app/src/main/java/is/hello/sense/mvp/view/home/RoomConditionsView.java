package is.hello.sense.mvp.view.home;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

import is.hello.go99.animators.AnimatorContext;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.sensors.Sensor;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.mvp.view.home.roomconditions.SensorResponseAdapter;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.units.UnitFormatter;

public final class RoomConditionsView extends PresenterView {
    @VisibleForTesting
    final SensorResponseAdapter adapter;
    @VisibleForTesting
    UnitFormatter unitFormatter;

    public RoomConditionsView(@NonNull final Activity activity,
                              @NonNull final UnitFormatter unitFormatter,
                              @NonNull final AnimatorContext animatorContext) {
        super(activity);
        this.unitFormatter = unitFormatter;
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.fragment_room_conditions_refresh_container);
        swipeRefreshLayout.setEnabled(false);
        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.fragment_room_conditions_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = context.getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new CardItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        this.adapter = new SensorResponseAdapter(activity.getLayoutInflater(), unitFormatter, animatorContext);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_room_conditions;
    }

    @Override
    public final void releaseViews() {
        if (adapter != null) {
            this.adapter.setOnItemClickedListener(null);
        }
        this.unitFormatter = null;
    }

    public final void setOnAdapterItemClickListener(@NonNull final ArrayRecyclerAdapter.OnItemClickedListener<Sensor> listener) {
        adapter.setOnItemClickedListener(listener);
    }

    public final void notifyDataSetChanged() {
        adapter.notifyDataSetChanged();
    }

    public final void replaceAllSensors(@NonNull final List<Sensor> sensors) {
        adapter.dismissMessage();
        adapter.replaceAll(sensors);
    }

    public final void displayMessage(final boolean messageWantsSenseIcon,
                                     @StringRes final int title,
                                     @NonNull final CharSequence message,
                                     @StringRes final int actionTitle,
                                     @NonNull final View.OnClickListener actionOnClick) {
        adapter.clear();
        adapter.displayMessage(messageWantsSenseIcon, title, message, actionTitle, actionOnClick);
        adapter.notifyDataSetChanged();

    }

}
