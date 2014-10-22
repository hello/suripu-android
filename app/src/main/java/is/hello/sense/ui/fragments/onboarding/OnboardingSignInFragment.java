package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiEnvironment;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.util.EditorActionHandler;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class OnboardingSignInFragment extends InjectionFragment {
    @Inject ApiSessionManager apiSessionManager;
    @Inject ApiService apiService;
    @Inject ApiEnvironment environment;

    private EditText email;
    private EditText password;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_sign_in, container, false);

        this.email = (EditText) view.findViewById(R.id.fragment_onboarding_email);
        this.password = (EditText) view.findViewById(R.id.fragment_onboarding_password);
        password.setOnEditorActionListener(new EditorActionHandler(this::signIn));

        return view;
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void signIn() {
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            ErrorDialogFragment.presentError(getFragmentManager(), new Throwable(getString(R.string.dialog_error_generic_form_issue)));
            return;
        }

        getOnboardingActivity().beginBlockingWork(R.string.dialog_loading_message);

        OAuthCredentials credentials = new OAuthCredentials(environment, email, password);
        Observable<OAuthSession> request = bindFragment(this, apiService.authorize(credentials));
        request.subscribe(session -> {
            getOnboardingActivity().finishBlockingWork();
            apiSessionManager.setSession(session);
            getOnboardingActivity().showWhichPill();
        }, error -> {
            getOnboardingActivity().finishBlockingWork();
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
