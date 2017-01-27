package is.hello.sense.flows.home.ui.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.flows.expansions.ui.activities.ExpansionSettingsActivity;
import is.hello.sense.flows.home.ui.views.AppSettingsView;
import is.hello.sense.flows.voice.ui.activities.VoiceSettingsActivity;
import is.hello.sense.interactors.HasVoiceInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Share;

public class AppSettingsFragment extends PresenterFragment<AppSettingsView> implements
        AppSettingsView.RunnableGenerator {

    @Inject
    HasVoiceInteractor hasVoiceInteractor;


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
        addInteractor(hasVoiceInteractor);
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(hasVoiceInteractor.hasVoice,
                         this.presenterView::showVoiceEnabledRows,
                         e -> this.presenterView.showVoiceEnabledRows(false));

        hasVoiceInteractor.update();
    }

    @Override
    public final Runnable create(@NonNull final Class<? extends Fragment> fragmentClass,
                                 @StringRes final int titleRes,
                                 final boolean lockOrientation) {
        return () -> {
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
