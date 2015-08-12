package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;

public class UnitSettingsFragment extends InjectionFragment implements Handler.Callback {
    private static final int REQUEST_CODE_ERROR = 0xE3;

    private static final int DELAY_PUSH_PREFERENCES = 3000;
    private static final int MSG_PUSH_PREFERENCES = 0x5;

    @Inject PreferencesPresenter preferencesPresenter;

    private final Handler handler = new Handler(Looper.getMainLooper(), this);
    private StaticItemAdapter.CheckItem use24TimeItem;

    private ProgressBar loadingIndicator;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_UNITS_TIME, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);

        this.listView = (ListView) view.findViewById(android.R.id.list);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());

        boolean use24Time = preferencesPresenter.getUse24Time();
        this.use24TimeItem = adapter.addCheckItem(R.string.setting_title_use_24_time, use24Time, item -> {
            updatePreference(PreferencesPresenter.USE_24_TIME, item);
        });

        listView.setOnItemClickListener(adapter);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showLoading();
        bindAndSubscribe(preferencesPresenter.pullAccountPreferences(),
                         ignored -> hideLoading(),
                         this::pullingPreferencesFailed);

        bindAndSubscribe(preferencesPresenter.observableUse24Time(),
                         use24TimeItem::setChecked,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.use24TimeItem = null;

        this.loadingIndicator = null;
        this.listView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (handler.hasMessages(MSG_PUSH_PREFERENCES)) {
            handler.removeMessages(MSG_PUSH_PREFERENCES);
            preferencesPresenter.pushAccountPreferences();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }


    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }


    public void updatePreference(@NonNull String key, @NonNull StaticItemAdapter.CheckItem item) {
        boolean update = !item.isChecked();
        preferencesPresenter.edit()
                            .putBoolean(key, update)
                            .apply();

        handler.removeMessages(MSG_PUSH_PREFERENCES);
        handler.sendEmptyMessageDelayed(MSG_PUSH_PREFERENCES, DELAY_PUSH_PREFERENCES);
    }

    public void pullingPreferencesFailed(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == MSG_PUSH_PREFERENCES) {
            preferencesPresenter.pushAccountPreferences();
            return true;
        }
        return false;
    }
}
