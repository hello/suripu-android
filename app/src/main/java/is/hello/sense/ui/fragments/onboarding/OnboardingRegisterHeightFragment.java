package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.units.UnitOperations;
import is.hello.sense.util.EditorActionHandler;
import is.hello.sense.util.Logger;

public class OnboardingRegisterHeightFragment extends Fragment {
    private static final String ARG_ACCOUNT = OnboardingRegisterHeightFragment.class.getName() + ".ARG_ACCOUNT";

    private Account account;
    private EditText feetText;
    private EditText inchesText;

    public static OnboardingRegisterHeightFragment newInstance(@NonNull Account account) {
        OnboardingRegisterHeightFragment fragment = new OnboardingRegisterHeightFragment();

        Bundle arguments = new Bundle();
        arguments.putSerializable(ARG_ACCOUNT, account);
        fragment.setArguments(arguments);

        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.account = (Account) savedInstanceState.getSerializable(ARG_ACCOUNT);
        } else {
            this.account = (Account) getArguments().getSerializable(ARG_ACCOUNT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_height, container, false);

        this.feetText = (EditText) view.findViewById(R.id.fragment_onboarding_register_height_feet);
        this.inchesText = (EditText) view.findViewById(R.id.fragment_onboarding_register_height_inches);
        inchesText.setOnEditorActionListener(new EditorActionHandler(this::next) {
            @Override
            protected boolean isValid(@Nullable CharSequence value) {
                try {
                    return (Integer.parseInt(feetText.getText().toString()) > 0);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(ignored -> next());

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ACCOUNT, account);
    }


    public void next() {
        try {
            int feet = Integer.parseInt(feetText.getText().toString());
            int inches = TextUtils.isEmpty(inchesText.getText()) ? 0 : Integer.parseInt(inchesText.getText().toString());
            int totalInches = (feet / 12) + inches;
            account.setHeight(UnitOperations.inchesToCentimeters(totalInches));
            ((OnboardingActivity) getActivity()).showWeight(account);
        } catch (NumberFormatException e) {
            Logger.warn(OnboardingRegisterHeightFragment.class.getSimpleName(), "Invalid input fed to height fragment, ignoring.", e);
        }
    }
}
