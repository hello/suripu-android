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

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.TimeZoneAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Logger;

public class DeviceTimeZoneFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject AccountPresenter accountPresenter;

    private ListView listView;
    private ProgressBar activityIndicator;

    private @Nullable Account account;

    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(accountPresenter);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        TimeZoneAdapter adapter = new TimeZoneAdapter(getActivity());
        listView.setAdapter(adapter);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

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
        DateTimeZone timeZone = DateTimeZone.forID(timeZoneId);
        int offset = timeZone.getOffset(DateTimeUtils.currentTimeMillis());
        account.setTimeZoneOffset(offset);

        beginActivity();

        bindAndSubscribe(accountPresenter.saveAccount(account),
                         ignored -> {},
                         this::presentError);

        accountPresenter.updateTimeZone(SenseTimeZone.fromDateTimeZone(timeZone))
                        .subscribe(ignored -> Logger.info(getClass().getSimpleName(), "Updated time zone"),
                                   Functions.LOG_ERROR);
    }


    public void bindAccount(@NonNull Account account) {
        this.account = account;
        endActivity(true);
    }

    public void presentError(Throwable e) {
        endActivity(false);
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }
}
