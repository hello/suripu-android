package is.hello.sense.ui.activities;

import android.app.Fragment;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.fragments.settings.AccountSettingsFragment;
import is.hello.sense.ui.fragments.settings.DeviceListFragment;
import is.hello.sense.ui.fragments.settings.MyInfoFragment;
import is.hello.sense.ui.fragments.settings.SettingsFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

public class SettingsActivity extends FragmentNavigationActivity {
    private boolean isDeviceMenuVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            showFragment(new RootSettingsFragment(), getString(R.string.action_settings), false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isDeviceMenuVisible) {
            getMenuInflater().inflate(R.menu.settings, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pair_new_sense: {
                Intent onboarding = new Intent(this, OnboardingActivity.class);
                onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_QUESTIONS);
                startActivity(onboarding);
                return true;
            }

            case R.id.pair_new_pill: {
                Intent onboarding = new Intent(this, OnboardingActivity.class);
                onboarding.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                startActivity(onboarding);
                return true;
            }

            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @Override
    protected int getDefaultTitle() {
        return R.string.action_settings;
    }

    @Override
    public void showFragment(@NonNull Fragment fragment, @Nullable String title, boolean wantsBackStackEntry) {
        super.showFragment(fragment, title, wantsBackStackEntry);

        this.isDeviceMenuVisible = (fragment instanceof DeviceListFragment);
        invalidateOptionsMenu();
    }

    @Override
    public void onBackStackChanged() {
        super.onBackStackChanged();

        this.isDeviceMenuVisible = (getTopFragment() instanceof DeviceListFragment);
        invalidateOptionsMenu();
    }

    private void showSettings(@XmlRes int prefsRes, @StringRes int titleRes) {
        showFragment(SettingsFragment.newInstance(prefsRes), getString(titleRes), true);
    }


    //region Root Settings

    public static class RootSettingsFragment extends ListFragment {
        @Inject ApiSessionManager sessionManager;

        private StaticItemAdapter adapter;

        public RootSettingsFragment() {
            SenseApplication.getInstance().inject(this);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.adapter = new StaticItemAdapter(getActivity());
            adapter.addItem(getString(R.string.label_my_info), null, () -> getSettingsActivity().showFragment(new MyInfoFragment(), getString(R.string.label_my_info), true));
            adapter.addItem(getString(R.string.label_account), null, () -> getSettingsActivity().showFragment(new AccountSettingsFragment(), getString(R.string.label_account), true));
            adapter.addItem(getString(R.string.label_units_and_time), null, () -> getSettingsActivity().showSettings(R.xml.settings_units_and_time, R.string.label_units_and_time));
            adapter.addItem(getString(R.string.label_devices), null, () -> getSettingsActivity().showFragment(new DeviceListFragment(), getString(R.string.label_devices), true));
            adapter.addItem(getString(R.string.action_log_out), null, this::logOut);
            setListAdapter(adapter);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            StaticItemAdapter.Item item = adapter.getItem(position);
            if (item.getAction() != null) {
                item.getAction().run();
            }
        }


        //region Actions

        private SettingsActivity getSettingsActivity() {
            return (SettingsActivity) getActivity();
        }

        public void logOut() {
            SenseAlertDialog builder = new SenseAlertDialog(getActivity());
            builder.setTitle(R.string.dialog_title_log_out);
            builder.setMessage(R.string.dialog_message_log_out);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                SettingsActivity activity = getSettingsActivity();
                sessionManager.logOut(activity);
                activity.finish();

                Analytics.event(Analytics.EVENT_SIGNED_OUT, null);
            });
            builder.show();
        }

        //endregion
    }

    //endregion
}
