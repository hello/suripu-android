package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.segment.analytics.Properties;

import org.joda.time.DateTimeZone;

import java.util.Date;
import java.util.EnumSet;
import java.util.TimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.ui.adapter.TimeZoneAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Logger;

public class DeviceTimeZoneFragment extends InjectionFragment implements TimeZoneAdapter.OnClickListener {
    @Inject
    AccountInteractor accountPresenter;

    private RecyclerView recyclerView;
    private ProgressBar activityIndicator;
    private TextView currentTimeZone;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);

        Analytics.trackEvent(Analytics.Backside.EVENT_TIME_ZONE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_device_time_zone, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.fragment_device_time_zone_loading);
        this.currentTimeZone = (TextView) view.findViewById(R.id.fragment_device_time_current_zone);
        currentTimeZone.setText(R.string.missing_data_placeholder);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_device_time_zone_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Activity activity = getActivity();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, getResources(),
                                                                     EnumSet.of(ScrollEdge.TOP), FadingEdgesItemDecoration.Style.STRAIGHT));
        recyclerView.setAdapter(new TimeZoneAdapter(activity, this));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        beginActivity();
        bindAndSubscribe(accountPresenter.currentTimeZone(), this::bindTimeZone, this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.recyclerView = null;
        this.activityIndicator = null;
        this.currentTimeZone = null;
    }

    //endregion


    private void beginActivity() {
        recyclerView.setVisibility(View.INVISIBLE);
        activityIndicator.setVisibility(View.VISIBLE);
    }

    private void endActivity(boolean success) {
        if (success) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        activityIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onTimeZoneItemClicked(int position) {
        final String[] timeZoneIds = getResources().getStringArray(R.array.timezone_ids);
        if (timeZoneIds == null || timeZoneIds.length == 0 || timeZoneIds.length < position + 1) {
            return; // error
        }
        final String timeZoneId = timeZoneIds[position];
        final DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
        final SenseTimeZone senseTimeZone = SenseTimeZone.fromDateTimeZone(timeZone);
        LoadingDialogFragment.show(getFragmentManager(),
                                   null, LoadingDialogFragment.OPAQUE_BACKGROUND);
        bindAndSubscribe(accountPresenter.updateTimeZone(senseTimeZone),
                         ignored -> {
                             Logger.info(getClass().getSimpleName(), "Updated time zone");

                             final Properties properties =
                                     Analytics.createProperties(Analytics.Backside.PROP_TIME_ZONE,
                                                                senseTimeZone.timeZoneId);
                             Analytics.trackEvent(Analytics.Backside.EVENT_TIME_ZONE_CHANGED, properties);

                             LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);

                             getFragmentNavigation().popFragment(this, true);
                         },
                         this::presentError);
    }


    public void bindTimeZone(@NonNull SenseTimeZone senseTimeZone) {
        final DateTimeZone timeZone = senseTimeZone.toDateTimeZone();
        final TimeZone displayTimeZone = timeZone.toTimeZone();
        final boolean inDST = displayTimeZone.inDaylightTime(new Date());
        final String timeZoneName = displayTimeZone.getDisplayName(inDST, TimeZone.LONG);
        currentTimeZone.setText(timeZoneName);

        endActivity(true);
    }

    public void presentError(Throwable e) {
        endActivity(false);
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getActivity(), e);
    }
}
