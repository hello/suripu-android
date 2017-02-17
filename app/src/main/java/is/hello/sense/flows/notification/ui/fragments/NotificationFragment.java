package is.hello.sense.flows.notification.ui.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.NotificationSetting;
import is.hello.sense.api.model.VoidResponse;
import is.hello.sense.flows.notification.interactors.NotificationSettingsInteractor;
import is.hello.sense.flows.notification.ui.adapters.NotificationSettingsAdapter;
import is.hello.sense.flows.notification.ui.views.NotificationView;
import is.hello.sense.mvp.fragments.SenseViewFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.Analytics;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class NotificationFragment extends SenseViewFragment<NotificationView>
        implements ArrayRecyclerAdapter.ErrorHandler {

    @Inject
    NotificationSettingsInteractor notificationSettingsInteractor;
    private NotificationSettingsAdapter notificationSettingsAdapter;
    private Subscription saveSubscription = Subscriptions.empty();

    //region PresenterFragment
    @Override
    public void initializeSenseView() {
        if (senseView == null) {
            notificationSettingsAdapter = createAdapter();
            senseView = new NotificationView(getActivity(),
                                                 notificationSettingsAdapter);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState == null) {
            showBlockingActivity(null);
        }
        addInteractor(notificationSettingsInteractor);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.notificationSettingsAdapter.showNotificationHeader(!NotificationManagerCompat.from(getActivity()).areNotificationsEnabled());
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu,
                                    final MenuInflater inflater) {
        inflater.inflate(R.menu.save, menu);
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        notificationSettingsAdapter.setErrorHandler(null);
        saveSubscription.unsubscribe();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save) {
            onSaveSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(notificationSettingsInteractor.notificationSettings,
                         this::bindNotificationSettings,
                         this::bindNotificationSettingsError);
        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_NOTIFICATIONS, null);
            this.notificationSettingsInteractor.update();
        } else {
            this.notificationSettingsAdapter.restoreState(savedInstanceState);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.notificationSettingsAdapter != null) {
            this.notificationSettingsAdapter.saveState(outState);
        }
    }

    //endregion

    //region ErrorHandler
    @Override
    public void retry() {
        showProgress(true);
        notificationSettingsInteractor.update();
    }
    //endregion

    //region methods
    private void bindNotificationSettings(@NonNull final List<NotificationSetting> settings) {
        for (final NotificationSetting setting : settings) {
            if (NotificationSetting.SLEEP_REMINDER.equalsIgnoreCase(setting.getType())) {
                settings.remove(setting);
            }
        }
        this.notificationSettingsAdapter.bindSettings(settings);
        showProgress(false);
    }

    private void bindNotificationSettingsError(@NonNull final Throwable throwable) {
        this.notificationSettingsAdapter.setHasError(true);
        showProgress(false);

    }

    private void bindSave(@NonNull final VoidResponse voidResponse) {
        hideBlockingActivity(true, this::finishFlow);
    }

    private void bindSaveError(@NonNull final Throwable throwable) {
        hideBlockingActivity(false, null);
        showErrorDialog(new ErrorDialogFragment.PresenterBuilder(throwable));
    }

    private NotificationSettingsAdapter createAdapter() {
        final NotificationSettingsAdapter adapter = new NotificationSettingsAdapter();
        adapter.setErrorHandler(this);
        return adapter;
    }

    private void showProgress(final boolean show) {
        if (show) {
            senseView.setVisibility(View.INVISIBLE);
            showBlockingActivity(null);
        } else {
            hideBlockingActivity(false, null);
            senseView.setVisibility(View.VISIBLE);
        }
    }

    private void onSaveSelected() {
        if (this.senseView == null) {
            return;
        }
        showLockedBlockingActivity(R.string.updating);
        final List<NotificationSetting> settings = this.notificationSettingsAdapter.getItems();
        saveSubscription.unsubscribe();
        saveSubscription = bind(notificationSettingsInteractor.updateNotificationSettings(settings))
                .subscribe(
                        this::bindSave,
                        this::bindSaveError);
    }
    //endregion
}
