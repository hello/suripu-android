package is.hello.sense.mvp.presenters.home;

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
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.mvp.view.home.AppSettingsView;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Share;

public class AppSettingsFragment extends BacksideTabFragment<AppSettingsView> implements
        AppSettingsView.ClickListenerGenerator {

    @Inject
    AccountInteractor accountInteractor;


    @Override
    public final void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new AppSettingsView(getActivity(),
                                                this,
                                                showDeviceList(),
                                                tellAFriend(),
                                                showExpansions()); //todo if should not have expansions make null
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
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(accountInteractor.account, this::bindAccount, Functions.LOG_ERROR);
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


    private View.OnClickListener showDeviceList() {
        return v -> {
            final FragmentNavigationActivity.Builder builder =
                    new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
            builder.setDefaultTitle(R.string.label_devices);
            builder.setFragmentClass(DeviceListFragment.class);
            startActivity(builder.toIntent());
        };
    }

    private View.OnClickListener tellAFriend() {
        return v -> {
            Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
            Share.text(getString(R.string.tell_a_friend_body))
                 .withSubject(getString(R.string.tell_a_friend_subject))
                 .send(getActivity());

        };
    }

    private View.OnClickListener showExpansions() {
        return v -> {
            startActivity(new Intent(getActivity(), ExpansionSettingsActivity.class));
        };
    }
}
