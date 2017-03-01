package is.hello.sense.ui.fragments.onboarding;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.Gender;
import is.hello.sense.databinding.FragmentOnboardingRegisterGenderBinding;
import is.hello.sense.flows.generic.ui.activities.ListActivity;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterGenderFragment extends SenseFragment {

    FragmentOnboardingRegisterGenderBinding binding;

    private Account account;
    @DrawableRes
    private static final int ON_IMAGE_RES = R.drawable.radio_on;
    @DrawableRes
    private static final int OFF_IMAGE_RES = R.drawable.radio_off;

    private Gender currentGender = Gender.MALE;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_GENDER, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        account = AccountEditor.getContainer(this).getAccount();
        final View view = inflater.inflate(R.layout.fragment_onboarding_register_gender, container, false);
        final Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        final Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        Views.setSafeOnClickListener(nextButton,
                                     this::onNextClick);
        Views.setSafeOnClickListener(skipButton,
                                     this::onSkipClick);

        if (AccountEditor.getWantsSkipButton(this)) {
            Views.setSafeOnClickListener(skipButton,
                                         this::onSkipClick);
        } else {
            skipButton.setVisibility(View.GONE);
            nextButton.setText(R.string.action_done);
        }
        return view;

    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.binding = DataBindingUtil.bind(view);
        Views.setSafeOnClickListener(this.binding.fragmentOnboardingGenderMaleImagetextview,
                                     this::onMaleClick);
        Views.setSafeOnClickListener(this.binding.fragmentOnboardingGenderFemaleImagetextview,
                                     this::onFemaleClick);
        Views.setSafeOnClickListener(this.binding.fragmentOnboardingGenderOtherRow,
                                     this::onOtherClick);

        final Account account = AccountEditor.getContainer(this).getAccount();
        if (account.getGender() != null) {
            this.currentGender = account.getGender();
            if (account.getGender() == Gender.MALE) {
                setImages(ON_IMAGE_RES, OFF_IMAGE_RES, OFF_IMAGE_RES);
            } else if (account.getGender() == Gender.FEMALE) {
                setImages(OFF_IMAGE_RES, ON_IMAGE_RES, OFF_IMAGE_RES);
            } else {
                setImages(OFF_IMAGE_RES, OFF_IMAGE_RES, ON_IMAGE_RES);

            }
        }

    }

    private void onNextClick(final View ignored) {
        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setGender(currentGender);
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    private void onSkipClick(final View ignored) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "gender"));
        account.setGender(Gender.OTHER);
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    private void onMaleClick(final View ignored) {
        setImages(ON_IMAGE_RES, OFF_IMAGE_RES, OFF_IMAGE_RES);
        this.currentGender = Gender.MALE;
    }

    private void onFemaleClick(final View ignored) {
        setImages(OFF_IMAGE_RES, ON_IMAGE_RES, OFF_IMAGE_RES);
        this.currentGender = Gender.FEMALE;
    }

    private void onOtherClick(final View ignored) {
        setImages(OFF_IMAGE_RES, OFF_IMAGE_RES, ON_IMAGE_RES);
        ListActivity.startActivity(getActivity(), ListActivity.GENDER_LIST);
        this.currentGender = Gender.OTHER;
    }

    public void setImages(@DrawableRes final int maleRes,
                          @DrawableRes final int femaleRes,
                          @DrawableRes final int otherRes) {
        this.binding.fragmentOnboardingGenderMaleImagetextview.setImageResource(maleRes);
        this.binding.fragmentOnboardingGenderFemaleImagetextview.setImageResource(femaleRes);
        this.binding.fragmentOnboardingGenderOtherImagetextview.setImageResource(otherRes);

    }
}
