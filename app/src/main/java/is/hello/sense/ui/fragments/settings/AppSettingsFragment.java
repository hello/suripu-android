package is.hello.sense.ui.fragments.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.UndersideTabFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.Share;

public class AppSettingsFragment extends UndersideTabFragment {
    private static final int SHARE_CODE = 0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_SETTINGS, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);

        View accountItem = view.findViewById(R.id.fragment_app_settings_account);
        Views.setSafeOnClickListener(accountItem, ignored -> showFragment(AccountSettingsFragment.class, R.string.label_account));

        View devicesItem = view.findViewById(R.id.fragment_app_settings_devices);
        Views.setSafeOnClickListener(devicesItem, this::showDeviceList);

        View notificationsItem = view.findViewById(R.id.fragment_app_settings_notifications);
        Views.setSafeOnClickListener(notificationsItem, ignored -> showFragment(NotificationsSettingsFragment.class, R.string.label_notifications));

        View unitsItem = view.findViewById(R.id.fragment_app_settings_units);
        Views.setSafeOnClickListener(unitsItem, ignored -> showFragment(UnitSettingsFragment.class, R.string.label_units_and_time));

        View supportItem = view.findViewById(R.id.fragment_app_settings_support);
        Views.setSafeOnClickListener(supportItem, ignored -> showFragment(SupportFragment.class, R.string.action_support));

        View tellAFriendItem = view.findViewById(R.id.fragment_app_settings_tell_a_friend);
        Views.setSafeOnClickListener(tellAFriendItem, ignored -> tellAFriend());

        TextView version = (TextView) view.findViewById(R.id.fragment_app_settings_version);
        version.setText(getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Views.setSafeOnClickListener(version, ignored -> Distribution.startDebugActivity(getActivity()));
        }

        return view;
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Analytics.trackEvent(Analytics.TopView.EVENT_TELL_A_FRIEND_COMPLETED, null);
    }

    private void showDeviceList(@NonNull View ignored) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
        builder.setDefaultTitle(R.string.label_devices);
        builder.setFragmentClass(DeviceListFragment.class);
        startActivity(builder.toIntent());
    }

    private void showFragment(@NonNull Class<? extends Fragment> fragmentClass, @StringRes int titleRes) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(getActivity(), HardwareFragmentActivity.class);
        builder.setDefaultTitle(titleRes);
        builder.setFragmentClass(fragmentClass);
        startActivity(builder.toIntent());
    }

    private void tellAFriend() {
        Analytics.trackEvent(Analytics.TopView.EVENT_TELL_A_FRIEND_TAPPED, null);
        Share.text(getString(R.string.tell_a_friend_body))
             .withSubject(getString(R.string.tell_a_friend_subject))
             .sendForResult(this, SHARE_CODE);
    }
}
