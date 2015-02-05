package is.hello.sense.ui.fragments.onboarding;

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
import android.widget.TextView;

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import java.util.regex.Pattern;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.ErrorResponse;
import is.hello.sense.api.model.RegistrationError;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.animation.Animations;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;
import is.hello.sense.util.EditorActionHandler;

public class OnboardingRegisterFragment extends InjectionFragment {
    private static final Pattern EMAIL = Pattern.compile("^.+@.+\\..+$");

    private LinearLayout credentialsContainer;
    private EditText nameText;
    private EditText emailText;
    private EditText passwordText;

    private TextView registrationErrorText;

    private final Account newAccount = new Account();

    @Inject ApiService apiService;
    @Inject ApiSessionManager sessionManager;
    @Inject PreferencesPresenter preferencesPresenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        newAccount.setHeight(UnitOperations.inchesToCentimeters(70));
        newAccount.setTimeZoneOffset(DateTimeZone.getDefault().getOffset(DateTimeUtils.currentTimeMillis()));

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.registrationErrorText = (TextView) inflater.inflate(R.layout.item_inline_field_error, container, false);
        this.credentialsContainer = (LinearLayout) view.findViewById(R.id.fragment_onboarding_register_credentials);
        Animations.Properties.DEFAULT.apply(credentialsContainer.getLayoutTransition(), false);

        this.nameText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_name);
        this.emailText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_email);
        this.passwordText = (EditText) credentialsContainer.findViewById(R.id.fragment_onboarding_register_password);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::register));

        Button register = (Button) view.findViewById(R.id.fragment_onboarding_register_go);
        Views.setSafeOnClickListener(register, ignored -> register());

        OnboardingToolbar.of(this, view).setWantsBackButton(true);

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
        String name = nameText.getText().toString().trim();
        nameText.setText(name);

        String email = emailText.getText().toString().trim();
        emailText.setText(email);

        if (TextUtils.isEmpty(name)) {
            displayRegistrationError(RegistrationError.NAME_TOO_SHORT);
            nameText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !EMAIL.matcher(email).matches()) {
            displayRegistrationError(RegistrationError.EMAIL_INVALID);
            return;
        }


        String password = passwordText.getText().toString();
        if (password.length() < Constants.MIN_PASSWORD_LENGTH) {
            displayRegistrationError(RegistrationError.PASSWORD_TOO_SHORT);
            return;
        }

        clearRegistrationError();


        newAccount.setName(name);
        newAccount.setEmail(email);
        newAccount.setPassword(password);

        LoadingDialogFragment.show(getFragmentManager(), getString(R.string.dialog_loading_message), true);

        bindAndSubscribe(apiService.createAccount(newAccount), this::login, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            if (ApiException.statusEquals(error, 409)) {
                String errorMessage = getString(R.string.error_account_email_taken, newAccount.getEmail());
                ErrorDialogFragment dialogFragment = ErrorDialogFragment.newInstance(errorMessage);
                dialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            } else if (ApiException.statusEquals(error, 400)) {
                ApiException apiError = (ApiException) error;
                ErrorResponse errorResponse = apiError.getErrorResponse();
                if (errorResponse != null) {
                    RegistrationError registrationError = RegistrationError.fromString(errorResponse.getMessage());
                    displayRegistrationError(registrationError);
                } else {
                    ErrorDialogFragment.presentError(getFragmentManager(), error);
                }
            } else {
                ErrorDialogFragment.presentError(getFragmentManager(), error);
            }
        });
    }

    public void login(@NonNull Account createdAccount) {
        OAuthCredentials credentials = new OAuthCredentials(emailText.getText().toString(), passwordText.getText().toString());
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            sessionManager.setSession(session);
            preferencesPresenter.pullAccountPreferences().subscribe();
            Analytics.trackEvent(Analytics.Global.EVENT_SIGNED_IN, null);

            getOnboardingActivity().showBirthday(createdAccount);
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(), () -> {});
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
