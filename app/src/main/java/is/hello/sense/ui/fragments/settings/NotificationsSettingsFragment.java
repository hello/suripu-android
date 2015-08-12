package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.Map;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;

public class NotificationsSettingsFragment extends InjectionFragment {
    private static final int REQUEST_CODE_ERROR = 0xE3;

    @Inject AccountPresenter accountPresenter;

    private StaticItemAdapter.CheckItem scoreItem;
    private StaticItemAdapter.CheckItem alertConditionsItem;

    private ProgressBar loadingIndicator;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_NOTIFICATIONS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        this.listView = (ListView) view.findViewById(android.R.id.list);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        this.scoreItem = adapter.addCheckItem(R.string.notification_setting_sleep_score, false, this::updateScore);
        this.alertConditionsItem = adapter.addCheckItem(R.string.notification_setting_alert_conditions, false, this::updateAlertConditions);

        listView.setOnItemClickListener(adapter);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showLoading();
        bindAndSubscribe(accountPresenter.preferences(),
                this::bindPreferences,
                this::preferencesUnavailable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.scoreItem = null;
        this.alertConditionsItem = null;

        this.loadingIndicator = null;
        this.listView = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }


    //region Binding

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    private boolean getBooleanPreference(@NonNull Map<Account.Preference, Boolean> preferences,
                                         @NonNull Account.Preference name,
                                         boolean defaultValue) {
        if (preferences.containsKey(name)) {
            return preferences.get(name);
        } else {
            return defaultValue;
        }
    }

    public void bindPreferences(@NonNull Map<Account.Preference, Boolean> preferences) {
        hideLoading();

        scoreItem.setChecked(getBooleanPreference(preferences, Account.Preference.PUSH_SCORE, true));
        alertConditionsItem.setChecked(getBooleanPreference(preferences, Account.Preference.PUSH_ALERT_CONDITIONS, true));
    }

    public void preferencesUnavailable(@NonNull Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    //endregion


    //region Actions

    public void updateScore(@NonNull StaticItemAdapter.CheckItem item) {
        Map<Account.Preference, Boolean> update = Account.Preference.PUSH_SCORE.toUpdate(item.isChecked());
        showLoading();
        bindAndSubscribe(accountPresenter.updatePreferences(update),
                         prefs -> {
                             scoreItem.setChecked(Account.Preference.PUSH_SCORE.getFrom(prefs));
                             hideLoading();
                         },
                         this::preferencesUnavailable);
    }

    public void updateAlertConditions(@NonNull StaticItemAdapter.CheckItem item) {
        Map<Account.Preference, Boolean> update = Account.Preference.PUSH_ALERT_CONDITIONS.toUpdate(!item.isChecked());
        showLoading();
        bindAndSubscribe(accountPresenter.updatePreferences(update),
                         prefs -> {
                             alertConditionsItem.setChecked(Account.Preference.PUSH_ALERT_CONDITIONS.getFrom(prefs));
                             hideLoading();
                         },
                         this::preferencesUnavailable);
    }

    //endregion
}
