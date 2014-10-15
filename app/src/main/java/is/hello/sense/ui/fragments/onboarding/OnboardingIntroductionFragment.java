package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;

public class OnboardingIntroductionFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_introduction, container, false);

        Button register = (Button) view.findViewById(R.id.fragment_onboarding_introduction_register);
        register.setOnClickListener(this::showRegister);

        Button signIn = (Button) view.findViewById(R.id.fragment_onboarding_introduction_sign_in);
        signIn.setOnClickListener(this::showSignIn);

        return view;
    }


    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void showRegister(@NonNull View sender) {
        getOnboardingActivity().showRegistration();
    }

    public void showSignIn(@NonNull View sender) {
        getOnboardingActivity().showSignIn();
    }
}
