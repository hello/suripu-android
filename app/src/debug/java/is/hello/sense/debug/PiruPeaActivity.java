package is.hello.sense.debug;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Logger;

public class PiruPeaActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject BluetoothStack stack;

    private SensePeripheral selectedPeripheral;

    private PeripheralAdapter scannedPeripheralsAdapter;
    private StaticItemAdapter peripheralActions;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_list_view);

        this.listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.scannedPeripheralsAdapter = new PeripheralAdapter(this);
        listView.setAdapter(scannedPeripheralsAdapter);

        this.peripheralActions = new StaticItemAdapter(this);

        peripheralActions.addItem("Disconnect", null, this::disconnect);
        peripheralActions.addItem("Pairing Mode", null);
        peripheralActions.addItem("Normal Mode", null);
        peripheralActions.addItem("Get Device ID", null);
        peripheralActions.addItem("Factory Reset", null);
        peripheralActions.addItem("Get WiFi Network", null);
        peripheralActions.addItem("Set WiFi Network", null);
        peripheralActions.addItem("Pair Pill", null);
        peripheralActions.addItem("Link Account", null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_piru_pea, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_scan) {
            scan();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //region Scanning

    public void scan() {
        disconnect();
        scannedPeripheralsAdapter.clear();

        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(SensePeripheral.discover(stack, new PeripheralCriteria()),
                peripherals -> {
                    scannedPeripheralsAdapter.addAll(peripherals);
                    LoadingDialogFragment.close(getFragmentManager());
                },
                this::presentError);
    }

    public void presentError(@Nullable Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }

    //endregion


    //region Actions

    public void disconnect() {
        if (selectedPeripheral != null) {
            selectedPeripheral.disconnect().subscribe(ignored -> {}, Functions.LOG_ERROR);
            this.selectedPeripheral = null;
        }

        listView.setAdapter(scannedPeripheralsAdapter);
    }

    //endregion


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (selectedPeripheral == null) {
            this.selectedPeripheral = scannedPeripheralsAdapter.getItem(position);

            LoadingDialogFragment loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(selectedPeripheral.connect(), status -> {
                switch (status) {
                    case CONNECTING:
                        loadingDialogFragment.setTitle(getString(R.string.title_connecting));
                        break;

                    case BONDING:
                        loadingDialogFragment.setTitle(getString(R.string.title_pairing));
                        break;

                    case DISCOVERING_SERVICES:
                        loadingDialogFragment.setTitle(getString(R.string.title_discovering_services));
                        break;

                    case CONNECTED:
                        LoadingDialogFragment.close(getFragmentManager());
                        listView.setAdapter(peripheralActions);
                        break;
                }
            }, this::presentError);
        } else {
            StaticItemAdapter.Item selectedItem = peripheralActions.getItem(position);
            if (selectedItem.getAction() != null) {
                selectedItem.getAction().run();
            }
        }
    }


    private class PeripheralAdapter extends ArrayAdapter<SensePeripheral> {
        private PeripheralAdapter(@NonNull Context context) {
            super(context, R.layout.item_simple_text);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView) super.getView(position, convertView, parent);

            SensePeripheral peripheral = getItem(position);
            text.setText(peripheral.getName() + " - " + peripheral.getAddress());

            return text;
        }
    }
}
