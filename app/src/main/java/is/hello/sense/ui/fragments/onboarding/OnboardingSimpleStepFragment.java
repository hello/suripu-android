package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Analytics;

public final class OnboardingSimpleStepFragment extends SenseFragment {
    private static final String ARG_ANALYTICS_EVENT = OnboardingSimpleStepFragment.class.getName() + "ARG_ANALYTICS_EVENT";
    private static final String ARG_HEADING = OnboardingSimpleStepFragment.class.getName() + "ARG_HEADING";
    private static final String ARG_SUBHEADING = OnboardingSimpleStepFragment.class.getName() + "ARG_SUBHEADING";
    private static final String ARG_DIAGRAM_RES = OnboardingSimpleStepFragment.class.getName() + "ARG_DIAGRAM_RES";
    private static final String ARG_DIAGRAM_INSET_START_RES = OnboardingSimpleStepFragment.class.getName() + "ARG_DIAGRAM_INSET_START_RES";
    private static final String ARG_DIAGRAM_INSET_END_RES = OnboardingSimpleStepFragment.class.getName() + "ARG_DIAGRAM_INSET_END_RES";
    private static final String ARG_DIAGRAM_EDGE_TO_EDGE = OnboardingSimpleStepFragment.class.getName() + ".ARG_DIAGRAM_EDGE_TO_EDGE";
    private static final String ARG_BUTTON_TEXT = OnboardingSimpleStepFragment.class.getName() + "ARG_BUTTON_TEXT";
    private static final String ARG_HIDE_TOOLBAR = OnboardingSimpleStepFragment.class.getName() + "ARG_HIDE_TOOLBAR";
    private static final String ARG_WANTS_BACK = OnboardingSimpleStepFragment.class.getName() + ".ARG_WANTS_BACK";
    private static final String ARG_HELP_STEP = OnboardingSimpleStepFragment.class.getName() + "ARG_HELP_STEP";
    private static final String ARG_COMPACT = OnboardingSimpleStepFragment.class.getName() + ".ARG_COMPACT";
    private static final String ARG_NEXT_CLASS = OnboardingSimpleStepFragment.class.getName() + ".ARG_NEXT_CLASS";
    private static final String ARG_NEXT_ARGUMENTS = OnboardingSimpleStepFragment.class.getName() + ".ARG_NEXT_ARGUMENTS";
    private static final String ARG_EXIT_ANIMATION_NAME = OnboardingSimpleStepFragment.class.getName() + ".ARG_EXIT_ANIMATION_NAME";
    private static final String ARG_NEXT_WANTS_BACK_STACK = OnboardingSimpleStepFragment.class.getName() + ".ARG_NEXT_WANTS_BACK_STACK";

    private UserSupport.OnboardingStep helpOnboardingStep;
    private OnboardingSimpleStepViewBuilder simpleStepHolder;
    private @Nullable ExitAnimationProvider exitAnimationProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getArguments().containsKey(ARG_ANALYTICS_EVENT)) {
            //noinspection ConstantConditions
            Analytics.trackEvent(getArguments().getString(ARG_ANALYTICS_EVENT), null);
        }

        String animationName = getArguments().getString(ARG_EXIT_ANIMATION_NAME);
        if (!TextUtils.isEmpty(animationName)) {
            OnboardingActivity onboardingActivity = (OnboardingActivity) getActivity();
            this.exitAnimationProvider = onboardingActivity.getExitAnimationProviderNamed(animationName);
        }

        String helpStepName = getArguments().getString(ARG_HELP_STEP);
        this.helpOnboardingStep = UserSupport.OnboardingStep.fromString(helpStepName);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.simpleStepHolder = new OnboardingSimpleStepViewBuilder(this, inflater, container);

        simpleStepHolder.setHeadingText(getArguments().getString(ARG_HEADING));
        simpleStepHolder.setSubheadingText(getArguments().getString(ARG_SUBHEADING));
        if (getArguments().containsKey(ARG_DIAGRAM_RES)) {
            simpleStepHolder.setDiagramImage(getArguments().getInt(ARG_DIAGRAM_RES));
            simpleStepHolder.setDiagramInset(getArguments().getInt(ARG_DIAGRAM_INSET_START_RES, 0),
                                             getArguments().getInt(ARG_DIAGRAM_INSET_END_RES, 0));
            simpleStepHolder.setDiagramEdgeToEdge(getArguments().getBoolean(ARG_DIAGRAM_EDGE_TO_EDGE, true));
        }

        simpleStepHolder.setCompact(getArguments().getBoolean(ARG_COMPACT, false));

        simpleStepHolder.setPrimaryOnClickListener(this::next);
        simpleStepHolder.setWantsSecondaryButton(false);

        if (getArguments().getBoolean(ARG_HIDE_TOOLBAR, false)) {
            simpleStepHolder.hideToolbar();
        } else {
            simpleStepHolder.setToolbarWantsBackButton(getArguments().getBoolean(ARG_WANTS_BACK, false));
            if (getArguments().containsKey(ARG_HELP_STEP)) {
                simpleStepHolder.setToolbarOnHelpClickListener(this::help);
            }
        }

        return simpleStepHolder.create();
    }


    public void showNextFragment() {
        try {
            //noinspection unchecked
            Class<Fragment> fragmentClass = (Class<Fragment>) Class.forName(getArguments().getString(ARG_NEXT_CLASS));
            Fragment fragment = fragmentClass.newInstance();

            Bundle arguments = getArguments().getParcelable(ARG_NEXT_ARGUMENTS);
            fragment.setArguments(arguments);

            boolean wantsBackStackEntry = getArguments().getBoolean(ARG_NEXT_WANTS_BACK_STACK, true);
            ((OnboardingActivity) getActivity()).pushFragment(fragment, null, wantsBackStackEntry);
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve next step fragment class", e);
        }
    }

    public void next(@NonNull View sender) {
        if (exitAnimationProvider != null) {
            exitAnimationProvider.executeAnimation(simpleStepHolder, this::showNextFragment);
        } else {
            showNextFragment();
        }
    }

    public void help(@NonNull View sender) {
        UserSupport.showForOnboardingStep(getActivity(), helpOnboardingStep);
    }


    public final static class Builder {
        private final Context context;
        private final Bundle arguments;

        public Builder(@NonNull Context context) {
            this.context = context;
            this.arguments = new Bundle();
        }


        //region Analytics

        public Builder setAnalyticsEvent(@NonNull String event) {
            arguments.putString(ARG_ANALYTICS_EVENT, event);
            return this;
        }

        //endregion


        //region Text & Diagrams

        public Builder setHeadingText(@Nullable String text) {
            arguments.putString(ARG_HEADING, text);
            return this;
        }

        public Builder setHeadingText(@StringRes int resId) {
            return setHeadingText(context.getString(resId));
        }

        public Builder setSubheadingText(@Nullable String text) {
            arguments.putString(ARG_SUBHEADING, text);
            return this;
        }

        public Builder setSubheadingText(@StringRes int resId) {
            return setSubheadingText(context.getString(resId));
        }

        public Builder setDiagramImage(@DrawableRes @ColorRes int resId) {
            arguments.putInt(ARG_DIAGRAM_RES, resId);
            return this;
        }

        public Builder setDiagramInset(@DimenRes int startInsetRes, @DimenRes int endInsetRes) {
            Resources resources = context.getResources();
            arguments.putInt(ARG_DIAGRAM_INSET_START_RES, resources.getDimensionPixelSize(startInsetRes));
            arguments.putInt(ARG_DIAGRAM_INSET_END_RES, resources.getDimensionPixelSize(endInsetRes));
            return this;
        }

        public Builder setDiagramEdgeToEdge(boolean edgeToEdge) {
            arguments.putBoolean(ARG_DIAGRAM_EDGE_TO_EDGE, edgeToEdge);
            return this;
        }

        //endregion


        //region Buttons & Toolbar

        public Builder setButtonText(@Nullable String text) {
            arguments.putString(ARG_BUTTON_TEXT, text);
            return this;
        }

        public Builder setButtonText(@StringRes int resId) {
            return setButtonText(context.getString(resId));
        }

        public Builder setHideToolbar(boolean hideToolbar) {
            arguments.putBoolean(ARG_HIDE_TOOLBAR, hideToolbar);
            return this;
        }

        public Builder setWantsBack(boolean wantsBack) {
            arguments.putBoolean(ARG_WANTS_BACK, wantsBack);
            return this;
        }

        public Builder setHelpStep(@NonNull UserSupport.OnboardingStep step) {
            arguments.putString(ARG_HELP_STEP, step.toString());
            return this;
        }

        public Builder setCompact(boolean compact) {
            arguments.putBoolean(ARG_COMPACT, compact);
            return this;
        }

        //endregion


        //region Next Fragment

        public Builder setNextFragmentClass(@NonNull Class<? extends Fragment> nextClass) {
            arguments.putString(ARG_NEXT_CLASS, nextClass.getName());
            return this;
        }

        public Builder setNextFragmentArguments(@NonNull Bundle nextFragmentArguments) {
            arguments.putParcelable(ARG_NEXT_ARGUMENTS, nextFragmentArguments);
            return this;
        }

        public Builder setExitAnimationName(@NonNull String name) {
            arguments.putString(ARG_EXIT_ANIMATION_NAME, name);
            return this;
        }

        public Builder setNextWantsBackStackEntry(boolean wantsEntry) {
            arguments.putBoolean(ARG_NEXT_WANTS_BACK_STACK, wantsEntry);
            return this;
        }

        //endregion


        public Bundle toArguments() {
            return arguments;
        }

        public OnboardingSimpleStepFragment toFragment() {
            OnboardingSimpleStepFragment fragment = new OnboardingSimpleStepFragment();
            fragment.setArguments(arguments);
            return fragment;
        }
    }


    /**
     * Provides a dynamic animation on a static step's contents
     * before the step is exited by the user pressing continue.
     *
     * @see is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepFragment.Builder#setExitAnimationName(String)
     */
    public interface ExitAnimationProvider {
        /**
         * Performs an animation on a given static step container.
         * <p/>
         * The provider must run the provided onComplete
         * Runnable after all animation is completed.
         *
         * @param holder        The simple step holder whose contents must be animated.
         * @param onComplete    The completion handler provided by the static step fragment.
         *
         * @see is.hello.sense.ui.fragments.onboarding.OnboardingSimpleStepViewBuilder
         */
        void executeAnimation(@NonNull OnboardingSimpleStepViewBuilder holder, @NonNull Runnable onComplete);
    }
}
