package is.hello.sense.ui.fragments.onboarding;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

public class OnboardingSignInFragment extends InjectionFragment {
    @Inject ApiSessionManager apiSessionManager;
    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferencesPresenter;

    private EditText emailText;
    private EditText passwordText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SIGN_IN, null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.fragment_onboarding_sign_in, container, false);

        this.emailText = (EditText) view.findViewById(R.id.fragment_onboarding_email);
        this.passwordText = (EditText) view.findViewById(R.id.fragment_onboarding_password);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::signIn));

        Button signIn = (Button) view.findViewById(R.id.fragment_onboarding_sign_in_go);
        Views.setSafeOnClickListener(signIn, ignored -> signIn());

        Button forgotPassword = (Button) view.findViewById(R.id.fragment_onboarding_sign_in_forgot_password);
        forgotPassword.setOnClickListener(this::forgotPassword);
        view.removeView(forgotPassword);

        OnboardingToolbar.of(this, view)
                .setWantsBackButton(true)
                .setWantsHelpButton(true)
                .replaceHelpButton(forgotPassword);

        return view;
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void signIn() {
        String email = this.emailText.getText().toString().trim();
        this.emailText.setText(email);

        String password = this.passwordText.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_account_incomplete_credentials));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            return;
        }

        LoadingDialogFragment.show(getFragmentManager(), getString(R.string.dialog_loading_message), true);

        OAuthCredentials credentials = new OAuthCredentials(email, password);
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            apiSessionManager.setSession(session);
            preferencesPresenter.pullAccountPreferences().subscribe();
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_IN, null);

            getOnboardingActivity().showHomeActivity();
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            if (ApiException.statusEquals(error, 401)) {
                ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_account_invalid_credentials));
                dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            } else {
                ErrorDialogFragment.presentError(getFragmentManager(), error);
            }
        });
    }

    public void forgotPassword(@NonNull View sender) {
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.FORGOT_PASSWORD_URL));
    }
}
