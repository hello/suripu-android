package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.widget.DiagramVideoView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Logger;
import rx.functions.Action1;

public class OnboardingSimpleStepView extends RelativeLayout {
    public final OnboardingToolbar toolbar;

    public final Button primaryButton;
    public final Button secondaryButton;

    public final ScrollView contentsScrollView;
    public final LinearLayout contents;
    public final TextView headingText;
    public final TextView subheadingText;
    public /*lazy*/ ImageView diagramImage;
    public /*lazy*/ DiagramVideoView diagramVideo;


    //region Lifecycle

    public OnboardingSimpleStepView(@NonNull Fragment fragment,
                                    @NonNull LayoutInflater inflater) {
        super(fragment.getActivity());

        inflater.inflate(R.layout.view_onboarding_simple_step, this, true);

        this.toolbar = OnboardingToolbar.of(fragment, this);

        this.primaryButton = (Button) findViewById(R.id.view_onboarding_simple_step_primary);
        this.secondaryButton = (Button) findViewById(R.id.view_onboarding_simple_step_secondary);

        this.contentsScrollView = (ScrollView) findViewById(R.id.view_onboarding_simple_step_contents_scroll);
        this.contents = (LinearLayout) contentsScrollView.findViewById(R.id.view_onboarding_simple_step_contents);
        this.headingText = (TextView) contents.findViewById(R.id.view_onboarding_simple_step_heading);
        this.subheadingText = (TextView) contents.findViewById(R.id.view_onboarding_simple_step_subheading);
    }

    public OnboardingSimpleStepView configure(@NonNull Action1<OnboardingSimpleStepView> visitor) {
        visitor.call(this);
        return this;
    }

    public void destroy() {
        if (diagramVideo != null) {
            diagramVideo.destroy();
        }
    }

    //endregion


    //region Toolbar

    public OnboardingSimpleStepView hideToolbar() {
        int newTopPadding = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        headingText.setPadding(headingText.getPaddingLeft(), newTopPadding, headingText.getRight(), headingText.getBottom());
        toolbar.hide();
        return this;
    }

    public OnboardingSimpleStepView setToolbarWantsBackButton(boolean wantsBackButton) {
        toolbar.setWantsBackButton(wantsBackButton);
        return this;
    }

    public OnboardingSimpleStepView setToolbarWantsHelpButton(boolean wantsHelpButton) {
        toolbar.setWantsHelpButton(wantsHelpButton);
        return this;
    }

    public OnboardingSimpleStepView setToolbarOnHelpClickListener(@Nullable View.OnClickListener onHelpClickListener) {
        toolbar.setOnHelpClickListener(onHelpClickListener);
        return this;
    }

    public OnboardingSimpleStepView setToolbarOnHelpLongClickListener(@Nullable View.OnLongClickListener onHelpLongClickListener) {
        toolbar.setOnHelpLongClickListener(onHelpLongClickListener);
        return this;
    }

    public OnboardingSimpleStepView setCompact(boolean compact) {
        toolbar.setCompact(compact);

        Resources resources = getResources();
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


    public OnboardingSimpleStepView setPrimaryButtonText(@StringRes int resId) {
        primaryButton.setText(resId);
        return this;
    }

    public OnboardingSimpleStepView setPrimaryButtonText(@Nullable CharSequence text) {
        primaryButton.setText(text);
        return this;
    }

    public OnboardingSimpleStepView setSecondaryButtonText(@Nullable CharSequence text) {
        secondaryButton.setText(text);
        return this;
    }

    public OnboardingSimpleStepView setSecondaryButtonText(@StringRes int resId) {
        secondaryButton.setText(resId);
        return this;
    }

    public OnboardingSimpleStepView setPrimaryOnClickListener(@NonNull View.OnClickListener listener) {
        Views.setSafeOnClickListener(primaryButton, listener);
        return this;
    }

    public OnboardingSimpleStepView setSecondaryOnClickListener(@NonNull View.OnClickListener listener) {
        Views.setSafeOnClickListener(secondaryButton, listener);
        setWantsSecondaryButton(true);
        return this;
    }

    public OnboardingSimpleStepView setWantsSecondaryButton(boolean wantsSecondaryButton) {
        if (wantsSecondaryButton) {
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }
        return this;
    }

    //endregion


    //region Text

    public OnboardingSimpleStepView setHeadingText(@Nullable CharSequence text) {
        headingText.setText(text);
        return this;
    }

    public OnboardingSimpleStepView setHeadingText(@StringRes int resId) {
        headingText.setText(resId);
        return this;
    }

    public OnboardingSimpleStepView setSubheadingText(@Nullable CharSequence text) {
        subheadingText.setText(text);
        return this;
    }

    public OnboardingSimpleStepView setSubheadingText(@StringRes int resId) {
        subheadingText.setText(resId);
        return this;
    }

    public OnboardingSimpleStepView initializeSupportLinksForSubheading(@NonNull Activity fromActivity) {
        Styles.initializeSupportFooter(fromActivity, subheadingText);
        return this;
    }

    //endregion


    //region Diagram

    private void ensureDiagramImage() {
        if (diagramVideo != null) {
            throw new IllegalStateException();
        }

        if (diagramImage == null) {
            this.diagramImage = new ImageView(contents.getContext());
            diagramImage.setAdjustViewBounds(true);

            final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                               LayoutParams.WRAP_CONTENT);
            contents.addView(diagramImage, contents.getChildCount() - 1, layoutParams);
        }
    }

    private void ensureDiagramVideo() {
        if (diagramImage != null) {
            throw new IllegalStateException();
        }

        if (diagramVideo == null) {
            this.diagramVideo = new DiagramVideoView(contents.getContext());

            final LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                               LayoutParams.WRAP_CONTENT);
            contents.addView(diagramVideo, contents.getChildCount() - 1, layoutParams);
        }
    }

    public OnboardingSimpleStepView setDiagramImage(@Nullable Drawable image) {
        if (diagramVideo != null) {
            diagramVideo.setPlaceholder(image);
        } else {
            ensureDiagramImage();
            diagramImage.setImageDrawable(image);
        }
        return this;
    }

    public OnboardingSimpleStepView setDiagramImage(@DrawableRes @ColorRes int resId) {
        if (diagramVideo != null) {
            diagramVideo.setPlaceholder(resId);
        } else {
            ensureDiagramImage();
            diagramImage.setImageResource(resId);
        }
        return this;
    }

    public OnboardingSimpleStepView setDiagramInset(@DimenRes int startInsetRes, @DimenRes int endInsetRes) {
        Resources resources = contents.getResources();
        int start = startInsetRes != 0 ? resources.getDimensionPixelSize(startInsetRes) : 0;
        int end = endInsetRes != 0 ? resources.getDimensionPixelSize(endInsetRes) : 0;
        if (diagramImage != null) {
            diagramImage.setPaddingRelative(start, 0, end, 0);
        } else if (diagramVideo != null) {
            diagramVideo.setPaddingRelative(start, 0, end, 0);
        } else {
            Logger.warn(getClass().getSimpleName(), "setDiagramInset called before diagram specified");
        }
        return this;
    }

    public OnboardingSimpleStepView setDiagramEdgeToEdge(boolean edgeToEdge) {
        if (diagramVideo != null) {
            Logger.warn(getClass().getSimpleName(), "setDiagramEdgeToEdge unsupported with video");
        } else {
            ensureDiagramImage();

            if (edgeToEdge) {
                diagramImage.setAdjustViewBounds(true);
                diagramImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                diagramImage.setAdjustViewBounds(false);
                diagramImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        }
        return this;
    }

    public OnboardingSimpleStepView setDiagramVideo(@NonNull Uri video) {
        ensureDiagramVideo();
        diagramVideo.setDataSource(video);

        return this;
    }

    //endregion
}
