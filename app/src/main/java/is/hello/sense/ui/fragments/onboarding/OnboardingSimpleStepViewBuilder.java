package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.widget.util.Views;

public final class OnboardingSimpleStepViewBuilder {
    private final ViewGroup stepView;

    private final OnboardingToolbar toolbar;

    private final Button primaryButton;
    private final Button secondaryButton;

    private final TextView headingText;
    private final TextView subheadingText;

    private final ImageView diagramImage;


    //region Lifecycle

    public OnboardingSimpleStepViewBuilder(@NonNull Fragment fragment, @NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        this.stepView = (ViewGroup) inflater.inflate(R.layout.fragment_onboarding_simple_step, container, false);

        this.toolbar = OnboardingToolbar.of(fragment, stepView);

        this.primaryButton = (Button) stepView.findViewById(R.id.fragment_onboarding_simple_step_primary);
        this.secondaryButton = (Button) stepView.findViewById(R.id.fragment_onboarding_simple_step_secondary);

        this.headingText = (TextView) stepView.findViewById(R.id.fragment_onboarding_simple_step_heading);
        this.subheadingText = (TextView) stepView.findViewById(R.id.fragment_onboarding_simple_step_subheading);

        this.diagramImage = (ImageView) stepView.findViewById(R.id.fragment_onboarding_simple_step_diagram);
    }

    public @NonNull ViewGroup create() {
        return stepView;
    }

    //endregion


    //region Toolbar

    public OnboardingSimpleStepViewBuilder hideToolbar() {
        toolbar.hide();
        return this;
    }

    public OnboardingSimpleStepViewBuilder setToolbarWantsBackButton(boolean wantsBackButton) {
        toolbar.setWantsBackButton(wantsBackButton);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setToolbarWantsHelpButton(boolean wantsHelpButton) {
        toolbar.setWantsHelpButton(wantsHelpButton);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setToolbarOnHelpClickListener(@Nullable View.OnClickListener onHelpClickListener) {
        toolbar.setOnHelpClickListener(onHelpClickListener);
        return this;
    }


    //endregion


    //region Buttons


    public OnboardingSimpleStepViewBuilder setPrimaryButtonText(@StringRes int resId) {
        primaryButton.setText(resId);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setPrimaryButtonText(@Nullable CharSequence text) {
        primaryButton.setText(text);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setSecondaryButtonText(@Nullable CharSequence text) {
        secondaryButton.setText(text);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setSecondaryButtonText(@StringRes int resId) {
        secondaryButton.setText(resId);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setPrimaryOnClickListener(@NonNull View.OnClickListener listener) {
        Views.setSafeOnClickListener(primaryButton, listener);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setSecondaryOnClickListener(@NonNull View.OnClickListener listener) {
        Views.setSafeOnClickListener(secondaryButton, listener);
        setWantsSecondaryButton(true);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setWantsSecondaryButton(boolean wantsSecondaryButton) {
        if (wantsSecondaryButton) {
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }
        return this;
    }

    //endregion


    //region Text

    public OnboardingSimpleStepViewBuilder setHeadingText(@Nullable CharSequence text) {
        headingText.setText(text);
        diagramImage.setContentDescription(text);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setHeadingText(@StringRes int resId) {
        headingText.setText(resId);
        diagramImage.setContentDescription(diagramImage.getResources().getText(resId));
        return this;
    }

    public OnboardingSimpleStepViewBuilder setSubheadingText(@Nullable CharSequence text) {
        subheadingText.setText(text);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setSubheadingText(@StringRes int resId) {
        subheadingText.setText(resId);
        return this;
    }

    //endregion


    //region Diagram

    public OnboardingSimpleStepViewBuilder setDiagramImage(@Nullable Drawable image) {
        diagramImage.setImageDrawable(image);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setDiagramImage(@DrawableRes @ColorRes int resId) {
        diagramImage.setImageResource(resId);
        return this;
    }

    //endregion
}
