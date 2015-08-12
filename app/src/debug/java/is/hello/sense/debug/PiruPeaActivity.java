package is.hello.sense.debug;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.util.Operation;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.bluetooth.sense.SensePeripheral;
import is.hello.sense.bluetooth.sense.model.SenseLedAnimation;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class PiruPeaActivity extends InjectionActivity implements AdapterView.OnItemClickListener {
    @Inject BluetoothStack stack;
    @Inject ApiSessionManager apiSessionManager;
    @Inject HardwarePresenter hardwarePresenter;

    private SensePeripheral selectedPeripheral;

    private PeripheralAdapter scannedPeripheralsAdapter;
    private StaticItemAdapter peripheralActions;
    private ListView listView;
    private ProgressBar loadingIndicator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_view_static);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.loadingIndicator = (ProgressBar) findViewById(R.id.list_view_static_loading);

        this.listView = (ListView) findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.scannedPeripheralsAdapter = new PeripheralAdapter(this);
        listView.setAdapter(scannedPeripheralsAdapter);

        this.peripheralActions = new StaticItemAdapter(this);

        peripheralActions.addTextItem("Disconnect", null, this::disconnect);
        peripheralActions.addTextItem("Pairing Mode", null, this::putIntoPairingMode);
        peripheralActions.addTextItem("Normal Mode", null, this::putIntoNormalMode);
        peripheralActions.addTextItem("Device Factory Reset", null, this::factoryReset);
        peripheralActions.addTextItem("Get WiFi Network", null, this::getWifiNetwork);
        peripheralActions.addTextItem("Set WiFi Network", null, this::setWifiNetwork);
        peripheralActions.addTextItem("Pair Pill Mode", null, this::pairPillMode);
        peripheralActions.addTextItem("Link Account", null, this::linkAccount);
        peripheralActions.addTextItem("Push Data", null, this::pushData);
        peripheralActions.addTextItem("Busy LEDs", null, this::busyLedAnimation);
        peripheralActions.addTextItem("Trippy LEDs", null, this::trippyLedAnimation);
        peripheralActions.addTextItem("Fade Out LEDs", null, this::stopAnimationWithFade);
        peripheralActions.addTextItem("Turn Off LEDs", null, this::stopAnimationWithoutFade);

        IntentFilter filter = new IntentFilter(HardwarePresenter.ACTION_CONNECTION_LOST);
        Observable<Intent> onConnectionLost = Rx.fromLocalBroadcast(this, filter);
        bindAndSubscribe(onConnectionLost,
                         intent -> disconnect(null),
                         Functions.LOG_ERROR);
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


    //region Activity

    private void showLoadingIndicator() {
        animatorFor(listView)
                .fadeOut(View.INVISIBLE)
                .start();
        animatorFor(loadingIndicator)
                .fadeIn()
                .start();
    }

    private void hideLoadingIndicator() {
        animatorFor(loadingIndicator)
                .fadeOut(View.INVISIBLE)
                .start();
        animatorFor(listView)
                .fadeIn()
                .start();
    }

    //endregion


    //region Scanning

    public void scan() {
        disconnect(null);
        scannedPeripheralsAdapter.clear();

        SenseAlertDialog includeHighPower = new SenseAlertDialog(this);
        includeHighPower.setTitle("High Power Pre-scan");
        includeHighPower.setMessage("Do you want to include a high power pre-scan? This will add 12 seconds scan time, and disrupt all other Bluetooth services on your phone.");
        includeHighPower.setPositiveButton("No", (dialog, which) -> startScan(false));
        includeHighPower.setNegativeButton("Include", (dialog, which) -> startScan(true));
        includeHighPower.show();
    }

    public void startScan(boolean includeHighPowerPreScan) {
        hardwarePresenter.setWantsHighPowerPreScan(includeHighPowerPreScan);

        PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.setWantsHighPowerPreScan(includeHighPowerPreScan);

        showLoadingIndicator();
        bindAndSubscribe(SensePeripheral.discover(stack, criteria),
                         peripherals -> {
                             scannedPeripheralsAdapter.addAll(peripherals);
                             hideLoadingIndicator();
                         },
                         this::presentError);
    }

    public void presentError(Throwable e) {
        hideLoadingIndicator();
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e)
                .withSupportLink()
                .build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    //endregion


    //region Actions

    public <T> void runSimpleCommand(@NonNull Observable<T> command) {
        showLoadingIndicator();
        bindAndSubscribe(command,
                         ignored -> hideLoadingIndicator(),
                         this::presentError);
    }

    public void disconnect(@Nullable StaticItemAdapter.TextItem item) {
        if (selectedPeripheral != null) {
            selectedPeripheral.disconnect().subscribe(ignored -> {}, Functions.LOG_ERROR);
            hardwarePresenter.setPeripheral(null);
            this.selectedPeripheral = null;
        }

        listView.setAdapter(scannedPeripheralsAdapter);
    }

    public void putIntoPairingMode(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.putIntoPairingMode());
    }

    public void putIntoNormalMode(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.putIntoNormalMode());
    }

    public void factoryReset(@Nullable StaticItemAdapter.TextItem item) {
        SenseAlertDialog alertDialog = new SenseAlertDialog(this);
        alertDialog.setTitle(R.string.dialog_title_factory_reset);
        alertDialog.setMessage("This is a device only factory reset, all paired accounts in API will persist. Use the ‘Devices’ screen to perform a full factory reset.");
        alertDialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> runSimpleCommand(selectedPeripheral.factoryReset()));
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        alertDialog.show();
    }

    public void getWifiNetwork(@Nullable StaticItemAdapter.TextItem item) {
        showLoadingIndicator();
        bindAndSubscribe(selectedPeripheral.getWifiNetwork(),
                network -> {
                    hideLoadingIndicator();
                    MessageDialogFragment dialogFragment = MessageDialogFragment.newInstance("Wifi Network", network.ssid + "\n" + network.connectionState);
                    dialogFragment.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);
                },
                this::presentError);
    }

    public void setWifiNetwork(@Nullable StaticItemAdapter.TextItem item) {
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.putExtra(OnboardingActivity.EXTRA_WIFI_CHANGE_ONLY, true);
        startActivity(intent);
    }

    public void pairPillMode(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.pairPill(apiSessionManager.getAccessToken()));
    }

    public void linkAccount(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.linkAccount(apiSessionManager.getAccessToken()));
    }

    public void pushData(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.pushData());
    }

    public void busyLedAnimation(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.BUSY));
    }

    public void trippyLedAnimation(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.TRIPPY));
    }

    public void stopAnimationWithFade(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.FADE_OUT));
    }

    public void stopAnimationWithoutFade(@Nullable StaticItemAdapter.TextItem item) {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.STOP));
    }

    //endregion


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (selectedPeripheral == null) {
            this.selectedPeripheral = scannedPeripheralsAdapter.getItem(position);

            showLoadingIndicator();
            hardwarePresenter.setPeripheral(selectedPeripheral);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(), status -> {
                if (status == Operation.CONNECTED) {
                    hideLoadingIndicator();
                    listView.setAdapter(peripheralActions);
                }
            }, this::presentError);
        } else {
            peripheralActions.onItemClick(adapterView, view, position, id);
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
