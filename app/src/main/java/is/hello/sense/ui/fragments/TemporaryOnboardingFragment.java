package is.hello.sense.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.ui.activities.HomeActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Logger;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class TemporaryOnboardingFragment extends InjectionFragment {
    @Inject ApiSessionManager apiSessionManager;
    @Inject ApiService apiService;

    private EditText email;
    private EditText password;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_temporary_onboarding, container, false);

        this.email = (EditText) view.findViewById(R.id.fragment_temporary_onboarding_email);
        this.password = (EditText) view.findViewById(R.id.fragment_temporary_onboarding_password);

        Button signInButton = (Button) view.findViewById(R.id.fragment_temporary_onboarding_sign_in);
        signInButton.setOnClickListener(this::signIn);

        return view;
    }


    public void showHomeActivity() {
        startActivity(new Intent(getActivity(), HomeActivity.class));
        getActivity().finish();
    }

    public void signIn(@NonNull View sender) {
        LoadingDialogFragment.show(getFragmentManager());

        OAuthCredentials credentials = new OAuthCredentials(email.getText().toString(), password.getText().toString());
        Observable<OAuthSession> request = bindFragment(this, apiService.authorize(credentials));
        request.subscribe(session -> {
            apiSessionManager.setSession(session);
            showHomeActivity();
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
