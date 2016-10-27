package is.hello.sense.flows.home.ui.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.flows.home.ui.views.AppSettingsView;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Share;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class AppSettingsFragment extends BacksideTabFragment<AppSettingsView> implements
        AppSettingsView.ClickListenerGenerator {

    @Inject
    AccountInteractor accountInteractor;
    @Inject
    PreferencesInteractor preferencesInteractor;
    @Inject
    DevicesInteractor devicesInteractor;

    @NonNull
    private Subscription devicesSubscription = Subscriptions.empty();


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new AppSettingsView(getActivity(),
                                                this,
                                                this::onDeviceListClick,
                                                this::onTellAFriendClick,
                                                this::onExpansionsClick,
                                                this::onVoiceClick);
        }
    }

    @Override
    public final void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Analytics.trackEvent(Analytics.Backside.EVENT_SETTINGS, null);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addInteractor(accountInteractor);
        addInteractor(preferencesInteractor);
        addInteractor(devicesInteractor);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(accountInteractor.account,
                         this::bindAccount,
                         Functions.LOG_ERROR);

        bindAndSubscribe(preferencesInteractor.observableBoolean(PreferencesInteractor.HAS_VOICE, false),
                         this.presenterView::showVoiceEnabledRows,
                         Functions.LOG_ERROR);

        // If this preference is missing we need to query the server.
        if (!preferencesInteractor.contains(PreferencesInteractor.HAS_VOICE)) {
            devicesSubscription.unsubscribe();
            devicesSubscription = bind(devicesInteractor.devices)
                    .subscribe((devices -> preferencesInteractor.setDevice(devices.getSense())),
                               Functions.LOG_ERROR);
            devicesInteractor.update();
        }

        accountInteractor.update();
    }

    @Override
    public final void onSwipeInteractionDidFinish() {
    }

    @Override
    public final void onUpdate() {
    }

    @Override
    public final void onResume() {
        super.onResume();
        accountInteractor.update();
    }

    @Override
    protected void onRelease() {
        super.onRelease();
        devicesSubscription.unsubscribe();
        devicesSubscription = Subscriptions.empty();
    }

    @Override
    public final View.OnClickListener create(@NonNull final Class<? extends Fragment> fragmentClass,
                                             @StringRes final int titleRes,
                                             final boolean lockOrientation) {
        return v -> {
            final FragmentNavigationActivity.Builder builder =
                    new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
            builder.setDefaultTitle(titleRes);
            builder.setFragmentClass(fragmentClass);
            if (lockOrientation) {
                builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            startActivity(builder.toIntent());
        };

    }

    @VisibleForTesting
    public void bindAccount(@NonNull final Account account) {
        presenterView.setBreadcrumbVisible(Tutorial.TAP_NAME.shouldShow(getActivity()) && account.getCreated().isBefore(Constants.RELEASE_DATE_FOR_LAST_NAME));
    }


    private void onDeviceListClick(final View ignored) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
        builder.setDefaultTitle(R.string.label_devices);
        builder.setFragmentClass(DeviceListFragment.class);
        startActivity(builder.toIntent());
    }

    private void onTellAFriendClick(final View ignored) {
        Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
        Share.text(getString(R.string.tell_a_friend_body))
             .withSubject(getString(R.string.tell_a_friend_subject))
             .send(getActivity());

    }

    private void onExpansionsClick(final View ignored) {
        startActivity(new Intent(getActivity(), ExpansionSettingsActivity.class));
    }

    private void onVoiceClick(final View ignored) {
        startActivity(new Intent(getActivity(), VoiceSettingsActivity.class));
    }
}
