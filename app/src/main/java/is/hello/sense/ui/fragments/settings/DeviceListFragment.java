package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.EnumSet;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.BaseDevice;
import is.hello.sense.api.model.Devices;
import is.hello.sense.api.model.PlaceholderDevice;
import is.hello.sense.api.model.SenseDevice;
import is.hello.sense.api.model.SleepPillDevice;
import is.hello.sense.interactors.DeviceIssuesInteractor;
import is.hello.sense.interactors.DevicesInteractor;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.activities.HardwareFragmentActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.activities.SenseUpgradeActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.DevicesAdapter;
import is.hello.sense.ui.adapter.FooterRecyclerAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class DeviceListFragment extends InjectionFragment
        implements DevicesAdapter.OnDeviceInteractionListener,
        ArrayRecyclerAdapter.OnItemClickedListener<BaseDevice> {
    private static final int DEVICE_REQUEST_CODE = 0x14;
    private static final int PAIR_DEVICE_REQUEST_CODE = 0x15;
    private static final int UPGRADE_SENSE_DEVICE_REQUEST_CODE = 0x16;

    @Inject
    DevicesInteractor devicesPresenter;
    @Inject
    DeviceIssuesInteractor deviceIssuesPresenter;

    private ProgressBar loadingIndicator;
    private DevicesAdapter adapter;
    private TextView supportInfoFooter;
    private final LocationPermission locationPermission = new LocationPermission(this);

    public static void startStandaloneFrom(@NonNull Activity activity) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(activity, HardwareFragmentActivity.class);
        builder.setDefaultTitle(R.string.label_devices);
        builder.setFragmentClass(DeviceListFragment.class);
        activity.startActivity(builder.toIntent());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPresenter(devicesPresenter);

        setRetainInstance(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Backside.EVENT_DEVICES, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_settings_device_list, container, false);

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.fragment_settings_device_list_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(resources));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     EnumSet.of(ScrollEdge.TOP), FadingEdgesItemDecoration.Style.STRAIGHT));

        this.adapter = new DevicesAdapter(getActivity());
        adapter.setOnItemClickedListener(this);
        adapter.setOnDeviceInteractionListener(this);

        this.supportInfoFooter = (TextView) inflater.inflate(R.layout.item_device_support_footer, recyclerView, false);
        supportInfoFooter.setVisibility(View.INVISIBLE);
        Styles.initializeSupportFooter(getActivity(), supportInfoFooter);

        recyclerView.setAdapter(new FooterRecyclerAdapter(adapter)
                                        .addFooter(supportInfoFooter)
                                        .setFlattenChanges(true));


        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_settings_device_list_progress);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(devicesPresenter.devices, this::bindDevices, this::devicesUnavailable);
    }

    @Override
    public void onResume() {
        super.onResume();

        devicesPresenter.update();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.loadingIndicator = null;
        this.adapter = null;
        this.supportInfoFooter = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DEVICE_REQUEST_CODE
                || requestCode == PAIR_DEVICE_REQUEST_CODE
                || requestCode == UPGRADE_SENSE_DEVICE_REQUEST_CODE) {
            adapter.clear();

            supportInfoFooter.setVisibility(View.INVISIBLE);
            animatorFor(loadingIndicator)
                    .fadeIn()
                    .start();

            devicesPresenter.update();
        } else if( requestCode == PillUpdateActivity.REQUEST_CODE && resultCode == Activity.RESULT_OK){
            // can do something here but not needed because onResume will call update devices
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            onPairNewDevice(PlaceholderDevice.Type.SLEEP_PILL);
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }

    public void bindDevices(@NonNull final Devices devices) {
        adapter.bindDevices(shouldUpdateDevices(devices));
        loadingIndicator.setVisibility(View.GONE);
        supportInfoFooter.setVisibility(View.VISIBLE);
    }

    private Devices shouldUpdateDevices(final Devices devices) {
        final ArrayList<SenseDevice> updateSenses = devices.senses;
        final ArrayList<SleepPillDevice> updatePills = new ArrayList<>();

        for(final SleepPillDevice device : devices.sleepPills){
            //todo will remove method once no longer needed to suppress on client side
            device.setShouldUpdateOverride(
                    !device.hasLowBattery()
                            && device.shouldUpdate()
                            && deviceIssuesPresenter.shouldShowUpdateFirmwareAction(device.deviceId));
            updatePills.add(device);
        }

        return new Devices(updateSenses, updatePills);
    }

    public void devicesUnavailable(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        adapter.devicesUnavailable(e);

        ErrorDialogFragment.presentError(getActivity(), e);
    }


    @Override
    public void onItemClicked(final int position, final BaseDevice device) {
        final DeviceDetailsFragment fragment;
        final String title = getString(device.getDisplayTitleRes());
        if (device instanceof SenseDevice) {
            fragment = SenseDetailsFragment.newInstance((SenseDevice) device);
        } else if (device instanceof SleepPillDevice) {
            fragment = PillDetailsFragment.newInstance((SleepPillDevice) device);
        } else {
            return;
        }
        fragment.setTargetFragment(this, DEVICE_REQUEST_CODE);

        final FragmentNavigation fragmentNavigation = getFragmentNavigation();
        fragmentNavigation.pushFragmentAllowingStateLoss(fragment,
                                                         title,
                                                         true);
    }

    @Override
    public void onPairNewDevice(@NonNull final PlaceholderDevice.Type type) {
        final Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        switch (type) {
            case SENSE: {
                //todo make separate activity to handle pairing only
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                break;
            }

            case SLEEP_PILL: {
                if (!locationPermission.isGranted()) {
                    locationPermission.requestPermission();
                    return;
                }
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_PILL);
                intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                break;
            }

            default: {
                throw new IllegalStateException();
            }
        }
        startActivityForResult(intent, PAIR_DEVICE_REQUEST_CODE);
    }

    @Override
    public void onUpdateDevice(@NonNull final BaseDevice device) {
        if(device instanceof SleepPillDevice) {
            UserSupport.showUpdatePill(this, device.deviceId);
        } else if(device instanceof SenseDevice){
            startActivityForResult(new Intent(getActivity(), SenseUpgradeActivity.class), UPGRADE_SENSE_DEVICE_REQUEST_CODE);
        }

    }
}
