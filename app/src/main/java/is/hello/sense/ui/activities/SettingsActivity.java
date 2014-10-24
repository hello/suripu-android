package is.hello.sense.ui.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.XmlRes;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.fragments.settings.DevicesFragment;
import is.hello.sense.ui.fragments.settings.MyInfoFragment;
import is.hello.sense.ui.fragments.settings.SettingsFragment;

public class SettingsActivity extends InjectionActivity {
    @Inject ApiSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            showFragment(new RootSettingsFragment());
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

    public void showFragment(@NonNull Fragment fragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        String tag = fragment.getClass().getSimpleName();
        if (getFragmentManager().findFragmentById(R.id.activity_settings_container) == null) {
            transaction.add(R.id.activity_settings_container, fragment, tag);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.replace(R.id.activity_settings_container, fragment, tag);
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private void showSettings(@XmlRes int prefsRes) {
        showFragment(SettingsFragment.newInstance(prefsRes));
    }


    //region Root Settings

    public static class RootSettingsFragment extends ListFragment {
        private StaticItemAdapter adapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.adapter = new StaticItemAdapter(getActivity());
            adapter.addItem(getString(R.string.label_my_info), null, () -> getSettingsActivity().showFragment(new MyInfoFragment()));
            adapter.addItem(getString(R.string.label_account), null);
            adapter.addItem(getString(R.string.label_units_and_time), null, () -> getSettingsActivity().showSettings(R.xml.settings_units_and_time));
            adapter.addItem(getString(R.string.label_devices), null, () -> getSettingsActivity().showFragment(new DevicesFragment()));
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
            });
            builder.create().show();
        }

        //endregion
    }

    //endregion
}
