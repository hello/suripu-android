package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.DateTimeZone;

import java.util.Date;
import java.util.TimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.TimeZoneAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Logger;

public class DeviceTimeZoneFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject AccountPresenter accountPresenter;

    private ListView listView;
    private ProgressBar activityIndicator;
    private TextView headerDetail;

    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountPresenter.update();
        addPresenter(accountPresenter);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        View header = inflater.inflate(R.layout.item_static_text, listView, false);

        TextView headerTitle = (TextView) header.findViewById(R.id.item_static_text_title);
        headerTitle.setText(R.string.label_time_zone);

        this.headerDetail = (TextView) header.findViewById(R.id.item_static_text_detail);
        headerDetail.setText(R.string.missing_data_placeholder);

        ListViews.addHeaderView(listView, header, null, false);

        View headerDivider = Styles.createHorizontalDivider(getActivity(), ViewGroup.LayoutParams.MATCH_PARENT);
        headerDivider.setLayoutParams(new AbsListView.LayoutParams(headerDivider.getLayoutParams()));
        ListViews.addHeaderView(listView, headerDivider, null, false);

        TimeZoneAdapter adapter = new TimeZoneAdapter(getActivity());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        beginActivity();
        bindAndSubscribe(accountPresenter.currentTimeZone(), this::bindTimeZone, this::presentError);
    }

    //endregion


    private void beginActivity() {
        listView.setVisibility(View.INVISIBLE);
        activityIndicator.setVisibility(View.VISIBLE);
    }

    private void endActivity(boolean success) {
        if (success) {
            listView.setVisibility(View.VISIBLE);
        }
        activityIndicator.setVisibility(View.GONE);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String timeZoneId = (String) parent.getItemAtPosition(position);
        if (timeZoneId == null) {
            return;
        }

        DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
        SenseTimeZone senseTimeZone = SenseTimeZone.fromDateTimeZone(timeZone);
        LoadingDialogFragment.show(getFragmentManager(), null, true);
        bindAndSubscribe(accountPresenter.updateTimeZone(senseTimeZone),
                         ignored -> {
                             Logger.info(getClass().getSimpleName(), "Updated time zone");
                             LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {
                                 FragmentNavigation navigation = (FragmentNavigation) getActivity();
                                 navigation.popFragment(this, true);
                             });
                         },
                         this::presentError);
    }


    public void bindTimeZone(@NonNull SenseTimeZone senseTimeZone) {
        DateTimeZone timeZone = senseTimeZone.toDateTimeZone();
        TimeZone displayTimeZone = timeZone.toTimeZone();
        boolean inDST = displayTimeZone.inDaylightTime(new Date());
        String timeZoneName = displayTimeZone.getDisplayName(inDST, TimeZone.LONG);
        headerDetail.setText(timeZoneName);

        endActivity(true);
    }

    public void presentError(Throwable e) {
        endActivity(false);
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
