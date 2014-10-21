package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.TimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class OnboardingRegisterFragment extends InjectionFragment {
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
        newAccount.setTimeZoneOffset(TimeZone.getDefault().getRawOffset());

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.nameText = (EditText) view.findViewById(R.id.fragment_onboarding_register_name);
        this.emailText = (EditText) view.findViewById(R.id.fragment_onboarding_register_email);
        this.passwordText = (EditText) view.findViewById(R.id.fragment_onboarding_register_password);
        passwordText.setOnEditorActionListener(new EditorActionHandler(this::register));

        return view;
    }

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void register() {
        String name = nameText.getText().toString();
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            ErrorDialogFragment.presentError(getFragmentManager(), new Throwable(getString(R.string.dialog_error_generic_form_issue)));

            return;
        }

        newAccount.setName(name);
        newAccount.setEmail(email);
        newAccount.setPassword(password);

        getOnboardingActivity().beginBlockingWork(R.string.dialog_loading_message);

        Observable<Account> observable = bindFragment(this, apiService.createAccount(newAccount));
        observable.subscribe(unused -> login(), error -> {
            getOnboardingActivity().finishBlockingWork();
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }

    public void login() {
        OAuthCredentials credentials = new OAuthCredentials(environment, emailText.getText().toString(), passwordText.getText().toString());
        Observable<OAuthSession> observable = bindFragment(this, apiService.authorize(credentials));
        observable.subscribe(session -> {
            getOnboardingActivity().finishBlockingWork();

            sessionManager.setSession(session);
            getOnboardingActivity().showBirthday(newAccount);
        }, error -> {
            getOnboardingActivity().finishBlockingWork();
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
