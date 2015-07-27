package is.hello.sense.ui.fragments.settings;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.UndersideTabFragment;
import is.hello.sense.ui.fragments.support.SupportFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class AppSettingsFragment extends UndersideTabFragment {
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

        TextView version = (TextView) view.findViewById(R.id.fragment_app_settings_version);
        version.setText(getString(R.string.app_version_fmt, getString(R.string.app_name), BuildConfig.VERSION_NAME));
        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            Views.setSafeOnClickListener(version, v -> startActivity(new Intent(getActivity(), DebugActivity.class)));
        }

        return view;
    }

    @Override
    public void onSwipeInteractionDidFinish() {

    }

    @Override
    public void onUpdate() {

    }


    private void showDeviceList(@NonNull View ignored) {
        Bundle intentArguments = HardwareFragmentActivity.getArguments(getString(R.string.label_devices), DeviceListFragment.class, null);
        Intent intent = new Intent(getActivity(), HardwareFragmentActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }

    private void showFragment(@NonNull Class<? extends Fragment> fragmentClass, @StringRes int titleRes) {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(titleRes), fragmentClass, null);
        Intent intent = new Intent(getActivity(), FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }
}
