package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import java.util.ArrayList;

import is.hello.sense.R;
import is.hello.sense.api.model.Alarm;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.SmartAlarmAdapter;
import is.hello.sense.ui.recycler.CardItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Views;

@SuppressLint("ViewConstructor")
public class SmartAlarmListView extends PresenterView {
    private final RecyclerView recyclerView;
    private final ProgressBar progressBar;
    private final ImageButton addButton;
    private final SmartAlarmAdapter adapter;

    public SmartAlarmListView(@NonNull final Activity activity,
                              @NonNull final SmartAlarmAdapter adapter,
                              @NonNull final OnClickListener addButtonClickListener) {
        super(activity);
        this.recyclerView = (RecyclerView) findViewById(R.id.fragment_smart_alarm_recycler);
        this.progressBar = (ProgressBar) findViewById(R.id.fragment_smart_alarm_list_activity);
        this.addButton = (ImageButton) findViewById(R.id.fragment_smart_alarm_list_add);
        this.adapter = adapter;

        //RecyclerView
        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        final CardItemDecoration decoration = new CardItemDecoration(resources);
        decoration.contentInset = new Rect(0, 0, 0, resources.getDimensionPixelSize(R.dimen.gap_smart_alarm_list_bottom));
        recyclerView.addItemDecoration(decoration);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);

        // AddButton
        Views.setSafeOnClickListener(addButton, addButtonClickListener);
    }

    //region PresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_smart_alarm_list; //todo replace with view
    }

    @Override
    public void releaseViews() {
        recyclerView.setAdapter(null);
        addButton.setOnClickListener(null);
    }
    //endregion

    //region methods
    public void setProgressBarVisible(final boolean isLoading) {
        if (isLoading) {
            this.adapter.clearMessage();
            this.progressBar.setVisibility(VISIBLE);
        } else {
            this.progressBar.setVisibility(GONE);
        }
    }

    public void updateAdapterTime(final boolean use24Time) {
        this.adapter.setUse24Time(use24Time);
    }

    public void updateAdapterAlarms(@NonNull final ArrayList<Alarm> alarms) {
        adapter.bindAlarms(alarms);
    }

    public void bindAdapterMessage(@NonNull final SmartAlarmAdapter.Message message) {
        adapter.bindMessage(message);
    }

    public void notifyAdapterUpdate() {
        adapter.notifyDataSetChanged();
    }

    public void setAddButtonVisible(final boolean visible) {
        addButton.setVisibility(visible ? VISIBLE : GONE);
    }
    //endregion
}
