package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.DynamicApiEndpoint;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.functional.Functions;
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
import rx.Observable;

public class OnboardingSignInFragment extends InjectionFragment {
    @Inject ApiEndpoint apiEndpoint;
    @Inject ApiSessionManager apiSessionManager;
    @Inject ApiService apiService;
    @Inject PreferencesPresenter preferences;

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

        if (BuildConfig.DEBUG) {
            LinearLayout content = (LinearLayout) view.findViewById(R.id.fragment_onboarding_sign_in_content);

            Button selectHost = new Button(getActivity());
            selectHost.setTextAppearance(getActivity(), R.style.AppTheme_Button_Borderless_Accent_Bounded);
            selectHost.setBackgroundResource(R.drawable.selectable_dark_bounded);
            selectHost.setGravity(Gravity.CENTER);
            Observable<String> apiUrl = preferences.observableString(DynamicApiEndpoint.PREF_API_ENDPOINT_OVERRIDE, apiEndpoint.getUrl());
            bindAndSubscribe(apiUrl, selectHost::setText, Functions.LOG_ERROR);

            int padding = getResources().getDimensionPixelSize(R.dimen.gap_small);
            selectHost.setPadding(padding, padding, padding, padding);

            Views.setSafeOnClickListener(selectHost, ignored -> {
                try {
                    startActivity(new Intent(getActivity(), Class.forName("is.hello.sense.debug.EnvironmentActivity")));
                } catch (ClassNotFoundException e) {
                    Log.e(getClass().getSimpleName(), "Could not find environment activity", e);
                }
            });

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.gap_small);
            content.addView(selectHost, layoutParams);
        }

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

        LoadingDialogFragment.show(getFragmentManager(),
                getString(R.string.dialog_loading_message),
                LoadingDialogFragment.OPAQUE_BACKGROUND);

        OAuthCredentials credentials = new OAuthCredentials(apiEndpoint, email, password);
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            apiSessionManager.setSession(session);
            preferences.pullAccountPreferences().subscribe();

            String accountId = session.getAccountId();
            Analytics.trackSignIn(accountId);

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
