package is.hello.sense.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
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
        password.setOnEditorActionListener(this::onPasswordEditorAction);

        Button signInButton = (Button) view.findViewById(R.id.fragment_onboarding_sign_in);
        signInButton.setOnClickListener(this::signIn);

        return view;
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void signIn(@NonNull View sender) {
        String email = this.email.getText().toString();
        String password = this.password.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            ErrorDialogFragment.presentError(getFragmentManager(), new Throwable(getString(R.string.dialog_error_generic_form_issue)));
            return;
        }

        LoadingDialogFragment.show(getFragmentManager());

        OAuthCredentials credentials = new OAuthCredentials(environment, email, password);
        Observable<OAuthSession> request = bindFragment(this, apiService.authorize(credentials));
        request.subscribe(session -> {
            apiSessionManager.setSession(session);
            getOnboardingActivity().showHomeActivity();
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }

    public boolean onPasswordEditorAction(TextView sender, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_GO || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(sender.getWindowToken(), 0);
            signIn(sender);

            return true;
        }

        return false;
    }
}
