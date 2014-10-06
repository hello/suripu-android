package is.hello.sense.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.joda.time.DateTime;

import java.util.TimeZone;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Gender;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthCredentials;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.DatePickerDialogFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.HeightDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.dialogs.WeightDialogFragment;
import is.hello.sense.util.Units;
import rx.Observable;

import static rx.android.observables.AndroidObservable.bindFragment;

public class OnboardingRegisterFragment extends InjectionFragment {
    private static final int PICK_DOB_REQUEST_CODE = 0x99;
    private static final int PICK_WEIGHT_REQUEST_CODE = 0x100;
    private static final int PICK_HEIGHT_REQUEST_CODE = 0x101;

    private EditText nameText;
    private EditText emailText;
    private EditText passwordText;
    private Spinner genderSpinner;
    private Button heightButton;
    private Button weightButton;
    private Button dateOfBirthButton;

    private final Account newAccount = new Account();

    @Inject ApiService apiService;
    @Inject ApiSessionManager sessionManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        newAccount.setWeight(Units.poundsToGrams(120));
        newAccount.setHeight(Units.inchesToCentimeters(70));
        newAccount.setTimeZoneOffset(TimeZone.getDefault().getRawOffset());
        newAccount.setBirthDate(DateTime.now());

        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register, container, false);

        this.nameText = (EditText) view.findViewById(R.id.fragment_onboarding_register_name);
        this.emailText = (EditText) view.findViewById(R.id.fragment_onboarding_register_email);
        this.passwordText = (EditText) view.findViewById(R.id.fragment_onboarding_register_password);

        this.genderSpinner = (Spinner) view.findViewById(R.id.fragment_onboarding_register_gender);
        Gender.Adapter genderAdapter = new Gender.Adapter(getActivity());
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                newAccount.setGender(genderAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                newAccount.setGender(null);
            }
        });
        genderSpinner.setSelection(genderAdapter.getPosition(Gender.OTHER));

        this.heightButton = (Button) view.findViewById(R.id.fragment_onboarding_register_height);
        heightButton.setOnClickListener(this::showHeightSelector);

        this.weightButton = (Button) view.findViewById(R.id.fragment_onboarding_register_weight);
        weightButton.setOnClickListener(this::showWeightSelector);

        this.dateOfBirthButton = (Button) view.findViewById(R.id.fragment_onboarding_register_dob);
        dateOfBirthButton.setOnClickListener(this::showDateOfBirthSelector);

        Button actionButton = (Button) view.findViewById(R.id.fragment_onboarding_register_action);
        actionButton.setOnClickListener(this::register);

        updateHeightSelector();
        updateWeightSelector();
        updateDateOfBirthSelector();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_DOB_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int year = data.getIntExtra(DatePickerDialogFragment.RESULT_YEAR, 1962);
            int month = data.getIntExtra(DatePickerDialogFragment.RESULT_MONTH, 10);
            int day = data.getIntExtra(DatePickerDialogFragment.RESULT_DAY, 20);
            DateTime birthDate = new DateTime(year, month, day, 0, 0);
            newAccount.setBirthDate(birthDate);
            updateDateOfBirthSelector();
        } else if (requestCode == PICK_WEIGHT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int value = data.getIntExtra(WeightDialogFragment.RESULT_WEIGHT, 120);
            newAccount.setWeight(Units.poundsToGrams(value));
            updateWeightSelector();
        } else if (requestCode == PICK_HEIGHT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            int value = data.getIntExtra(HeightDialogFragment.RESULT_HEIGHT, 70);
            newAccount.setHeight(Units.inchesToCentimeters(value));

            updateHeightSelector();
        }
    }

    private OnboardingActivity getOnboardingActivity() {
        return (OnboardingActivity) getActivity();
    }


    public void showHeightSelector(@NonNull View sender) {
        int height = Units.centimetersToInches(newAccount.getHeight());
        HeightDialogFragment heightDialogFragment = HeightDialogFragment.newInstance(height);
        heightDialogFragment.setTargetFragment(this, PICK_HEIGHT_REQUEST_CODE);
        heightDialogFragment.show(getFragmentManager(), HeightDialogFragment.TAG);
    }

    private void updateHeightSelector() {
        int height = Units.centimetersToInches(newAccount.getHeight());
        int feet = height / 12;
        int inches = height % 12;
        heightButton.setText(feet + "' " + inches + "''");
    }

    public void showWeightSelector(@NonNull View sender) {
        int currentWeight = Units.gramsToPounds(newAccount.getWeight());
        WeightDialogFragment weightDialogFragment = WeightDialogFragment.newInstance(currentWeight);
        weightDialogFragment.setTargetFragment(this, PICK_WEIGHT_REQUEST_CODE);
        weightDialogFragment.show(getFragmentManager(), WeightDialogFragment.TAG);
    }

    private void updateWeightSelector() {
        weightButton.setText(Integer.toString(Units.gramsToPounds(newAccount.getWeight())));
    }

    public void showDateOfBirthSelector(@NonNull View sender) {
        DatePickerDialogFragment datePickerDialogFragment = DatePickerDialogFragment.newInstance(newAccount.getBirthDate());
        datePickerDialogFragment.setTargetFragment(this, PICK_DOB_REQUEST_CODE);
        datePickerDialogFragment.show(getFragmentManager(), DatePickerDialogFragment.TAG);
    }

    private void updateDateOfBirthSelector() {
        dateOfBirthButton.setText(newAccount.getBirthDate().toString("MMMM dd, yyyy"));
    }


    public void register(@NonNull View sender) {
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

        LoadingDialogFragment.show(getFragmentManager());

        Observable<Account> observable = bindFragment(this, apiService.createAccount(newAccount));
        observable.subscribe(unused -> login(), error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }

    public void login() {
        OAuthCredentials credentials = new OAuthCredentials(emailText.getText().toString(), passwordText.getText().toString());
        Observable<OAuthSession> observable = bindFragment(this, apiService.authorize(credentials));
        observable.subscribe(session -> {
            sessionManager.setSession(session);
            LoadingDialogFragment.close(getFragmentManager());

            getOnboardingActivity().showHomeActivity();
        }, error -> {
            LoadingDialogFragment.close(getFragmentManager());
            ErrorDialogFragment.presentError(getFragmentManager(), error);
        });
    }
}
