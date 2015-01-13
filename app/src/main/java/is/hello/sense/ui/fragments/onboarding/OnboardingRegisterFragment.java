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

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import java.util.regex.Pattern;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.EditorActionHandler;

public class OnboardingRegisterFragment extends InjectionFragment {
    private static final Pattern EMAIL = Pattern.compile("^.+@.+\\..+$");

    private EditText nameText;
    private EditText emailText;
    private EditText passwordText;

    private final Account newAccount = new Account();

    @Inject ApiService apiService;
    @Inject ApiSessionManager sessionManager;
    @Inject ApiEnvironment environment;

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

        this.nameText = (EditText) view.findViewById(R.id.fragment_onboarding_register_name);
        this.emailText = (EditText) view.findViewById(R.id.fragment_onboarding_register_email);
        this.passwordText = (EditText) view.findViewById(R.id.fragment_onboarding_register_password);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::register));

        Button register = (Button) view.findViewById(R.id.fragment_onboarding_register_go);
        Views.setSafeOnClickListener(register, ignored -> register());

        OnboardingToolbar.of(this, view).setWantsBackButton(true);

        return view;
    }

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void register() {
        String name = nameText.getText().toString().trim();
        nameText.setText(name);

        String email = emailText.getText().toString().trim();
        emailText.setText(email);

        if (TextUtils.isEmpty(name)) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_invalid_register_name));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            nameText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !EMAIL.matcher(email).matches()) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_invalid_register_email));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            emailText.requestFocus();
            return;
        }


        String password = passwordText.getText().toString();
        if (password.length() <= 3) {
            ErrorDialogFragment errorDialogFragment = ErrorDialogFragment.newInstance(getString(R.string.error_invalid_register_password));
            errorDialogFragment.show(getFragmentManager(), ErrorDialogFragment.TAG);
            passwordText.requestFocus();
            return;
        }


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
            } else {
                ErrorDialogFragment.presentError(getFragmentManager(), error);
            }
        });
    }

    public void login(@NonNull Account createdAccount) {
        OAuthCredentials credentials = new OAuthCredentials(environment, emailText.getText().toString(), passwordText.getText().toString());
        bindAndSubscribe(apiService.authorize(credentials), session -> {
            sessionManager.setSession(session);
            Analytics.trackEvent(Analytics.EVENT_SIGNED_IN, null);

            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(),
                    () -> getOnboardingActivity().showBirthday(createdAccount));
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
