package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.settings.DevicesFragment;
import is.hello.sense.ui.fragments.settings.MyInfoFragment;
import is.hello.sense.ui.fragments.settings.SettingsFragment;
import is.hello.sense.util.Analytics;

public class SettingsActivity extends InjectionActivity implements FragmentNavigation {
    @Inject ApiSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            showFragment(new RootSettingsFragment(), null, false);
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void showFragment(@NonNull Fragment fragment,
                             @Nullable String title,
                             boolean wantsBackStackEntry) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        if (getFragmentManager().findFragmentById(R.id.activity_settings_container) == null) {
            transaction.add(R.id.activity_settings_container, fragment, tag);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.activity_settings_container, fragment, tag);
        }

        if (wantsBackStackEntry) {
            transaction.setBreadCrumbTitle(title);
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }

        transaction.commit();
    }

    private void showSettings(@XmlRes int prefsRes) {
        showFragment(SettingsFragment.newInstance(prefsRes), null, true);
    }


    //region Root Settings

    public static class RootSettingsFragment extends ListFragment {
        private StaticItemAdapter adapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.adapter = new StaticItemAdapter(getActivity());
            adapter.addItem(getString(R.string.label_my_info), null, () -> getSettingsActivity().showFragment(new MyInfoFragment(), null, true));
            adapter.addItem(getString(R.string.label_account), null);
            adapter.addItem(getString(R.string.label_units_and_time), null, () -> getSettingsActivity().showSettings(R.xml.settings_units_and_time));
            adapter.addItem(getString(R.string.label_devices), null, () -> getSettingsActivity().showFragment(new DevicesFragment(), null, true));
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
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.dialog_title_log_out);
            builder.setMessage(R.string.dialog_message_log_out);
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                SettingsActivity activity = getSettingsActivity();
                activity.sessionManager.logOut(activity);
                activity.finish();

                Analytics.event(Analytics.EVENT_SIGNED_OUT, null);
            });
            builder.create().show();
        }

        //endregion
    }

    //endregion
}
