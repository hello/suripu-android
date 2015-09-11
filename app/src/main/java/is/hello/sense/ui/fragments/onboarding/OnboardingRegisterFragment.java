package is.hello.sense.ui.fragments.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

public class OnboardingRegisterFragment extends InjectionFragment {
    private LinearLayout credentialsContainer;
    private EditText nameText;
    private EditText emailText;
    private EditText passwordText;

    private TextView registrationErrorText;

    private final Account newAccount = Account.createDefault();

    @Inject ApiService apiService;
    @Inject ApiEndpoint apiEndpoint;
    @Inject ApiSessionManager sessionManager;
    @Inject AccountPresenter accountPresenter;
    @Inject PreferencesPresenter preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.Onboarding.EVENT_ACCOUNT, null);

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.registrationErrorText = (TextView) inflater.inflate(R.layout.item_inline_field_error, container, false);
        this.credentialsContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_register_credentials);
        AnimatorTemplate.DEFAULT.apply(credentialsContainer.getLayoutTransition());

        this.nameText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_name);
        this.emailText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_email);
        this.passwordText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_password);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::register));

        Button register = (Button) view.findViewById(R.id.fragment_onboarding_register_go);
        Views.setSafeOnClickListener(register, ignored -> register());

        OnboardingToolbar.of(this, view).setWantsBackButton(true);

        if (BuildConfig.DEBUG) {
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
            credentialsContainer.addView(selectHost, layoutParams);
        }

        return view;
    }



    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    private void displayRegistrationError(@NonNull RegistrationError error) {
        clearRegistrationError();
        registrationErrorText.setText(error.messageRes);
        EditText affectedField;
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

        credentialsContainer.addView(registrationErrorText, credentialsContainer.indexOfChild(affectedField));
        affectedField.setBackgroundResource(R.drawable.edit_text_background_error);
        affectedField.requestFocus();
    }

    private void clearRegistrationError() {
        credentialsContainer.removeView(registrationErrorText);

        nameText.setBackgroundResource(R.drawable.edit_text_selector);
        emailText.setBackgroundResource(R.drawable.edit_text_selector);
        passwordText.setBackgroundResource(R.drawable.edit_text_selector);
    }


    public void register() {
        String name = AccountPresenter.normalizeInput(nameText.getText());
        nameText.setText(name);

        String email = AccountPresenter.normalizeInput(emailText.getText());
        emailText.setText(email);

        if (!AccountPresenter.validateName(name)) {
            displayRegistrationError(RegistrationError.NAME_TOO_SHORT);
            nameText.requestFocus();
            return;
        }

        if (!AccountPresenter.validateEmail(email)) {
            displayRegistrationError(RegistrationError.EMAIL_INVALID);
            emailText.requestFocus();
            return;
        }


        String password = passwordText.getText().toString();
        if (!AccountPresenter.validatePassword(password)) {
            displayRegistrationError(RegistrationError.PASSWORD_TOO_SHORT);
            passwordText.requestFocus();
            return;
        }

        clearRegistrationError();


        newAccount.setName(name);
        newAccount.setEmail(email);
        newAccount.setPassword(password);

        LoadingDialogFragment.show(getFragmentManager(),
                getString(R.string.dialog_loading_message),
                LoadingDialogFragment.OPAQUE_BACKGROUND);

        bindAndSubscribe(apiService.createAccount(newAccount), this::login, error -> {
            LoadingDialogFragment.close(getFragmentManager());

            if (ApiException.statusEquals(error, 400)) {
                ApiException apiError = (ApiException) error;
                ErrorResponse errorResponse = apiError.getErrorResponse();
                if (errorResponse != null) {
                    RegistrationError registrationError = RegistrationError.fromString(errorResponse.getMessage());
                    displayRegistrationError(registrationError);

                    return;
                }
            }

            ErrorDialogFragment.Builder errorDialogBuilder = new ErrorDialogFragment.Builder(error);

            if (ApiException.statusEquals(error, 409)) {
                errorDialogBuilder.withMessage(StringRef.from(R.string.error_account_email_taken, newAccount.getEmail()));
            }

            ErrorDialogFragment errorDialogFragment = errorDialogBuilder.build();
            errorDialogFragment.showAllowingStateLoss(getFragmentManager(), ErrorDialogFragment.TAG);
        });
    }

    public void login(@NonNull Account createdAccount) {
        OAuthCredentials credentials = new OAuthCredentials(apiEndpoint, emailText.getText().toString(), passwordText.getText().toString());
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            sessionManager.setSession(session);

            preferences.putLocalDate(PreferencesPresenter.ACCOUNT_CREATION_DATE,
                                     LocalDate.now());
            accountPresenter.pushAccountPreferences();

            Analytics.trackRegistration(session.getAccountId(),
                                        createdAccount.getName(),
                                        DateTime.now());

            getOnboardingActivity().showBirthday(createdAccount);
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), null);
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
