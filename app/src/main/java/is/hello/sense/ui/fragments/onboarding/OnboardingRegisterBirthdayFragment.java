package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;

import org.joda.time.LocalDate;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterBirthdayFragment extends AccountEditingFragment {
    private Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = getContainer().getAccount();

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.EVENT_ONBOARDING_BIRTHDAY, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_register_birthday, container, false);

        DatePicker datePicker = (DatePicker) view.findViewById(R.id.fragment_onboarding_register_birthday_picker);
        if (account.getBirthDate() != null) {
            datePicker.updateDate(account.getBirthDate().getYear(),
                                  account.getBirthDate().getMonthOfYear() - 1,
                                  account.getBirthDate().getDayOfMonth());
        }

        Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        Views.setSafeOnClickListener(nextButton, ignored -> {
            account.setBirthDate(new LocalDate(datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth()));
            getContainer().onAccountUpdated(this);
        });

        Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        if (getWantsSkipButton()) {
            Views.setSafeOnClickListener(skipButton, ignored -> getContainer().onAccountUpdated(this));
        } else {
            skipButton.setVisibility(View.INVISIBLE);
        }

        return view;
    }
}
