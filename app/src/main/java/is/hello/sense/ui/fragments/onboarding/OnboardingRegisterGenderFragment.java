package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
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
import is.hello.sense.flows.generic.ui.fragments.ListFragment;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Constants;

public class OnboardingRegisterGenderFragment extends SenseFragment {
    private static final int GENDER_REQUEST = 98281;

    @DrawableRes
    private static final int ON_IMAGE_RES = R.drawable.radio_on;
    @DrawableRes
    private static final int OFF_IMAGE_RES = R.drawable.radio_off;

    @NonNull
    private Gender currentGender = Gender.OTHER;
    @NonNull
    private String currentOtherGender = Constants.EMPTY_STRING;
    FragmentOnboardingRegisterGenderBinding binding;

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
        return inflater.inflate(R.layout.fragment_onboarding_register_gender, container, false);

    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Button nextButton = (Button) view.findViewById(R.id.fragment_onboarding_next);
        final Button skipButton = (Button) view.findViewById(R.id.fragment_onboarding_skip);
        Views.setSafeOnClickListener(nextButton,
                                     this::onNextClick);
        Views.setSafeOnClickListener(skipButton,
                                     this::onSkipClick);
        this.binding = DataBindingUtil.bind(view);
        Views.setSafeOnClickListener(this.binding.fragmentOnboardingGenderMaleImagetextview,
                                     this::onMaleClick);
        Views.setSafeOnClickListener(this.binding.fragmentOnboardingGenderFemaleImagetextview,
                                     this::onFemaleClick);
        Views.setSafeOnClickListener(this.binding.fragmentOnboardingGenderOtherRow,
                                     this::onOtherClick);

        final Account account = AccountEditor.getContainer(this).getAccount();
        account.useDefaultGenderIfNull();
        this.currentGender = account.getGender();
        this.currentOtherGender = account.getGenderOther();
        if (AccountEditor.getWantsSkipButton(this)) {
            Views.setSafeOnClickListener(skipButton,
                                         this::onSkipClick);
        } else {
            skipButton.setVisibility(View.GONE);
            nextButton.setText(R.string.action_done);
        }
        updateView();
    }

    @Override
    public void onActivityResult(final int requestCode,
                                 final int resultCode,
                                 @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GENDER_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                final String selectedGender = data.getStringExtra(ListFragment.KEY_SELECTION);
                if (selectedGender != null) {
                    // currentGender is set to OTHER from onOtherClick
                    this.currentOtherGender = selectedGender;
                    updateView();
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.binding = null;
    }

    public void updateView() {
        if (currentGender == Gender.MALE) {
            setImages(ON_IMAGE_RES, OFF_IMAGE_RES, OFF_IMAGE_RES);
        } else if (currentGender == Gender.FEMALE) {
            setImages(OFF_IMAGE_RES, ON_IMAGE_RES, OFF_IMAGE_RES);
        } else if (currentOtherGender.isEmpty()) {
            setImages(OFF_IMAGE_RES, OFF_IMAGE_RES, OFF_IMAGE_RES);
        } else {
            setImages(OFF_IMAGE_RES, OFF_IMAGE_RES, ON_IMAGE_RES);
        }
        updateSelectText(currentOtherGender);
    }

    private void updateSelectText(@Nullable final String genderOther) {
        if (genderOther == null || genderOther.isEmpty()) {
            this.binding.fragmentOnboardingGenderSelect.setText(R.string.action_select);
        } else {
            this.binding.fragmentOnboardingGenderSelect.setText(genderOther);
        }
    }

    private void onNextClick(final View ignored) {
        if (currentGender == Gender.OTHER && currentOtherGender.isEmpty()) {
            onSkipClick(ignored);
            return;
        }
        final AccountEditor.Container container = AccountEditor.getContainer(this);
        container.getAccount().setGender(currentGender);
        if (currentGender == Gender.OTHER) {
            container.getAccount().setGenderOther(currentOtherGender);
        }
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    private void onSkipClick(final View ignored) {
        Analytics.trackEvent(Analytics.Onboarding.EVENT_SKIP, Analytics.createProperties(Analytics.Onboarding.PROP_SKIP_SCREEN, "gender"));
        final Account account = AccountEditor.getContainer(this).getAccount();
        account.setGender(Gender.OTHER);
        account.setGenderOther(Constants.EMPTY_STRING);
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    private void onMaleClick(final View ignored) {
        this.currentGender = Gender.MALE;
        updateView();
    }

    private void onFemaleClick(final View ignored) {
        this.currentGender = Gender.FEMALE;
        updateView();
    }

    private void onOtherClick(final View ignored) {
        setImages(OFF_IMAGE_RES, OFF_IMAGE_RES, ON_IMAGE_RES);
        ListActivity.startActivityForResult(this,
                                            ListActivity.GENDER_LIST,
                                            null,
                                            GENDER_REQUEST);
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
