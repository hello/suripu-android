package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticItemAdapter;

public class SettingsActivity extends SenseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            showFragment(new RootSettingsFragment());
        }
    }


    private void showFragment(@NonNull Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (getSupportFragmentManager().findFragmentById(R.id.activity_settings_container) == null) {
            transaction.add(R.id.activity_settings_container, fragment);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.activity_settings_container, fragment);
            transaction.addToBackStack(fragment.getClass().getSimpleName());
        }
        transaction.commit();
    }


    public static class RootSettingsFragment extends ListFragment {
        private StaticItemAdapter adapter;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.adapter = new StaticItemAdapter(getActivity());
            adapter.addItem(getString(R.string.label_my_info), null);
            adapter.addItem(getString(R.string.label_account), null);
            adapter.addItem(getString(R.string.label_units_and_time), null);
            adapter.addItem(getString(R.string.label_devices), null);
            adapter.addItem(getString(R.string.action_log_out), null);
            setListAdapter(adapter);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            StaticItemAdapter.Item item = adapter.getItem(position);
            if (item.action != null) {
                item.action.run();
            }
        }
    }
}
