package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.EnumSet;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class NotificationsSettingsFragment extends InjectionFragment implements Handler.Callback {
    private static final int REQUEST_CODE_ERROR = 0xE3;

    private static final int DELAY_PUSH_PREFERENCES = 3000;
    private static final int MSG_PUSH_PREFERENCES = 0x5;

    @Inject
    AccountInteractor accountPresenter;
    @Inject
    PreferencesInteractor preferences;

    private final Handler handler = new Handler(Looper.getMainLooper(), this);

    private ProgressBar loadingIndicator;
    private RecyclerView recyclerView;

    private SettingsRecyclerAdapter.ToggleItem pushScoreItem;
    private SettingsRecyclerAdapter.ToggleItem pushAlertConditionsItem;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_NOTIFICATIONS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.static_recycler, container, false);

        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.static_recycler_view_loading);

        this.recyclerView = (RecyclerView) view.findViewById(R.id.static_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     EnumSet.of(ScrollEdge.TOP), FadingEdgesItemDecoration.Style.STRAIGHT));

        final int verticalPadding = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);

        final SettingsRecyclerAdapter adapter = new SettingsRecyclerAdapter(getActivity());
        adapter.setWantsDividers(false);


        decoration.addTopInset(adapter.getItemCount(), verticalPadding);
        this.pushScoreItem =
                new SettingsRecyclerAdapter.ToggleItem(getString(R.string.notification_setting_sleep_score), () -> updatePreference(PreferencesInteractor.PUSH_SCORE_ENABLED, pushScoreItem));
        adapter.add(pushScoreItem);

        decoration.addBottomInset(adapter.getItemCount(), verticalPadding);
        this.pushAlertConditionsItem =
                new SettingsRecyclerAdapter.ToggleItem(getString(R.string.notification_setting_alert_conditions), () -> updatePreference(PreferencesInteractor.PUSH_ALERT_CONDITIONS_ENABLED, pushAlertConditionsItem));
        adapter.add(pushAlertConditionsItem);

        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        showLoading();
        bindAndSubscribe(accountPresenter.pullAccountPreferences(),
                         ignored -> hideLoading(),
                         this::pullingPreferencesFailed);

        final Observable<Boolean> pushScore =
                preferences.observableBoolean(PreferencesInteractor.PUSH_SCORE_ENABLED, true);
        bindAndSubscribe(pushScore,
                         pushScoreItem::setValue,
                         Functions.LOG_ERROR);

        final Observable<Boolean> pushAlertConditions =
                preferences.observableBoolean(PreferencesInteractor.PUSH_ALERT_CONDITIONS_ENABLED, true);
        bindAndSubscribe(pushAlertConditions,
                         pushAlertConditionsItem::setValue,
                         Functions.LOG_ERROR);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (handler.hasMessages(MSG_PUSH_PREFERENCES)) {
            handler.removeMessages(MSG_PUSH_PREFERENCES);
            accountPresenter.pushAccountPreferences();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.pushScoreItem = null;
        this.pushAlertConditionsItem = null;

        this.loadingIndicator = null;
        this.recyclerView = null;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ERROR && resultCode == Activity.RESULT_OK) {
            getActivity().finish();
        }
    }


    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    public void pullingPreferencesFailed(final Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, getActivity()).build();
        errorDialogFragment.setTargetFragment(this, REQUEST_CODE_ERROR);
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }


    public void updatePreference(@NonNull final String key, @NonNull final SettingsRecyclerAdapter.ToggleItem item) {
        final boolean update = !item.getValue();
        preferences.edit()
                   .putBoolean(key, update)
                   .apply();

        handler.removeMessages(MSG_PUSH_PREFERENCES);
        handler.sendEmptyMessageDelayed(MSG_PUSH_PREFERENCES, DELAY_PUSH_PREFERENCES);
    }

    @Override
    public boolean handleMessage(final Message msg) {
        if (msg.what == MSG_PUSH_PREFERENCES) {
            accountPresenter.pushAccountPreferences();
            return true;
        }
        return false;
    }
}
