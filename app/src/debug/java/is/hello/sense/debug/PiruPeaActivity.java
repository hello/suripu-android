package is.hello.sense.debug;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import is.hello.commonsense.bluetooth.SensePeripheral;
import is.hello.commonsense.bluetooth.model.SenseLedAnimation;
import is.hello.commonsense.util.ConnectProgress;
import is.hello.sense.R;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.interactors.HardwareInteractor;
import is.hello.sense.settings.SettingsPairSenseModule;
import is.hello.sense.settings.SettingsWifiModule;
import is.hello.sense.ui.activities.ScopedInjectionActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.MessageDialogFragment;
import is.hello.sense.ui.fragments.updating.SelectWifiNetworkFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import rx.Observable;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class PiruPeaActivity extends ScopedInjectionActivity implements ArrayRecyclerAdapter.OnItemClickedListener<SensePeripheral> {
    @Inject BluetoothStack stack;
    @Inject ApiSessionManager apiSessionManager;
    @Inject
    HardwareInteractor hardwarePresenter;

    private SensePeripheral selectedPeripheral;

    private PeripheralAdapter scannedPeripheralsAdapter;
    private SettingsRecyclerAdapter peripheralActions;
    private RecyclerView recyclerView;
    private ProgressBar loadingIndicator;


    @Override
    protected List<Object> getModules() {
        return Arrays.asList(new SettingsWifiModule(), new SettingsPairSenseModule());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.static_recycler);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.loadingIndicator = (ProgressBar) findViewById(R.id.static_recycler_view_loading);

        this.recyclerView = (RecyclerView) findViewById(R.id.static_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        this.scannedPeripheralsAdapter = new PeripheralAdapter(this);
        scannedPeripheralsAdapter.setOnItemClickedListener(this);
        recyclerView.setAdapter(scannedPeripheralsAdapter);

        this.peripheralActions = new SettingsRecyclerAdapter(this);
        peripheralActions.setWantsDividers(false);

        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Disconnect", this::disconnect));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Pairing Mode", this::putIntoPairingMode));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Normal Mode", this::putIntoNormalMode));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Device Factory Reset", this::factoryReset));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Get WiFi Network", this::getWifiNetwork));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Set WiFi Network", this::setWifiNetwork));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Pair Pill Mode", this::pairPillMode));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Link Account", this::linkAccount));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Push Data", this::pushData));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Busy LEDs", this::busyLedAnimation));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Trippy LEDs", this::trippyLedAnimation));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Fade Out LEDs", this::stopAnimationWithFade));
        peripheralActions.add(new SettingsRecyclerAdapter.DetailItem("Turn Off LEDs", this::stopAnimationWithoutFade));

        IntentFilter filter = new IntentFilter(HardwareInteractor.ACTION_CONNECTION_LOST);
        Observable<Intent> onConnectionLost = Rx.fromLocalBroadcast(this, filter);
        bindAndSubscribe(onConnectionLost,
                         intent -> disconnect(),
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
        animatorFor(recyclerView)
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
        animatorFor(recyclerView)
                .fadeIn()
                .start();
    }

    //endregion


    //region Scanning

    public void scan() {
        disconnect();
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
        ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder(e, this)
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

    public void disconnect() {
        if (selectedPeripheral != null) {
            selectedPeripheral.disconnect().subscribe(ignored -> {}, Functions.LOG_ERROR);
            hardwarePresenter.setPeripheral(null);
            this.selectedPeripheral = null;
        }

        recyclerView.swapAdapter(scannedPeripheralsAdapter, true);
    }

    public void putIntoPairingMode() {
        runSimpleCommand(selectedPeripheral.putIntoPairingMode());
    }

    public void putIntoNormalMode() {
        runSimpleCommand(selectedPeripheral.putIntoNormalMode());
    }

    public void factoryReset() {
        SenseAlertDialog alertDialog = new SenseAlertDialog(this);
        alertDialog.setTitle(R.string.dialog_title_factory_reset);
        alertDialog.setMessage("This is a device only factory reset, all paired accounts in API will persist. Use the ‘Devices’ screen to perform a full factory reset.");
        alertDialog.setPositiveButton(R.string.action_factory_reset, (d, which) -> runSimpleCommand(selectedPeripheral.factoryReset()));
        alertDialog.setNegativeButton(android.R.string.cancel, null);
        alertDialog.setButtonDestructive(DialogInterface.BUTTON_POSITIVE, true);
        alertDialog.show();
    }

    public void getWifiNetwork() {
        showLoadingIndicator();
        bindAndSubscribe(selectedPeripheral.getWifiNetwork(),
                         network -> {
                             hideLoadingIndicator();
                             MessageDialogFragment dialogFragment = MessageDialogFragment.newInstance("Wifi Network", network.ssid + "\n" + network.connectionState);
                             dialogFragment.showAllowingStateLoss(getFragmentManager(), MessageDialogFragment.TAG);
                         },
                         this::presentError);
    }

    public void setWifiNetwork() {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(this);
        builder.setDefaultTitle(R.string.title_edit_wifi);
        builder.setFragmentClass(SelectWifiNetworkFragment.class);
        builder.setWindowBackgroundColor(getResources().getColor(R.color.background_onboarding));
        builder.setOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        startActivity(builder.toIntent());
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
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.BUSY));
    }

    public void trippyLedAnimation() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.TRIPPY));
    }

    public void stopAnimationWithFade() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.FADE_OUT));
    }

    public void stopAnimationWithoutFade() {
        runSimpleCommand(selectedPeripheral.runLedAnimation(SenseLedAnimation.STOP));
    }

    //endregion


    @Override
    public void onItemClicked(int position, SensePeripheral item) {
        if (selectedPeripheral == null) {
            this.selectedPeripheral = item;

            showLoadingIndicator();
            hardwarePresenter.setPeripheral(selectedPeripheral);
            bindAndSubscribe(hardwarePresenter.connectToPeripheral(),
                             status -> {
                                 if (status == ConnectProgress.CONNECTED) {
                                     hideLoadingIndicator();
                                     recyclerView.swapAdapter(peripheralActions, true);
                                 }
                             },
                             this::presentError);
        }
    }


    static class PeripheralAdapter extends ArrayRecyclerAdapter<SensePeripheral,
            PeripheralAdapter.ViewHolder> {
        private final LayoutInflater inflater;

        PeripheralAdapter(@NonNull Context context) {
            super(new ArrayList<>());

            this.inflater = LayoutInflater.from(context);
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = inflater.inflate(R.layout.item_simple_text, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final SensePeripheral peripheral = getItem(position);
            holder.text.setText(peripheral.getName() + " - " + peripheral.getAddress());
        }


        class ViewHolder extends ArrayRecyclerAdapter.ViewHolder {
            final TextView text;

            ViewHolder(@NonNull View itemView) {
                super(itemView);

                this.text = (TextView) itemView.findViewById(R.id.item_simple_text);
                itemView.setOnClickListener(this);
            }
        }
    }
}
