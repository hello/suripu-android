package is.hello.sense.ui.fragments.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.Date;
import java.util.TimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.TimeZoneAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.util.Logger;

public class DeviceTimeZoneFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject AccountPresenter accountPresenter;

    private ListView listView;
    private ProgressBar activityIndicator;

    private @Nullable Account account;
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

        TimeZoneAdapter adapter = new TimeZoneAdapter(getActivity());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        beginActivity();
        bindAndSubscribe(accountPresenter.account, this::bindAccount, this::presentError);
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
        if (account == null) {
            return;
        }

        String timeZoneId = (String) parent.getItemAtPosition(position);
        if (timeZoneId == null) {
            return;
        }

        DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
        int offset = timeZone.getOffset(Instant.now());
        account.setTimeZoneOffset(offset);

        beginActivity();
        bindAndSubscribe(accountPresenter.updateTimeZone(SenseTimeZone.fromDateTimeZone(timeZone)),
                ignored -> {
                    Logger.info(getClass().getSimpleName(), "Updated time zone");
                    bindAndSubscribe(accountPresenter.saveAccount(account),
                            updatedAccount -> {
                            },
                            this::presentError);
                },
                this::presentError);
    }


    public void bindAccount(@NonNull Account account) {
        this.account = account;

        DateTimeZone timeZone = DateTimeZone.forOffsetMillis(account.getTimeZoneOffset());
        TimeZone displayTimeZone = timeZone.toTimeZone();
        boolean inDST = displayTimeZone.inDaylightTime(new Date());
        String timeZoneName = displayTimeZone.getDisplayName(inDST, TimeZone.LONG);
        headerDetail.setText(timeZoneName);

        endActivity(true);
    }

    public void presentError(Throwable e) {
        endActivity(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
