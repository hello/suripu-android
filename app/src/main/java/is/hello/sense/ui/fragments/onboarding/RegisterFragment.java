package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import javax.inject.Inject;

import is.hello.buruberi.util.StringRef;
import is.hello.go99.animators.AnimatorTemplate;
import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.ApiEndpoint;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.DynamicApiEndpoint;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.model.RegistrationError;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.AccountPresenter;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.StatusBarColorProvider;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

public class RegisterFragment extends InjectionFragment
        implements StatusBarColorProvider, TextWatcher {
    @Inject ApiService apiService;
    @Inject ApiEndpoint apiEndpoint;
    @Inject ApiSessionManager sessionManager;
    @Inject AccountPresenter accountPresenter;
    @Inject PreferencesPresenter preferences;

    private Account account;

    private EditText nameText;
    private EditText emailText;
    private EditText passwordText;

    private Button nextButton;

    private LinearLayout credentialsContainer;
    private TextView registrationErrorText;


    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_ACCOUNT, null);

            this.account = Account.createDefault();
        } else {
            this.account = (Account) savedInstanceState.getSerializable("account");
        }

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.registrationErrorText = (TextView) inflater.inflate(R.layout.item_inline_field_error, container, false);
        this.credentialsContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_register_credentials);
        AnimatorTemplate.DEFAULT.apply(credentialsContainer.getLayoutTransition());

        this.nameText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_name);
        nameText.addTextChangedListener(this);

        this.emailText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_email);
        emailText.addTextChangedListener(this);

        this.passwordText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_password);
        passwordText.addTextChangedListener(this);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::register));

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_register_next);
        nextButton.setEnabled(false);
        Views.setSafeOnClickListener(nextButton, ignored -> register());

        OnboardingToolbar.of(this, view).setWantsBackButton(true);

        if (BuildConfig.DEBUG) {
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
            credentialsContainer.addView(selectHost, layoutParams);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        nameText.removeTextChangedListener(this);
        this.nameText = null;

        emailText.removeTextChangedListener(this);
        this.emailText = null;

        passwordText.removeTextChangedListener(this);
        this.passwordText = null;

        this.nextButton = null;
        this.credentialsContainer = null;
        this.registrationErrorText = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("account", account);
    }

    @Override
    public int getStatusBarColor(@NonNull Resources resources) {
        return resources.getColor(R.color.status_bar_grey);
    }

    @Override
    public void onStatusBarTransitionBegan(@ColorInt int targetColor) {
    }

    @Override
    public void onStatusBarTransitionEnded(@ColorInt int finalColor) {
    }

    //endregion


    //region Registration

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }

    private void displayRegistrationError(@NonNull RegistrationError error) {
        clearRegistrationError();
        registrationErrorText.setText(error.messageRes);
        final EditText affectedField;
        switch (error) {
            default:
            case UNKNOWN:
            case NAME_TOO_LONG:
            case NAME_TOO_SHORT: {
                affectedField = nameText;
                break;
            }

            case EMAIL_INVALID: {
                affectedField = emailText;
                break;
            }

            case PASSWORD_INSECURE:
            case PASSWORD_TOO_SHORT: {
                affectedField = passwordText;
                break;
            }
        }

        credentialsContainer.addView(registrationErrorText,
                                     credentialsContainer.indexOfChild(affectedField));
        affectedField.setBackgroundResource(R.drawable.edit_text_background_error);
        affectedField.requestFocus();
    }

    private void clearRegistrationError() {
        credentialsContainer.removeView(registrationErrorText);

        nameText.setBackgroundResource(R.drawable.edit_text_selector);
        emailText.setBackgroundResource(R.drawable.edit_text_selector);
        passwordText.setBackgroundResource(R.drawable.edit_text_selector);
    }

    private boolean doCompleteValidation() {
        final CharSequence name = AccountPresenter.normalizeInput(nameText.getText());
        final CharSequence email = AccountPresenter.normalizeInput(emailText.getText());
        final CharSequence password = passwordText.getText();

        if (!AccountPresenter.validateName(name)) {
            displayRegistrationError(RegistrationError.NAME_TOO_SHORT);
            nameText.requestFocus();
            return false;
        }

        if (!AccountPresenter.validateEmail(email)) {
            displayRegistrationError(RegistrationError.EMAIL_INVALID);
            emailText.requestFocus();
            return false;
        }

        if (!AccountPresenter.validatePassword(password)) {
            displayRegistrationError(RegistrationError.PASSWORD_TOO_SHORT);
            passwordText.requestFocus();
            return false;
        }

        nameText.setText(name);
        emailText.setText(email);
        clearRegistrationError();

        return true;
    }


    public void register() {
        if (!doCompleteValidation()) {
            return;
        }

        account.setName(nameText.getText().toString());
        account.setEmail(emailText.getText().toString());
        account.setPassword(passwordText.getText().toString());

        LoadingDialogFragment.show(getFragmentManager(),
                                   getString(R.string.dialog_loading_message),
                                   LoadingDialogFragment.OPAQUE_BACKGROUND);

        bindAndSubscribe(apiService.createAccount(account), this::login, error -> {
            LoadingDialogFragment.close(getFragmentManager());

            if (ApiException.statusEquals(error, 400)) {
                final ApiException apiError = (ApiException) error;
                final ErrorResponse errorResponse = apiError.getErrorResponse();
                if (errorResponse != null) {
                    final RegistrationError registrationError =
                            RegistrationError.fromString(errorResponse.getMessage());
                    displayRegistrationError(registrationError);

                    return;
                }
            }

            final ErrorDialogFragment.Builder errorDialogBuilder =
                    new ErrorDialogFragment.Builder(error, getResources());

            if (ApiException.statusEquals(error, 409)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_email_taken,
                                                              account.getEmail()));
            }

            final ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public void login(@NonNull Account createdAccount) {
        final OAuthCredentials credentials = new OAuthCredentials(apiEndpoint,
                                                                  emailText.getText().toString(),
                                                                  passwordText.getText().toString());
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            sessionManager.setSession(session);

            preferences.putLocalDate(PreferencesPresenter.ACCOUNT_CREATION_DATE,
                                     LocalDate.now());
            accountPresenter.pushAccountPreferences();

            Analytics.trackRegistration(session.getAccountId(),
                                        createdAccount.getName(),
                                        createdAccount.getEmail(),
                                        DateTime.now());

            getOnboardingActivity().showBirthday(createdAccount, true);
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getActivity(), error);
        });
    }

    //endregion


    //region Next button state control

    private boolean isInputValidSimple() {
        return (!TextUtils.isEmpty(nameText.getText()) &&
                TextUtils.getTrimmedLength(emailText.getText()) > 0 &&
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
        nextButton.setEnabled(isInputValidSimple());
    }

    //endregion
}
