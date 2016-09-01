package is.hello.sense.ui.fragments.updating;

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

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos;
import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import is.hello.sense.R;
import is.hello.sense.presenters.BasePresenter;
import is.hello.sense.presenters.connectwifi.BaseConnectWifiPresenter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.fragments.BasePresenterFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.EditorActionHandler;

import static is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint.sec_type;

//todo rename, remove "Update" after ConnectToWifiFragment is phased out.
public class UpdateConnectToWiFiFragment extends BasePresenterFragment
        implements
        AdapterView.OnItemSelectedListener,
        BaseConnectWifiPresenter.Output {
    public static final String ARG_SCAN_RESULT = UpdateConnectToWiFiFragment.class.getName() + ".ARG_SCAN_RESULT";

    private static final int ERROR_REQUEST_CODE = 0x30;

    private EditText networkName;
    private LabelEditText networkPassword;
    private Spinner networkSecurity;
    private Button continueButton;
    private View view;

    private OnboardingToolbar toolbar;

    @Inject
    BaseConnectWifiPresenter wifiPresenter;


    //region Lifecycle


    @Override
    protected BasePresenter getPresenter() {
        return wifiPresenter;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiPresenter.setNetwork((wifi_endpoint) getArguments().getSerializable(ARG_SCAN_RESULT));
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_connect_to_wifi, container, false);

        this.networkName = (EditText) view.findViewById(R.id.fragment_connect_to_wifi_network);
        this.networkPassword = (LabelEditText) view.findViewById(R.id.fragment_connect_to_wifi_password);
        networkPassword.setOnEditorActionListener(new EditorActionHandler(wifiPresenter::sendWifiCredentials));

        this.continueButton = (Button) view.findViewById(R.id.fragment_connect_to_wifi_continue);
        Views.setSafeOnClickListener(continueButton, ignored -> wifiPresenter.sendWifiCredentials());

        if (getActivity().getActionBar() != null) {
            setHasOptionsMenu(true);
        } else {
            this.toolbar = OnboardingToolbar.of(this, view);
            this.toolbar.setWantsBackButton(true)
                        .setOnHelpClickListener(wifiPresenter::onHelpClick);
            //todo add back support options after refactor
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkSecurity != null) {
            networkSecurity.setAdapter(null);
            networkSecurity.setOnItemSelectedListener(null);
            networkSecurity = null;
        }
        if (toolbar != null) {
            toolbar.onDestroyView();
            toolbar = null;
        }
        networkName = null;
        if (networkPassword != null) {
            networkPassword.setOnEditorActionListener(null);
        }
        networkPassword = null;
        if (continueButton != null) {
            continueButton.setOnClickListener(null);
        }
        continueButton = null;
        view = null;
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
                wifiPresenter.onHelpClick(null);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }


    //endregion

    @Override
    public wifi_endpoint.sec_type getNetworkSecurityType() {
        if (networkSecurity == null) {
            return null;
        }
        return (wifi_endpoint.sec_type) networkSecurity.getSelectedItem();
    }

    @Override
    public void updateView(@Nullable final wifi_endpoint network) {
        if (view == null) {
            return;
        }
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
    }

    //region Password Field


    @Override
    public String getNetworkName() {
        if (networkName == null) {
            return null;
        }
        return networkName.getText().toString();
    }

    @Override
    public String getNetworkPassword() {
        if (networkPassword == null) {
            return null;
        }
        return networkPassword.getInputText();
    }

    @Override
    public void setNetworkPassword(final int visibility,
                                   final boolean requestFocus,
                                   final boolean clearInput) {
        if (networkPassword == null) {
            return;
        }
        networkPassword.setVisibility(visibility);
        if (clearInput) {
            networkPassword.setInputText(null);
        }
        if (requestFocus) {
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

    private static class SecurityTypeAdapter extends ArrayAdapter<sec_type> {
        private SecurityTypeAdapter(final Context context) {
            super(context, R.layout.item_sec_type, sec_type.values());
        }

        private
        @StringRes
        int getTitle(final int position) {
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
