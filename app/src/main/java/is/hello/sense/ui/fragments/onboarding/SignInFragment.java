package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import javax.inject.Inject;

import is.hello.commonsense.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.DynamicApiEndpoint;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.StatusBarColorProvider;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;
import rx.Observable;

public class SignInFragment extends InjectionFragment
        implements StatusBarColorProvider, TextWatcher {
    @Inject ApiEndpoint apiEndpoint;
    @Inject ApiSessionManager apiSessionManager;
    @Inject ApiService apiService;
    @Inject AccountPresenter accountPresenter;
    @Inject PreferencesPresenter preferences;

    private EditText emailText;
    private EditText passwordText;
    private Button nextButton;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SIGN_IN, null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_onboarding_sign_in, container, false);

        this.emailText = (EditText) view.findViewById(R.id.fragment_onboarding_email);
        emailText.addTextChangedListener(this);

        this.passwordText = (EditText) view.findViewById(R.id.fragment_onboarding_password);
        passwordText.addTextChangedListener(this);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::signIn));

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_sign_in_go);
        nextButton.setEnabled(false);
        Views.setSafeOnClickListener(nextButton, ignored -> signIn());

        final Button forgotPassword = (Button) view.findViewById(R.id.fragment_onboarding_sign_in_forgot_password);
        forgotPassword.setOnClickListener(this::forgotPassword);
        view.removeView(forgotPassword);

        if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            forgotPassword.setOnLongClickListener(ignored -> {
                Distribution.startDebugActivity(getActivity());
                return true;
            });
        }

        OnboardingToolbar.of(this, view)
                         .setWantsBackButton(true)
                         .setWantsHelpButton(true)
                         .setDark(true)
                         .replaceHelpButton(forgotPassword);

        if (BuildConfig.DEBUG) {
            final LinearLayout content = (LinearLayout) view.findViewById(R.id.fragment_onboarding_sign_in_content);

            final Button selectHost = new Button(getActivity());
            selectHost.setTextAppearance(getActivity(), R.style.AppTheme_Button_Borderless_Accent_Bounded);
            selectHost.setBackgroundResource(R.drawable.selectable_dark_bounded);
            selectHost.setGravity(Gravity.CENTER);
            final Observable<String> apiUrl =
                    preferences.observableString(DynamicApiEndpoint.PREF_API_ENDPOINT_OVERRIDE,
                                                 apiEndpoint.getUrl());
            bindAndSubscribe(apiUrl, selectHost::setText, Functions.LOG_ERROR);

            final int padding = getResources().getDimensionPixelSize(R.dimen.gap_small);
            selectHost.setPadding(padding, padding, padding, padding);

            Views.setSafeOnClickListener(selectHost, ignored -> {
                try {
                    startActivity(new Intent(getActivity(), Class.forName("is.hello.sense.debug.EnvironmentActivity")));
                } catch (ClassNotFoundException e) {
                    Log.e(getClass().getSimpleName(), "Could not find environment activity", e);
                }
            });

            final LinearLayout.LayoutParams layoutParams =
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                  ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.bottomMargin = getResources().getDimensionPixelSize(R.dimen.gap_small);
            content.addView(selectHost, layoutParams);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        emailText.removeTextChangedListener(this);
        this.emailText = null;

        passwordText.removeTextChangedListener(this);
        this.passwordText = null;

        this.nextButton = null;
    }

    @Override
    public @ColorInt int getStatusBarColor(@NonNull Resources resources) {
        return resources.getColor(R.color.light_accent_darkened);
    }

    @Override
    public void onStatusBarTransitionBegan(@ColorInt int targetColor) {
    }

    @Override
    public void onStatusBarTransitionEnded(@ColorInt int finalColor) {
    }

    //endregion


    //region Actions

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    public void signIn() {
        if (!isInputValid()) {
            final ErrorDialogFragment errorDialogFragment = new ErrorDialogFragment.Builder()
                    .withMessage(StringRef.from(R.string.error_account_incomplete_credentials))
                    .build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
            return;
        }

        final String email = AccountPresenter.normalizeInput(emailText.getText());
        this.emailText.setText(email);

        final String password = this.passwordText.getText().toString();

        LoadingDialogFragment.show(getFragmentManager(),
                getString(R.string.dialog_loading_message),
                LoadingDialogFragment.OPAQUE_BACKGROUND);

        final OAuthCredentials credentials = new OAuthCredentials(apiEndpoint, email, password);
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            apiSessionManager.setSession(session);

            final Observable<Account> initializeLocalState =
                    Observable.combineLatest(accountPresenter.pullAccountPreferences(),
                                             accountPresenter.latest(),
                                             (ignored, account) -> account);

            bindAndSubscribe(initializeLocalState,
                             account -> {
                                 Analytics.trackSignIn(account.getId(),
                                                       account.getFullName(),
                                                       account.getEmail());
                                 getOnboardingActivity().showHomeActivity(OnboardingActivity.FLOW_SIGN_IN);
                             },
                             e -> {
                                 Logger.warn(getClass().getSimpleName(),
                                             "Could not update local account state", e);
                                 Analytics.trackSignIn(session.getAccountId(), null, null);
                                 getOnboardingActivity().showHomeActivity(OnboardingActivity.FLOW_SIGN_IN);
                             });
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());

            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(error, getResources());
            if (ApiException.statusEquals(error, 401)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_invalid_credentials));
            }

            final ErrorDialogFragment dialogFragment = errorDialogBuilder.build();
            dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public void forgotPassword(@NonNull View sender) {
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.FORGOT_PASSWORD_URL));
    }

    //endregion


    //region Next button state control

    private boolean isInputValid() {
        return (TextUtils.getTrimmedLength(emailText.getText()) > 0 &&
                !TextUtils.isEmpty(passwordText.getText()));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        nextButton.setEnabled(isInputValid());
    }

    //endregion
}
