package is.hello.sense.ui.common;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Drawables;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.Distribution;

public class OnboardingToolbar {
    private Fragment fragment;
    private final FrameLayout toolbarView;

    private final ImageButton backButton;
    private final ImageButton helpButton;

    private
    @Nullable
    View.OnClickListener onHelpClickListener;
    private
    @Nullable
    View.OnLongClickListener onHelpLongClickListener;

    public static OnboardingToolbar of(@NonNull final Fragment fragment, @NonNull final View view) {
        return new OnboardingToolbar(fragment, view.findViewById(R.id.sub_fragment_onboarding_toolbar));
    }

    public void onDestroyView() {
        backButton.setOnClickListener(null);
        helpButton.setOnClickListener(null);
        helpButton.setOnLongClickListener(null);
        onHelpClickListener = null;
        onHelpLongClickListener = null;
        fragment = null;
    }

    private OnboardingToolbar(@NonNull final Fragment fragment, @NonNull final View toolbarView) {
        this.fragment = fragment;
        this.toolbarView = (FrameLayout) toolbarView;

        final Context context = fragment.getActivity();
        this.backButton = (ImageButton) toolbarView.findViewById(R.id.sub_fragment_onboarding_toolbar_back);
        final Drawable backIcon = backButton.getDrawable().mutate();
        Drawables.setTintColor(backIcon, ContextCompat.getColor(context, R.color.light_accent));
        backButton.setImageDrawable(backIcon);
        Views.setSafeOnClickListener(backButton, this::onBack);

        this.helpButton = (ImageButton) toolbarView.findViewById(R.id.sub_fragment_onboarding_toolbar_help);
        final Drawable helpIcon = helpButton.getDrawable().mutate();
        Drawables.setTintColor(helpIcon, ContextCompat.getColor(context, R.color.light_accent));
        helpButton.setImageDrawable(helpIcon);
        Views.setSafeOnClickListener(helpButton, this::onHelp);
        helpButton.setOnLongClickListener(this::onHelpLongClick);

        setWantsBackButton(false);
        setWantsHelpButton(false);
    }

    private void onBack(final View view) {
        final Activity activity = fragment.getActivity();
        if (activity != null) {
            final View focusView = activity.getCurrentFocus();
            if (focusView != null && focusView instanceof EditText) {
                final InputMethodManager inputMethodManager =
                        (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            }

            activity.onBackPressed();

            Analytics.trackEvent(Analytics.Onboarding.EVENT_BACK, null);
        }
    }

    private void onHelp(final View view) {
        if (onHelpClickListener != null) {
            onHelpClickListener.onClick(view);
        }
    }

    private boolean onHelpLongClick(final View view) {
        if (onHelpLongClickListener != null) {
            return onHelpLongClickListener.onLongClick(view);
        } else if (BuildConfig.DEBUG_SCREEN_ENABLED) {
            final Activity activity = fragment.getActivity();
            if (activity != null) {
                Distribution.startDebugActivity(activity);
            }
            return true;
        } else {
            return false;
        }
    }


    public OnboardingToolbar hide() {
        toolbarView.setVisibility(View.GONE);

        return this;
    }

    public OnboardingToolbar setVisible(final boolean visible) {
        toolbarView.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);

        return this;
    }

    public OnboardingToolbar setWantsBackButton(final boolean wantsBackButton) {
        if (wantsBackButton) {
            backButton.setVisibility(View.VISIBLE);
        } else {
            backButton.setVisibility(View.GONE);
        }

        return this;
    }

    public OnboardingToolbar setWantsHelpButton(final boolean wantsHelpButton) {
        if (wantsHelpButton) {
            helpButton.setVisibility(View.VISIBLE);
        } else {
            helpButton.setVisibility(View.GONE);
        }

        return this;
    }

    public OnboardingToolbar setOnHelpClickListener(@Nullable final View.OnClickListener onHelpClickListener) {
        this.onHelpClickListener = onHelpClickListener;
        setWantsHelpButton(onHelpClickListener != null);

        return this;
    }

    public OnboardingToolbar setOnHelpLongClickListener(@Nullable final View.OnLongClickListener onHelpLongClickListener) {
        this.onHelpLongClickListener = onHelpLongClickListener;
        setWantsHelpButton(onHelpClickListener != null);

        return this;
    }

    public OnboardingToolbar setCompact(final boolean compact) {
        final Resources resources = toolbarView.getResources();
        if (compact) {
            toolbarView.getLayoutParams().height = resources.getDimensionPixelSize(R.dimen.action_bar_height_compact);
        } else {
            toolbarView.getLayoutParams().height = resources.getDimensionPixelSize(R.dimen.action_bar_height);
        }
        toolbarView.invalidate();

        return this;
    }

    public OnboardingToolbar replaceHelpButton(@NonNull final View view) {
        final FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                                             ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        toolbarView.removeViewAt(1);
        toolbarView.addView(view, layoutParams);
        return this;
    }

    public OnboardingToolbar setHelpButtonIcon(@DrawableRes final int iconRes){
        helpButton.setImageResource(iconRes);
        return this;
    }

    public OnboardingToolbar setDark(final boolean isDark) {
        final Resources resources = fragment.getResources();
        final @ColorInt int tintColor;
        final @ColorInt int backgroundColor;
        if (isDark) {
            tintColor = resources.getColor(R.color.white);
            backgroundColor = resources.getColor(R.color.light_accent);
        } else {
            tintColor = resources.getColor(R.color.light_accent);
            backgroundColor = Color.TRANSPARENT;
        }

        final Drawable backIcon = backButton.getDrawable().mutate();
        Drawables.setTintColor(backIcon, tintColor);
        backButton.setImageDrawable(backIcon);

        final Drawable helpIcon = helpButton.getDrawable().mutate();
        Drawables.setTintColor(helpIcon, tintColor);
        helpButton.setImageDrawable(helpIcon);

        toolbarView.setBackgroundColor(backgroundColor);

        return this;
    }
}
