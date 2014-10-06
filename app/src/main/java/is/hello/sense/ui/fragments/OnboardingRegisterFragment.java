package is.hello.sense.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.ui.common.InjectionFragment;

public class OnboardingRegisterFragment extends InjectionFragment {
    private EditText nameText;
    private EditText emailText;
    private EditText passwordText;
    private Spinner genderSpinner;
    private Button heightButton;
    private Button weightButton;
    private Button dateOfBirthButton;
    private Button actionButton;

    private final Account newAccount = new Account();

    @Inject ApiService apiService;
    @Inject ApiSessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.nameText = (EditText) view.findViewById(R.id.fragment_onboarding_register_name);
        this.emailText = (EditText) view.findViewById(R.id.fragment_onboarding_register_email);
        this.passwordText = (EditText) view.findViewById(R.id.fragment_onboarding_register_password);
        this.genderSpinner = (Spinner) view.findViewById(R.id.fragment_onboarding_register_gender);

        this.heightButton = (Button) view.findViewById(R.id.fragment_onboarding_register_height);
        heightButton.setOnClickListener(this::showHeightSelector);

        this.weightButton = (Button) view.findViewById(R.id.fragment_onboarding_register_weight);
        weightButton.setOnClickListener(this::showWeightSelector);

        this.dateOfBirthButton = (Button) view.findViewById(R.id.fragment_onboarding_register_dob);
        dateOfBirthButton.setOnClickListener(this::showDateOfBirthSelector);

        this.actionButton = (Button) view.findViewById(R.id.fragment_onboarding_register_action);
        actionButton.setOnClickListener(this::register);

        return view;
    }


    public void showHeightSelector(@NonNull View sender) {

    }

    public void showWeightSelector(@NonNull View sender) {

    }

    public void showDateOfBirthSelector(@NonNull View sender) {

    }

    public void register(@NonNull View sender) {

    }
}
