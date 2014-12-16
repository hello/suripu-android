package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;
import android.view.View;
import android.widget.ListView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.SenseApplication;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.activities.DebugActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.HelpUtil;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;

public class AppSettingsFragment extends ListFragment {
    @Inject ApiSessionManager sessionManager;
    @Inject BuildValues buildValues;

    private StaticItemAdapter adapter;

    public AppSettingsFragment() {
        SenseApplication.getInstance().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.adapter = new StaticItemAdapter(getActivity());
        adapter.addItem(getString(R.string.label_my_info), null, () -> showFragment(MyInfoFragment.class, R.string.label_my_info, null));
        adapter.addItem(getString(R.string.label_account), null, () -> showFragment(AccountSettingsFragment.class, R.string.label_account, null));
        adapter.addItem(getString(R.string.label_units_and_time), null, () -> showFragment(R.xml.settings_units_and_time, R.string.label_units_and_time));
        adapter.addItem(getString(R.string.label_devices), null, () -> showFragment(DeviceListFragment.class, R.string.label_devices, null));
        adapter.addItem(getString(R.string.action_help), null, this::showHelp);
        adapter.addItem(getString(R.string.action_log_out), null, this::logOut);

        if (buildValues.debugScreenEnabled) {
            adapter.addItem(getString(R.string.activity_debug), null, () -> startActivity(new Intent(getActivity(), DebugActivity.class)));
        }

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

    private void showFragment(@NonNull Class<? extends Fragment> fragmentClass,
                              @StringRes int titleRes,
                              @Nullable Bundle fragmentArguments) {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(getString(titleRes), fragmentClass, fragmentArguments);
        Intent intent = new Intent(getActivity(), FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        startActivity(intent);
    }

    private void showFragment(@XmlRes int prefsRes,
                              @StringRes int titleRes) {
        showFragment(StaticPreferencesFragment.class, titleRes, StaticPreferencesFragment.getArguments(prefsRes));
    }

    public void showHelp() {
        HelpUtil.showHelp(getActivity());
    }

    public void logOut() {
        SenseAlertDialog builder = new SenseAlertDialog(getActivity());
        builder.setTitle(R.string.dialog_title_log_out);
        builder.setMessage(R.string.dialog_message_log_out);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            Activity activity = getActivity();
            sessionManager.logOut(activity);
            activity.finish();

            Analytics.event(Analytics.EVENT_SIGNED_OUT, null);
        });
        builder.show();
    }

    //endregion
}
