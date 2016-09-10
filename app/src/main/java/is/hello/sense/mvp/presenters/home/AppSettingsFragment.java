package is.hello.sense.mvp.presenters.home;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
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
    public AppSettingsView getPresenterView() {
        if (presenterView == null) {
            return new AppSettingsView(getActivity());
        }
        return presenterView;
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
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
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenterView.viewCreated(view, this, showDeviceList(), tellAFriend());
        bindAndSubscribe(accountInteractor.account, this::bindAccount, Functions.LOG_ERROR);
        accountInteractor.update();
    }

    @Override
    public void onSwipeInteractionDidFinish() {
    }

    @Override
    public void onUpdate() {
    }

    private void bindAccount(@NonNull final Account account) {
        presenterView.setBreadcrumbVisible(Tutorial.TAP_NAME.shouldShow(getActivity()) && account.getCreated().isBefore(Constants.RELEASE_DATE_FOR_LAST_NAME));
    }

    @Override
    public void onResume() {
        super.onResume();
        accountInteractor.update();
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

    private View.OnClickListener tellAFriend() {
        return v -> {
            Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
            Share.text(getString(R.string.tell_a_friend_body))
                 .withSubject(getString(R.string.tell_a_friend_subject))
                 .send(getActivity());

        };
    }
}
