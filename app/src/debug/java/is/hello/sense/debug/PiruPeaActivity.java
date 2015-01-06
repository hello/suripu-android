package is.hello.sense.debug;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.stacks.BluetoothStack;
import is.hello.sense.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Observable;

public class PiruPeaActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject BluetoothStack stack;
    @Inject ApiSessionManager apiSessionManager;
    @Inject HardwarePresenter hardwarePresenter;

    private SensePeripheral selectedPeripheral;

    private PeripheralAdapter scannedPeripheralsAdapter;
    private StaticItemAdapter peripheralActions;
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piru_pea);

        this.listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.scannedPeripheralsAdapter = new PeripheralAdapter(this);
        listView.setAdapter(scannedPeripheralsAdapter);

        this.peripheralActions = new StaticItemAdapter(this);

        peripheralActions.addItem("Disconnect", null, this::disconnect);
        peripheralActions.addItem("Pairing Mode", null, this::putIntoPairingMode);
        peripheralActions.addItem("Normal Mode", null, this::putIntoNormalMode);
        peripheralActions.addItem("Clear Paired Phone", null, this::clearPairedPhone);
        peripheralActions.addItem("Device Factory Reset", null, this::factoryReset);
        peripheralActions.addItem("Get WiFi Network", null, this::getWifiNetwork);
        peripheralActions.addItem("Set WiFi Network", null, this::setWifiNetwork);
        peripheralActions.addItem("Pair Pill Mode", null, this::pairPillMode);
        peripheralActions.addItem("Link Account", null, this::linkAccount);
        peripheralActions.addItem("Push Data", null, this::pushData);
        peripheralActions.addItem("Busy LEDs", null, this::busyLedAnimation);
        peripheralActions.addItem("Trippy LEDs", null, this::trippyLedAnimation);
        peripheralActions.addItem("Fade Out LEDs", null, this::stopAnimationWithFade);
        peripheralActions.addItem("Turn Off LEDs", null, this::stopAnimationWithoutFade);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (selectedPeripheral != null) {
            selectedPeripheral.disconnect().subscribe(ignored -> {}, Functions.LOG_ERROR);
            hardwarePresenter.setPeripheral(null);
        }
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

    public void presentError(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_piru_pea_container);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), this, e);
        }
    }

    //endregion


    //region Actions

    public <T> void runSimpleCommand(@NonNull Observable<T> command) {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(command,
                         ignored -> LoadingDialogFragment.close(getFragmentManager()),
                         this::presentError);
    }

    public void disconnect() {
        if (selectedPeripheral != null) {
            selectedPeripheral.disconnect().subscribe(ignored -> {}, Functions.LOG_ERROR);
            hardwarePresenter.setPeripheral(null);
            this.selectedPeripheral = null;
        }

        listView.setAdapter(scannedPeripheralsAdapter);
    }

    public void putIntoPairingMode() {
        runSimpleCommand(selectedPeripheral.setPairingModeEnabled(true));
    }

    public void putIntoNormalMode() {
        runSimpleCommand(selectedPeripheral.setPairingModeEnabled(false));
    }

    public void clearPairedPhone() {
        runSimpleCommand(selectedPeripheral.clearPairedPhone());
    }

    public void factoryReset() {
        SenseAlertDialog alertDialog = new SenseAlertDialog(this);
        alertDialog.setTitle(R.string.dialog_title_factory_reset);
        alertDialog.setMessage("This is a device only factory reset, all paired accounts in API will persist. Use the ‘Devices’ screen to perform a full factory reset.");
        alertDialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> runSimpleCommand(selectedPeripheral.factoryReset()));
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setDestructive(true);
        alertDialog.show();
    }

    public void getWifiNetwork() {
        LoadingDialogFragment.show(getFragmentManager());
        bindAndSubscribe(selectedPeripheral.getWifiNetwork(),
                network -> {
                    LoadingDialogFragment.close(getFragmentManager());
                    MessageDialogFragment dialogFragment = MessageDialogFragment.newInstance("Wifi Network", network.ssid + "\n" + network.connectionState);
                    dialogFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
                },
                this::presentError);
    }

    public void setWifiNetwork() {
        hardwarePresenter.setPeripheral(selectedPeripheral);

        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, true);
        startActivity(intent);
    }

    public void pairPillMode() {
        runSimpleCommand(selectedPeripheral.pairPill(apiSessionManager.getAccessToken()));
    }

    public void linkAccount() {
        runSimpleCommand(selectedPeripheral.linkAccount(apiSessionManager.getAccessToken()));
    }

    public void pushData() {
        runSimpleCommand(selectedPeripheral.pushData());
    }

    public void busyLedAnimation() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SensePeripheral.LedAnimation.BUSY));
    }

    public void trippyLedAnimation() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SensePeripheral.LedAnimation.TRIPPY));
    }

    public void stopAnimationWithFade() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SensePeripheral.LedAnimation.FADE_OUT));
    }

    public void stopAnimationWithoutFade() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SensePeripheral.LedAnimation.STOP));
    }

    //endregion


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (selectedPeripheral == null) {
            this.selectedPeripheral = scannedPeripheralsAdapter.getItem(position);

            LoadingDialogFragment loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager());
            bindAndSubscribe(selectedPeripheral.connect(), status -> {
                if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                    LoadingDialogFragment.close(getFragmentManager());
                    listView.setAdapter(peripheralActions);
                } else {
                    loadingDialogFragment.setTitle(getString(status.messageRes));
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
