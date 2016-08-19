package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import is.hello.sense.interactors.AccountInteractor;
import is.hello.sense.interactors.PreferencesInteractor;
import is.hello.sense.interactors.UserFeaturesInteractor;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.StatusBarColorProvider;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.LabelEditText;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.InternalPrefManager;
import is.hello.sense.util.Logger;
import rx.Observable;

public class SignInFragment extends InjectionFragment
        implements StatusBarColorProvider, TextWatcher {
    @Inject
    ApiEndpoint apiEndpoint;
    @Inject
    ApiSessionManager apiSessionManager;
    @Inject
    ApiService apiService;
    @Inject
    AccountInteractor accountPresenter;
    @Inject
    PreferencesInteractor preferences;
    @Inject
    UserFeaturesInteractor userFeaturesPresenter;

    private LabelEditText emailTextLET;
    private LabelEditText passwordTextLET;
    private Button nextButton;


    //region Lifecycle

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_SIGN_IN, null);
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_onboarding_sign_in, container, false);

        this.emailTextLET = (LabelEditText) view.findViewById(R.id.fragment_onboarding_email_let);
        emailTextLET.addTextChangedListener(this);

        this.passwordTextLET = (LabelEditText) view.findViewById(R.id.fragment_onboarding_password_let);
        passwordTextLET.addTextChangedListener(this);
        passwordTextLET.setOnEditorActionListener(new EditorActionHandler(this::signIn));

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
            Styles.setTextAppearance(selectHost, R.style.AppTheme_Button_Borderless_Accent_Bounded);
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

        emailTextLET.removeTextChangedListener(this);
        this.emailTextLET = null;

        passwordTextLET.removeTextChangedListener(this);
        this.passwordTextLET = null;

        this.nextButton = null;
    }

    @Override
    public
    @ColorInt
    int getStatusBarColor(@NonNull final Resources resources) {
        return ContextCompat.getColor(getActivity(), R.color.light_accent_darkened);
    }

    @Override
    public void onStatusBarTransitionBegan(@ColorInt final int targetColor) {
    }

    @Override
    public void onStatusBarTransitionEnded(@ColorInt final int finalColor) {
    }

    //endregion


    //region Actions

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    public void signIn() {
        emailTextLET.removeError();
        passwordTextLET.removeError();

        if (inputIsInvalid(true)) {
            return;
        }

        final String email = AccountInteractor.normalizeInput(emailTextLET.getInputText());
        this.emailTextLET.setInputText(email);

        final String password = this.passwordTextLET.getInputText();

        LoadingDialogFragment.show(getFragmentManager(),
                                   getString(R.string.dialog_loading_message),
                                   LoadingDialogFragment.OPAQUE_BACKGROUND);

        final OAuthCredentials credentials = new OAuthCredentials(apiEndpoint, email, password);
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            apiSessionManager.setSession(session);

            final Observable<Account> initializeLocalState =
                    Observable.combineLatest(accountPresenter.pullAccountPreferences(),
                                             userFeaturesPresenter.storeFeaturesInPrefs(),
                                             accountPresenter.latest(),
                                             (ignored, ignored2, account) -> account);

            bindAndSubscribe(initializeLocalState,
                             account -> {
                                 InternalPrefManager.setAccountId(getActivity(), account.getId());
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
                    new ErrorDialogFragment.Builder(error, getActivity());
            if (ApiException.statusEquals(error, 401)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_invalid_credentials));
            }

            final ErrorDialogFragment dialogFragment = errorDialogBuilder.build();
            dialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public void forgotPassword(@NonNull final View sender) {
        UserSupport.openUri(getActivity(), Uri.parse(UserSupport.FORGOT_PASSWORD_URL));
    }

    //endregion


    //region Next button state control

    private boolean inputIsInvalid(final boolean showErrors) {
        if (emailTextLET.isInputEmpty() || !AccountInteractor.validateEmail(emailTextLET.getInputText())) {
            if (showErrors) {
                emailTextLET.setError(R.string.invalid_email); // todo confirm this error message.
            }
            return true;
        }
        if (passwordTextLET.isInputEmpty()) {
            if (showErrors) {
                passwordTextLET.setError(R.string.invalid_password); // todo confirm this error message.
            }
            return true;
        }
        return false;
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    @Override
    public void afterTextChanged(final Editable s) {
        nextButton.setEnabled(!inputIsInvalid(false));
    }

    //endregion
}
