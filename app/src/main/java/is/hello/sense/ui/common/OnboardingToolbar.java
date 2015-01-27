package is.hello.sense.ui.common;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;

public class OnboardingToolbar {
    private final Fragment fragment;
    private final FrameLayout toolbarView;

    private final View backButton;
    private final View helpButton;

    private @Nullable View.OnClickListener onHelpClickListener;

    public static OnboardingToolbar of(@NonNull Fragment fragment, @NonNull View view) {
        return new OnboardingToolbar(fragment, view.findViewById(R.id.sub_fragment_onboarding_toolbar));
    }

    private OnboardingToolbar(@NonNull Fragment fragment, @NonNull View toolbarView) {
        this.fragment = fragment;
        this.toolbarView = (FrameLayout) toolbarView;

        this.backButton = toolbarView.findViewById(R.id.sub_fragment_onboarding_toolbar_back);
        Views.setSafeOnClickListener(backButton, this::onBack);

        this.helpButton = toolbarView.findViewById(R.id.sub_fragment_onboarding_toolbar_help);
        Views.setSafeOnClickListener(helpButton, this::onHelp);

        setWantsBackButton(false);
        setWantsHelpButton(false);
    }


    private void onBack(View view) {
        Activity activity = fragment.getActivity();
        if (activity != null) {
            View focusView = activity.getCurrentFocus();
            if (focusView != null && focusView instanceof EditText) {
                InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(focusView.getWindowToken(), 0);
            }

            activity.onBackPressed();

            Analytics.trackEvent(Analytics.Onboarding.EVENT_BACK, null);
        }
    }

    private void onHelp(View view) {
        if (onHelpClickListener != null) {
            onHelpClickListener.onClick(view);
        }
    }


    public OnboardingToolbar hide() {
        toolbarView.setVisibility(View.GONE);

        return this;
    }

    public OnboardingToolbar setWantsBackButton(boolean wantsBackButton) {
        if (wantsBackButton) {
            backButton.setVisibility(View.VISIBLE);
        } else {
            backButton.setVisibility(View.GONE);
        }

        return this;
    }

    public OnboardingToolbar setWantsHelpButton(boolean wantsHelpButton) {
        if (wantsHelpButton) {
            helpButton.setVisibility(View.VISIBLE);
        } else {
            helpButton.setVisibility(View.GONE);
        }

        return this;
    }

    public OnboardingToolbar setOnHelpClickListener(@Nullable View.OnClickListener onHelpClickListener) {
        this.onHelpClickListener = onHelpClickListener;
        setWantsHelpButton(onHelpClickListener != null);

        return this;
    }

    public OnboardingToolbar replaceHelpButton(@NonNull View view) {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        toolbarView.removeViewAt(1);
        toolbarView.addView(view, layoutParams);
        return this;
    }
}
