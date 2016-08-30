package is.hello.sense.ui.fragments.updating;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Collection;

import javax.inject.Inject;

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import is.hello.sense.R;
import is.hello.sense.presenters.selectwifinetwork.BaseSelectWifiNetworkPresenter;
import is.hello.sense.ui.adapter.WifiNetworkAdapter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

//todo rename, remove "Update" after ConnectToWifiFragment is phased out.
public class UpdateSelectWifiNetworkFragment extends BasePresenterFragment
        implements AdapterView.OnItemClickListener, BaseSelectWifiNetworkPresenter.Output {
    public static final String ARG_SEND_ACCESS_TOKEN = UpdateSelectWifiNetworkFragment.class.getName() + ".ARG_SEND_ACCESS_TOKEN";

    private boolean sendAccessToken;

    private WifiNetworkAdapter networkAdapter;

    private TextView subheading;
    private TextView scanningIndicatorLabel;
    private ProgressBar scanningIndicator;
    private ListView listView;
    private Button rescanButton;
    private OnboardingToolbar toolbar;
    private View otherNetworkView;

    @Inject
    BaseSelectWifiNetworkPresenter presenter;

    //region Lifecycle


    @Override
    public void onInjected() {
        addScopedPresenter(presenter);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.networkAdapter = new WifiNetworkAdapter(getActivity());
        if (getArguments() != null && getArguments().containsKey(ARG_SEND_ACCESS_TOKEN)) {
            this.sendAccessToken = getArguments().getBoolean(ARG_SEND_ACCESS_TOKEN, true);
        } else {
            this.sendAccessToken = false;

        }

        Analytics.trackEvent(presenter.getOnCreateAnalyticsEvent(), null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_select_wifi_network, container, false);

        this.subheading = (TextView) view.findViewById(R.id.fragment_select_wifi_subheading);

        this.listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        this.otherNetworkView = inflater.inflate(R.layout.item_wifi_network, listView, false);
        final WifiNetworkAdapter.ViewHolder holder = new WifiNetworkAdapter.ViewHolder(otherNetworkView);
        holder.locked.setVisibility(View.GONE);
        holder.strength.setVisibility(View.INVISIBLE);
        holder.name.setText(R.string.wifi_other_network);
        listView.addFooterView(otherNetworkView, null, true);

        listView.setAdapter(networkAdapter);

        this.scanningIndicatorLabel = (TextView) view.findViewById(R.id.fragment_select_wifi_progress_label);
        this.scanningIndicator = (ProgressBar) view.findViewById(R.id.fragment_select_wifi_progress);

        this.rescanButton = (Button) view.findViewById(R.id.fragment_select_wifi_rescan);
        rescanButton.setEnabled(false);
        Views.setSafeOnClickListener(rescanButton, ignored -> {
            sendOnRescanAnalytics();
            presenter.rescan(true);
        });

        if (getActivity().getActionBar() != null) {
            final TextView heading = (TextView) view.findViewById(R.id.fragment_select_wifi_heading);
            heading.setVisibility(View.GONE);
            subheading.setVisibility(View.GONE);

            setHasOptionsMenu(true);
        } else {
            this.toolbar = OnboardingToolbar.of(this, view);
            toolbar.setWantsBackButton(false)
                   .setOnHelpClickListener(ignored -> UserSupport.showForHelpStep(getActivity(),
                                                                                  UserSupport.HelpStep.WIFI_SCAN));
            setHasOptionsMenu(false);
        }

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (networkAdapter.isEmpty()) {
            sendOnScanAnalytics();
            presenter.rescan(false);
        } else {
            showRescanOption();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (toolbar != null) {
            toolbar.onDestroyView();
            toolbar = null;
        }
        subheading = null;
        scanningIndicator = null;
        scanningIndicatorLabel = null;
        if (listView != null) {
            listView.setOnItemClickListener(null);
            listView.removeFooterView(otherNetworkView);
            listView.setAdapter(null);
            listView = null;
        }
        otherNetworkView = null;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help: {
                UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.WIFI_SCAN);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    @Override
    public void onItemClick(final AdapterView<?> adapterView,
                            final View view,
                            final int position,
                            final long id) {
        //todo cleanup manual routing here
        final wifi_endpoint network = (wifi_endpoint) adapterView.getItemAtPosition(position);
        final UpdateConnectToWiFiFragment nextFragment = new UpdateConnectToWiFiFragment();
        final Bundle arguments = new Bundle();
        arguments.putSerializable(UpdateConnectToWiFiFragment.ARG_SCAN_RESULT, network);
        arguments.putBoolean(UpdateConnectToWiFiFragment.ARG_SEND_ACCESS_TOKEN, sendAccessToken);
        nextFragment.setArguments(arguments);
        getFragmentNavigation().pushFragment(nextFragment, getString(R.string.title_edit_wifi), true);
    }

    @Override
    public void showScanning() {
        scanningIndicatorLabel.setVisibility(View.VISIBLE);
        scanningIndicator.setVisibility(View.VISIBLE);
        if (subheading.getVisibility() != View.GONE) {
            subheading.setVisibility(View.INVISIBLE);
        }
        listView.setVisibility(View.INVISIBLE);
        rescanButton.setVisibility(View.INVISIBLE);
        rescanButton.setEnabled(false);
        networkAdapter.clear();
    }

    @Override
    public void showRescanOption() {
        scanningIndicatorLabel.setVisibility(View.GONE);
        scanningIndicator.setVisibility(View.GONE);
        if (subheading.getVisibility() != View.GONE) {
            subheading.setVisibility(View.VISIBLE);
        }
        listView.setVisibility(View.VISIBLE);
        rescanButton.setVisibility(View.VISIBLE);
        rescanButton.setEnabled(true);
    }

    @Override
    public void bindScanResults(@NonNull final Collection<wifi_endpoint> scanResults) {
        networkAdapter.clear();
        networkAdapter.addAll(scanResults);
    }

    @Override
    public void presentErrorDialog(final Throwable e, final String operation) {
        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity())
                .withOperation(operation)
                .withSupportLink();

        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    public void sendOnScanAnalytics() {
        Analytics.trackEvent(presenter.getOnScanAnalyticsEvent(), null);
    }

    private void sendOnRescanAnalytics() {
        Analytics.trackEvent(presenter.getOnRescanAnalyticsEvent(), null);
    }
}
