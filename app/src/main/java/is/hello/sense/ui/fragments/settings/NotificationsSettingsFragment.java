package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.HashMap;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.AccountPreference;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;

public class NotificationsSettingsFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
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
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        this.scoreItem = adapter.addCheckItem(R.string.notification_setting_sleep_score, false, this::updateScore);
        this.alertConditionsItem = adapter.addCheckItem(R.string.notification_setting_alert_conditions, false, this::updateAlertConditions);

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

    private boolean getBooleanPreference(@NonNull HashMap<AccountPreference.Key, Object> preferences,
                                         @NonNull AccountPreference.Key key,
                                         boolean defaultValue) {
        if (preferences.containsKey(key)) {
            return (boolean) preferences.get(key);
        } else {
            return defaultValue;
        }
    }

    public void bindPreferences(@NonNull HashMap<AccountPreference.Key, Object> preferences) {
        hideLoading();

        scoreItem.setChecked(getBooleanPreference(preferences, AccountPreference.Key.PUSH_SCORE, true));
        alertConditionsItem.setChecked(getBooleanPreference(preferences, AccountPreference.Key.PUSH_ALERT_CONDITIONS, true));
    }

    public void preferencesUnavailable(@NonNull Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    //endregion


    //region Actions

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StaticItemAdapter.Item item = (StaticItemAdapter.Item) parent.getItemAtPosition(position);
        if (item.getAction() != null) {
            item.getAction().run();
        }
    }

    public void updateScore() {
        AccountPreference update = new AccountPreference(AccountPreference.Key.PUSH_SCORE);
        update.setEnabled(!scoreItem.isChecked());
        showLoading();
        bindAndSubscribe(accountPresenter.updatePreference(update),
                         pref -> {
                             scoreItem.setChecked(pref.isEnabled());
                             hideLoading();
                         },
                         this::preferencesUnavailable);
    }

    public void updateAlertConditions() {
        AccountPreference update = new AccountPreference(AccountPreference.Key.PUSH_ALERT_CONDITIONS);
        update.setEnabled(!alertConditionsItem.isChecked());
        showLoading();
        bindAndSubscribe(accountPresenter.updatePreference(update),
                         pref -> {
                             alertConditionsItem.setChecked(pref.isEnabled());
                             hideLoading();
                         },
                         this::preferencesUnavailable);
    }

    //endregion
}
