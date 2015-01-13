package is.hello.sense.ui.common;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import is.hello.sense.R;
import is.hello.sense.ui.widget.util.Views;

public class OnboardingToolbar {
    private final FragmentManager fragmentManager;
    private final View toolbarView;

    private final View backButton;
    private final View helpButton;

    private @Nullable View.OnClickListener onHelpClickListener;

    public static OnboardingToolbar of(@NonNull Fragment fragment, @NonNull View view) {
        return new OnboardingToolbar(fragment.getFragmentManager(), view.findViewById(R.id.sub_fragment_onboarding_toolbar));
    }

    private OnboardingToolbar(FragmentManager fragmentManager, View toolbarView) {
        this.fragmentManager = fragmentManager;
        this.toolbarView = toolbarView;

        this.backButton = toolbarView.findViewById(R.id.sub_fragment_onboarding_toolbar_back);
        Views.setSafeOnClickListener(backButton, this::onBack);

        this.helpButton = toolbarView.findViewById(R.id.sub_fragment_onboarding_toolbar_help);
        Views.setSafeOnClickListener(helpButton, this::onHelp);

        setWantsBackButton(false);
        setWantsHelpButton(false);
    }


    private void onBack(View view) {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
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
}
