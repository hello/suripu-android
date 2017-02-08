package is.hello.sense.flows.settings.ui.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.settings.ui.views.AppSettingsView;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.settings.NotificationsSettingsFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Share;

public class AppSettingsFragment extends PresenterFragment<AppSettingsView> implements
        AppSettingsView.Listener {

    @Inject
    HasVoiceInteractor hasVoiceInteractor;

    public static AppSettingsFragment newInstance() {
        return new AppSettingsFragment();
    }

    @Override
    public final void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new AppSettingsView(getActivity());
            this.presenterView.setListener(this);
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
        addInteractor(this.hasVoiceInteractor);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(this.hasVoiceInteractor.hasVoice,
                         this.presenterView::showVoiceEnabledRows,
                         e -> this.presenterView.showVoiceEnabledRows(false));


        this.presenterView.setDebugText(getString(R.string.app_version_fmt,
                                                  getString(R.string.app_name),
                                                  BuildConfig.VERSION_NAME));
        this.presenterView.showDebug(BuildConfig.DEBUG_SCREEN_ENABLED);
        this.hasVoiceInteractor.update();
    }

    public final void showFragment(@NonNull final Class<? extends Fragment> fragmentClass,
                                   @StringRes final int titleRes,
                                   final boolean lockOrientation) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
        builder.setDefaultTitle(titleRes);
        builder.setFragmentClass(fragmentClass);
        if (lockOrientation) {
            builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        startActivity(builder.toIntent());

    }

    private void onDevicesClick() {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
        builder.setDefaultTitle(R.string.label_devices);
        builder.setFragmentClass(DeviceListFragment.class);
        startActivity(builder.toIntent());
    }

    private void onShareClick() {
        Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
        Share.text(getString(R.string.tell_a_friend_body))
             .withSubject(getString(R.string.tell_a_friend_subject))
             .send(getActivity());

    }

    private void onExpansionsClick() {
        startActivity(new Intent(getActivity(), ExpansionSettingsActivity.class));
    }

    private void onVoiceClick() {
        startActivity(new Intent(getActivity(), VoiceSettingsActivity.class));
    }

    @Override
    public void onItemClicked(final int position) {
        //todo move this logic to activity one day
        switch (position) {
            case AppSettingsView.INDEX_ACCOUNT:
                showFragment(AccountSettingsFragment.class, R.string.label_account, true);
                break;
            case AppSettingsView.INDEX_DEVICES:
                onDevicesClick();
                break;
            case AppSettingsView.INDEX_NOTIFICATIONS:
                showFragment(NotificationsSettingsFragment.class, R.string.label_notifications, false);
                break;
            case AppSettingsView.INDEX_EXPANSIONS:
                onExpansionsClick();
                break;
            case AppSettingsView.INDEX_VOICE:
                onVoiceClick();
                break;
            case AppSettingsView.INDEX_SUPPORT:
                showFragment(SupportFragment.class, R.string.action_support, false);
                break;
            case AppSettingsView.INDEX_SHARE:
                onShareClick();
                break;
            case AppSettingsView.INDEX_DEBUG:
                Distribution.startDebugActivity(getActivity());

        }
    }
}
