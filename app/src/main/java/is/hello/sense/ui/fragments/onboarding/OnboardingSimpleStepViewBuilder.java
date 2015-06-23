package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
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
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import rx.functions.Action1;

import static android.view.ViewGroup.MarginLayoutParams;

public final class OnboardingSimpleStepViewBuilder {
    private final ViewGroup stepView;

    public final OnboardingToolbar toolbar;

    public final Button primaryButton;
    public final Button secondaryButton;

    public final ViewGroup contents;
    public final TextView headingText;
    public final TextView subheadingText;
    public final ImageView diagramImage;


    //region Lifecycle

    public OnboardingSimpleStepViewBuilder(@NonNull Fragment fragment, @NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        this.stepView = (ViewGroup) inflater.inflate(R.layout.fragment_onboarding_simple_step, container, false);

        this.toolbar = OnboardingToolbar.of(fragment, stepView);

        this.primaryButton = (Button) stepView.findViewById(R.id.fragment_onboarding_simple_step_primary);
        this.secondaryButton = (Button) stepView.findViewById(R.id.fragment_onboarding_simple_step_secondary);

        this.contents = (ViewGroup) stepView.findViewById(R.id.fragment_onboarding_simple_step_contents);
        this.headingText = (TextView) stepView.findViewById(R.id.fragment_onboarding_simple_step_heading);
        this.subheadingText = (TextView) stepView.findViewById(R.id.fragment_onboarding_simple_step_subheading);
        this.diagramImage = (ImageView) stepView.findViewById(R.id.fragment_onboarding_simple_step_diagram);
    }

    public OnboardingSimpleStepViewBuilder configure(@NonNull Action1<OnboardingSimpleStepViewBuilder> visitor) {
        visitor.call(this);
        return this;
    }

    public @NonNull ViewGroup create() {
        return stepView;
    }

    //endregion


    //region Toolbar

    public OnboardingSimpleStepViewBuilder hideToolbar() {
        int newTopPadding = stepView.getResources().getDimensionPixelSize(R.dimen.gap_outer);
        headingText.setPadding(headingText.getPaddingLeft(), newTopPadding, headingText.getRight(), headingText.getBottom());
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

    public OnboardingSimpleStepViewBuilder setCompact(boolean compact) {
        toolbar.setCompact(compact);

        Resources resources = stepView.getResources();
        int newBottomMargin;
        if (compact) {
            newBottomMargin = resources.getDimensionPixelSize(R.dimen.gap_small);
        } else {
            newBottomMargin = resources.getDimensionPixelSize(R.dimen.gap_outer);
        }
        ((MarginLayoutParams) primaryButton.getLayoutParams()).bottomMargin = newBottomMargin;
        primaryButton.invalidate();

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

    public OnboardingSimpleStepViewBuilder initializeSupportLinksForSubheading(@NonNull Activity fromActivity) {
        Styles.initializeSupportFooter(fromActivity, subheadingText);
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

    public OnboardingSimpleStepViewBuilder setDiagramInset(@DimenRes int startInsetRes, @DimenRes int endInsetRes) {
        Resources resources = diagramImage.getResources();
        int start = startInsetRes != 0 ? resources.getDimensionPixelSize(startInsetRes) : 0;
        int end = endInsetRes != 0 ? resources.getDimensionPixelSize(endInsetRes) : 0;
        diagramImage.setPaddingRelative(start, 0, end, 0);
        return this;
    }

    public OnboardingSimpleStepViewBuilder setDiagramEdgeToEdge(boolean edgeToEdge) {
        if (edgeToEdge) {
            diagramImage.setAdjustViewBounds(true);
            diagramImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            diagramImage.setAdjustViewBounds(false);
            diagramImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }
        return this;
    }

    //endregion
}
