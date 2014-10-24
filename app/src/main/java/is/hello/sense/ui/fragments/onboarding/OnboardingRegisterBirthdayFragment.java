package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterBirthdayFragment extends Fragment {
    private static final String ARG_ACCOUNT = OnboardingRegisterBirthdayFragment.class.getName() + ".ARG_ACCOUNT";

    private Account account;

    public static OnboardingRegisterBirthdayFragment newInstance(@NonNull Account account) {
        OnboardingRegisterBirthdayFragment fragment = new OnboardingRegisterBirthdayFragment();

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

            Analytics.event(Analytics.EVENT_ONBOARDING_BIRTHDAY, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_birthday, container, false);

        DatePicker datePicker = (DatePicker) view.findViewById(R.id.fragment_onboarding_register_birthday_picker);
        if (account.getBirthDate() != null) {
            datePicker.updateDate(account.getBirthDate().getYear(),
                                  account.getBirthDate().getMonthOfYear(),
                                  account.getBirthDate().getDayOfMonth());
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        nextButton.setOnClickListener(ignored -> {
            account.setBirthDate(new DateTime(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(), 0, 0));
            ((OnboardingActivity) getActivity()).showGender(account);
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(ARG_ACCOUNT, account);
    }
}
