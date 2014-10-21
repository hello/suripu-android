package is.hello.sense.ui.fragments.onboarding;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.DevicePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;

public class OnboardingSignIntoWifiFragment extends InjectionFragment {
    private static final String ARG_SCAN_RESULT = OnboardingSignIntoWifiFragment.class.getName() + ".ARG_SCAN_RESULT";

    @Inject DevicePresenter devicePresenter;

    private EditText networkName;
    private EditText networkPassword;

    private ScanResult network;

    public static OnboardingSignIntoWifiFragment newInstance(@Nullable ScanResult network) {
        OnboardingSignIntoWifiFragment fragment = new OnboardingSignIntoWifiFragment();

        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_SCAN_RESULT, network);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.network = getArguments().getParcelable(ARG_SCAN_RESULT);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sign_into_wifi, container, false);

        this.networkName = (EditText) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_network);
        this.networkPassword = (EditText) view.findViewById(R.id.fragment_onboarding_sign_into_wifi_password);
        networkPassword.setOnEditorActionListener(this::onPasswordEditorAction);

        if (network != null) {
            this.networkName.setText(network.SSID);
            this.networkPassword.requestFocus();
        }

        return view;
    }


    private void beginSettingWifi() {
        ((OnboardingActivity) getActivity()).beginBlockingWork(R.string.title_connecting_network);
    }

    private void finishedSettingWifi() {
        OnboardingActivity activity = (OnboardingActivity) getActivity();
        activity.finishBlockingWork();
        activity.showWelcome(); //TODO: Sense pairing starts here.
    }

    private void sendWifiCredentials() {
        String networkName = this.networkName.getText().toString();
        String password = this.networkPassword.getText().toString();

        if (TextUtils.isEmpty(networkName) || TextUtils.isEmpty(password)) {
            return;
        }

        beginSettingWifi();

        bindAndSubscribe(devicePresenter.sendWifiCredentials(networkName, networkName, password), ignored -> sendAccessToken(), this::presentError);
    }

    private void sendAccessToken() {
        bindAndSubscribe(devicePresenter.linkAccount(), ignored -> finishedSettingWifi(), this::presentError);
    }


    public void presentError(Throwable e) {
        ((OnboardingActivity) getActivity()).finishBlockingWork();
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    public boolean onPasswordEditorAction(TextView sender, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            sendWifiCredentials();

            return true;
        }

        return false;
    }
}
