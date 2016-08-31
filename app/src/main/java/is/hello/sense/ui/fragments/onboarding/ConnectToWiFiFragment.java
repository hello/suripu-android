package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.segment.analytics.Properties;

import javax.inject.Inject;

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import is.hello.sense.R;
import is.hello.sense.presenters.ConnectWifiPresenter;
import is.hello.sense.presenters.PairSensePresenter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;
@Deprecated
public class ConnectToWiFiFragment extends BasePresenterFragment
        implements AdapterView.OnItemSelectedListener, ConnectWifiPresenter.Output {
    public static final String ARG_SEND_ACCESS_TOKEN = ConnectToWiFiFragment.class.getName() + ".ARG_SEND_ACCESS_TOKEN";
    public static final String ARG_SCAN_RESULT = ConnectToWiFiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;
    private static final String HAS_SENT_ACCESS_TOKEN_KEY = "hasSentAccessToken";

    private boolean sendAccessToken;

    private EditText networkName;
    private LabelEditText networkPassword;
    private Spinner networkSecurity;
    private Button continueButton;

    private @Nullable wifi_endpoint network;

    private boolean hasSentAccessToken = false;
    private OnboardingToolbar toolbar;

    @Inject
    ConnectWifiPresenter wifiPresenter;

    @Inject
    PairSensePresenter presenter;

    //region Lifecycle


    @Override
    public void onInjected() {
        addScopedPresenter(presenter);
        addScopedPresenter(wifiPresenter);
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.sendAccessToken = getArguments().getBoolean(ARG_SEND_ACCESS_TOKEN, true);

        this.network = (wifi_endpoint) getArguments().getSerializable(ARG_SCAN_RESULT);
        if (savedInstanceState != null) {
            this.hasSentAccessToken = savedInstanceState.getBoolean(HAS_SENT_ACCESS_TOKEN_KEY, false);
        }
        sendOnCreateAnalytics();

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_connect_to_wifi, container, false);

        this.networkName = (EditText) view.findViewById(R.id.fragment_connect_to_wifi_network);
        this.networkPassword = (LabelEditText) view.findViewById(R.id.fragment_connect_to_wifi_password);
        networkPassword.setOnEditorActionListener(new EditorActionHandler(wifiPresenter::sendWifiCredentials));

        this.continueButton = (Button) view.findViewById(R.id.fragment_connect_to_wifi_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> wifiPresenter.sendWifiCredentials());

        final TextView title = (TextView) view.findViewById(R.id.fragment_connect_to_wifi_title);
        final TextView networkInfo = (TextView) view.findViewById(R.id.fragment_connect_to_wifi_info);
        final ViewGroup otherContainer = (ViewGroup) view.findViewById(R.id.fragment_connect_to_wifi_other_container);

        if (network != null) {
            networkName.setText(network.getSsid());
            wifiPresenter.updatePasswordField();

            final SpannableStringBuilder networkInfoBuilder = new SpannableStringBuilder();
            networkInfoBuilder.append(getString(R.string.label_wifi_network_name));
            final int start = networkInfoBuilder.length();
            networkInfoBuilder.append(network.getSsid());
            networkInfoBuilder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.text_dark)),
                    start, networkInfoBuilder.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            networkInfo.setText(networkInfoBuilder);

            networkInfo.setVisibility(View.VISIBLE);
            otherContainer.setVisibility(View.GONE);

            if (network.getSecurityType() == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
                title.setText(R.string.title_sign_into_wifi_selection_open);
            } else {
                title.setText(R.string.title_sign_into_wifi_selection);
            }
        } else {
            networkSecurity = (Spinner) otherContainer.findViewById(R.id.fragment_connect_to_wifi_security);
            networkSecurity.setAdapter(new SecurityTypeAdapter(getActivity()));
            networkSecurity.setSelection(sec_type.SL_SCAN_SEC_TYPE_WPA2_VALUE);
            networkSecurity.setOnItemSelectedListener(this);

            networkName.requestFocus();

            networkInfo.setVisibility(View.GONE);
            otherContainer.setVisibility(View.VISIBLE);

            title.setText(R.string.title_sign_into_wifi_other);
        }

        if (getActivity().getActionBar() != null) {
            setHasOptionsMenu(true);
        } else {
            this.toolbar = OnboardingToolbar.of(this, view);
            this.toolbar.setWantsBackButton(true)
                   .setOnHelpClickListener(ignored -> UserSupport.showForHelpStep(getActivity(), UserSupport.HelpStep.SIGN_INTO_WIFI));
                    //todo add back support options after refactor
        }

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (network != null && network.getSecurityType() == sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            wifiPresenter.sendWifiCredentials();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(HAS_SENT_ACCESS_TOKEN_KEY, hasSentAccessToken);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(networkSecurity != null) {
            networkSecurity.setAdapter(null);
            networkSecurity.setOnItemSelectedListener(null);
            networkSecurity = null;
        }
        if(toolbar != null){
            toolbar.onDestroyView();
            toolbar = null;
        }
        networkName = null;
        networkPassword.setOnEditorActionListener(null);
        networkPassword = null;
        continueButton.setOnClickListener(null);
        continueButton = null;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ERROR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            wifiPresenter.sendWifiCredentials();
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.help, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help: {
                UserSupport.showForHelpStep(getActivity(),
                                            UserSupport.HelpStep.SIGN_INTO_WIFI);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    //endregion


    //region Password Field
    @Override
    public sec_type getSecurityType() {
        if (network != null) {
            return network.getSecurityType();
        } else {
            return (sec_type) networkSecurity.getSelectedItem();
        }
    }

    @Override
    public String getNetworkName() {
        return networkName.getText().toString();
    }

    @Override
    public String getNetworkPassword() {
        return networkPassword.getInputText();
    }

    @Override
    public void setNetworkPassword(final int visibility,
                                   final boolean requestFocus,
                                   final boolean clearInput) {
        networkPassword.setVisibility(visibility);
        if(clearInput) {
            networkPassword.setInputText(null);
        }
        if(requestFocus){
            networkPassword.requestFocus();
        } else {
            networkPassword.clearFocus();
        }
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
        wifiPresenter.updatePasswordField();
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {
        wifiPresenter.updatePasswordField();
    }

    //endregion
    @Override
    public void sendAccessToken() {
        if (!sendAccessToken) {
            presenter.finishUpOperations();
        } else {
            presenter.checkLinkedAccount();
        }
    }

    public void sendOnCreateAnalytics(){
        final boolean hasNetwork = network != null;
        final Properties properties = Analytics.createProperties(Analytics.Onboarding.PROP_WIFI_IS_OTHER,
                                                                 !hasNetwork);
        final int rssi = hasNetwork ? network.getRssi() : 0;
        properties.put(Analytics.Onboarding.PROP_WIFI_RSSI, rssi);
        Analytics.trackEvent(wifiPresenter.getOnCreateAnalyticsEvent(), properties);
    }

    @Override
    public void presentWifiValidationErrorDialog(final Throwable e,
                                                 final String operation,
                                                 final Uri supportUri,
                                                 @StringRes final int actionStringRes) {
        final ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(e, getActivity())
                .withOperation(operation);

        final Uri uri = UserSupport.DeviceIssue.SENSE_ASCII_WEP.getUri();
        final Intent intent = UserSupport.createViewUriIntent(getActivity(), uri);
        errorDialogBuilder.withAction(intent, R.string.action_support);

        final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    @Override
    public void presentLinkedAccountErrorDialog(final Throwable e,
                                                final String operation,
                                                @StringRes final int titleRes) {
        final ErrorDialogFragment errorDialogFragment =
                new ErrorDialogFragment.Builder(e, getActivity())
                        .withOperation(operation)
                        .withTitle(R.string.failed_to_link_account)
                        .withSupportLink()
                        .build();

        errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
    }

    private static class SecurityTypeAdapter extends ArrayAdapter<sec_type> {
        private SecurityTypeAdapter(final Context context) {
            super(context, R.layout.item_sec_type, sec_type.values());
        }

        private @StringRes int getTitle(final int position) {
            switch (getItem(position)) {
                case SL_SCAN_SEC_TYPE_OPEN:
                    return R.string.sec_type_open;

                case SL_SCAN_SEC_TYPE_WEP:
                    return R.string.sec_type_wep;

                case SL_SCAN_SEC_TYPE_WPA:
                    return R.string.sec_type_wpa;

                case SL_SCAN_SEC_TYPE_WPA2:
                    return R.string.sec_type_wpa2;

                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final TextView text = (TextView) super.getView(position, convertView, parent);
            text.setText(getTitle(position));
            return text;
        }

        @Override
        public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
            final TextView text = (TextView) super.getDropDownView(position, convertView, parent);
            text.setText(getTitle(position));
            return text;
        }
    }

}
