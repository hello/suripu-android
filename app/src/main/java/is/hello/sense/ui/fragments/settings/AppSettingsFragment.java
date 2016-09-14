package is.hello.sense.ui.fragments.settings;

import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.BacksideTabFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.ui.handholding.Tutorial;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Share;

public class AppSettingsFragment extends BacksideTabFragment {
    private ImageView breadcrumb;

    @Inject
    AccountInteractor accountPresenter;

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
        addPresenter(accountPresenter);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_app_settings, container, false);
        breadcrumb = (ImageView) view.findViewById(R.id.fragment_app_settings_breadcrumb);

        final View accountItem = view.findViewById(R.id.fragment_app_settings_account);
        Views.setSafeOnClickListener(accountItem, ignored -> showFragment(AccountSettingsFragment.class, R.string.label_account, true));

        final View devicesItem = view.findViewById(R.id.fragment_app_settings_devices);
        Views.setSafeOnClickListener(devicesItem, this::showDeviceList);

        final View notificationsItem = view.findViewById(R.id.fragment_app_settings_notifications);
        Views.setSafeOnClickListener(notificationsItem, ignored -> showFragment(NotificationsSettingsFragment.class, R.string.label_notifications, false));

        final View unitsItem = view.findViewById(R.id.fragment_app_settings_units);
        Views.setSafeOnClickListener(unitsItem, ignored -> showFragment(UnitSettingsFragment.class, R.string.label_units_and_time, false));

        final View supportItem = view.findViewById(R.id.fragment_app_settings_support);
        Views.setSafeOnClickListener(supportItem, ignored -> showFragment(SupportFragment.class, R.string.action_support, false));

        final View tellAFriendItem = view.findViewById(R.id.fragment_app_settings_tell_a_friend);
        Views.setSafeOnClickListener(tellAFriendItem, ignored -> tellAFriend());

        final TextView version = (TextView) view.findViewById(R.id.fragment_app_settings_version);
        version.setText(getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Views.setSafeOnClickListener(version, ignored -> Distribution.startDebugActivity(getActivity()));
        }

        return view;
    }


    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(accountPresenter.account, this::bindAccount, Functions.LOG_ERROR);
        accountPresenter.update();
    }

    @Override
    public void onSwipeInteractionDidFinish() {
    }

    @Override
    public void onUpdate() {
    }

    private void bindAccount(@NonNull final Account account) {
        if (Tutorial.TAP_NAME.shouldShow(getActivity()) && account.getCreated().isBefore(Constants.RELEASE_DATE_FOR_LAST_NAME)) {
            breadcrumb.setVisibility(View.VISIBLE);
        } else {
            breadcrumb.setVisibility(View.GONE);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        accountPresenter.update();
    }

    @Override
    public void onPause() {
        super.onPause();
        breadcrumb.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        breadcrumb = null;
    }

    private void showDeviceList(@NonNull final View ignored) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
        builder.setDefaultTitle(R.string.label_devices);
        builder.setFragmentClass(DeviceListFragment.class);
        startActivity(builder.toIntent());
    }

    private void showFragment(@NonNull final Class<? extends Fragment> fragmentClass,
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

    private void tellAFriend() {
        Analytics.trackEvent(Analytics.Backside.EVENT_TELL_A_FRIEND_TAPPED, null);
        Share.text(getString(R.string.tell_a_friend_body))
             .withSubject(getString(R.string.tell_a_friend_subject))
             .send(getActivity());
    }
}
