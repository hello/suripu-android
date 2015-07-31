package is.hello.sense.ui.fragments.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.Device;
import is.hello.sense.graph.presenters.DevicesPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.adapter.DevicesAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.widget.util.ListViews;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class DeviceListFragment extends InjectionFragment implements AdapterView.OnItemClickListener, DevicesAdapter.OnPairNewDeviceListener {
    private static final int DEVICE_REQUEST_CODE = 0x14;
    private static final int PAIR_DEVICE_REQUEST_CODE = 0x15;

    @Inject DevicesPresenter devicesPresenter;
    @Inject PreferencesPresenter preferences;

    private ProgressBar loadingIndicator;
    private DevicesAdapter adapter;
    private TextView supportInfoFooter;

    public static void startStandaloneFrom(@NonNull Activity activity) {
        Bundle intentArguments = FragmentNavigationActivity.getArguments(activity.getString(R.string.label_devices),
                DeviceListFragment.class, null);
        Intent intent = new Intent(activity, FragmentNavigationActivity.class);
        intent.putExtras(intentArguments);
        activity.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        devicesPresenter.update();
        addPresenter(devicesPresenter);

        setRetainInstance(true);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.TopView.EVENT_DEVICES, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_device_list, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.adapter = new DevicesAdapter(getActivity(), preferences);
        adapter.setOnPairNewDeviceListener(this);

        Styles.addCardSpacing(listView, Styles.CARD_SPACING_HEADER_AND_FOOTER);
        this.supportInfoFooter = (TextView) inflater.inflate(R.layout.item_device_support_footer, listView, false);
        supportInfoFooter.setVisibility(View.INVISIBLE);
        Styles.initializeSupportFooter(getActivity(), supportInfoFooter);
        ListViews.addFooterView(listView, supportInfoFooter, null, false);
        listView.setAdapter(adapter);


        this.loadingIndicator = (ProgressBar) view.findViewById(R.id.fragment_settings_device_list_progress);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(devicesPresenter.devices, this::bindDevices, this::devicesUnavailable);
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

        if (requestCode == DEVICE_REQUEST_CODE || requestCode == PAIR_DEVICE_REQUEST_CODE) {
            adapter.clear();

            supportInfoFooter.setVisibility(View.INVISIBLE);
            animate(loadingIndicator)
                    .fadeIn()
                    .start();

            devicesPresenter.update();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Device device = (Device) adapterView.getItemAtPosition(position);
        if (device.exists()) {
            DeviceDetailsFragment fragment;
            if (device.getType() == Device.Type.SENSE) {
                fragment = SenseDetailsFragment.newInstance(device);
            } else if (device.getType() == Device.Type.PILL) {
                fragment = PillDetailsFragment.newInstance(device);
            } else {
                return;
            }
            fragment.setTargetFragment(this, DEVICE_REQUEST_CODE);
            ((FragmentNavigation) getActivity()).pushFragmentAllowingStateLoss(fragment, getString(device.getType().nameRes), true);
        }
    }


    public void bindDevices(@NonNull List<Device> devices) {
        adapter.bindDevices(devices);
        loadingIndicator.setVisibility(View.GONE);
        supportInfoFooter.setVisibility(View.VISIBLE);

        WelcomeDialogFragment.showIfNeeded(getActivity(), R.xml.welcome_dialog_pill_color);
    }

    public void devicesUnavailable(Throwable e) {
        loadingIndicator.setVisibility(View.GONE);

        adapter.devicesUnavailable(e);

        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    @Override
    public void onPairNewDevice(@NonNull Device.Type type) {
        Intent intent = new Intent(getActivity(), OnboardingActivity.class);
        switch (type) {
            case SENSE: {
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_QUESTIONS);
                intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                break;
            }

            case PILL: {
                intent.putExtra(OnboardingActivity.EXTRA_START_CHECKPOINT, Constants.ONBOARDING_CHECKPOINT_SENSE);
                intent.putExtra(OnboardingActivity.EXTRA_PAIR_ONLY, true);
                break;
            }

            default: {
                throw new IllegalStateException();
            }
        }
        startActivityForResult(intent, PAIR_DEVICE_REQUEST_CODE);
    }
}
