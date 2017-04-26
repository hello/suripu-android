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
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.activities.PillUpdateActivity;
import is.hello.sense.ui.activities.SenseUpgradeActivity;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.adapter.DevicesAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.ScrollEdge;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
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
    private final LocationPermission locationPermission = new LocationPermission(this);
    private RecyclerView recyclerView;

    public static void startStandaloneFrom(@NonNull Activity activity) {
        final FragmentNavigationActivity.Builder builder =
                new FragmentNavigationActivity.Builder(activity);
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

        this.adapter = new DevicesAdapter(getActivity());
        adapter.setOnItemClickedListener(this);
        adapter.setOnDeviceInteractionListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_settings_device_list, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.fragment_settings_device_list_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     EnumSet.of(ScrollEdge.TOP), FadingEdgesItemDecoration.Style.STRAIGHT));

        recyclerView.setAdapter(adapter);


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
        if (this.recyclerView != null) {
            this.recyclerView.setAdapter(null);
            this.recyclerView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(this.adapter != null) {
            this.adapter.setOnItemClickedListener(null);
            this.adapter.setOnDeviceInteractionListener(null);
            this.adapter = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DEVICE_REQUEST_CODE
                || requestCode == PAIR_DEVICE_REQUEST_CODE
                || requestCode == UPGRADE_SENSE_DEVICE_REQUEST_CODE) {
            adapter.clear();

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
            onPlaceholderInteraction(PlaceholderDevice.Type.SLEEP_PILL);
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }

    public void bindDevices(@NonNull final Devices devices) {
        adapter.bindDevices(shouldUpdateDevices(devices));
        loadingIndicator.setVisibility(View.GONE);
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

    //region DeviceInteractionListener

    @Override
    public void onPillInfoClick() {
        WelcomeDialogFragment.show(getActivity(), R.xml.welcome_dialog_pill_color, true);
    }

    @Override
    public void onPlaceholderInteraction(@NonNull final PlaceholderDevice.Type type) {
        switch (type) {
            case SENSE: {
                final Intent intent = OnboardingActivity.getPairOnlyIntent(getActivity());
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                startActivityForResult(intent, PAIR_DEVICE_REQUEST_CODE);
                break;
            }

            case SLEEP_PILL: {
                if (!locationPermission.isGranted()) {
                    locationPermission.requestPermission();
                    return;
                }
                final Intent intent = OnboardingActivity.getPairOnlyIntent(getActivity());
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_PILL);
                startActivityForResult(intent, PAIR_DEVICE_REQUEST_CODE);
                break;
            }

            case SENSE_WITH_VOICE: {
                startActivityForResult(new Intent(getActivity(), SenseUpgradeActivity.class),
                                       UPGRADE_SENSE_DEVICE_REQUEST_CODE);
                break;
            }

            default: {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public void onUpdateDevice(@NonNull final BaseDevice device) {
        if(device instanceof SleepPillDevice) {
            UserSupport.showUpdatePill(this, device.deviceId);
        }

    }

    @Override
    public void onScrollBy(final int x, final int y) {
        if (recyclerView != null) {
            recyclerView.scrollBy(x, y);
        }
    }

    //endregion
}
