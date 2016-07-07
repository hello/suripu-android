package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
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

import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.util.Analytics;

public final class SimpleStepFragment extends SenseFragment {
    private static final String ARG_ANALYTICS_EVENT = SimpleStepFragment.class.getName() + "ARG_ANALYTICS_EVENT";
    private static final String ARG_HEADING = SimpleStepFragment.class.getName() + "ARG_HEADING";
    private static final String ARG_SUBHEADING = SimpleStepFragment.class.getName() + "ARG_SUBHEADING";
    private static final String ARG_DIAGRAM_RES = SimpleStepFragment.class.getName() + "ARG_DIAGRAM_RES";
    private static final String ARG_DIAGRAM_VIDEO = SimpleStepFragment.class.getName() + "ARG_DIAGRAM_VIDEO";
    private static final String ARG_DIAGRAM_INSET_START_RES = SimpleStepFragment.class.getName() + "ARG_DIAGRAM_INSET_START_RES";
    private static final String ARG_DIAGRAM_INSET_END_RES = SimpleStepFragment.class.getName() + "ARG_DIAGRAM_INSET_END_RES";
    private static final String ARG_DIAGRAM_EDGE_TO_EDGE = SimpleStepFragment.class.getName() + ".ARG_DIAGRAM_EDGE_TO_EDGE";
    private static final String ARG_BUTTON_TEXT = SimpleStepFragment.class.getName() + "ARG_BUTTON_TEXT";
    private static final String ARG_HIDE_TOOLBAR = SimpleStepFragment.class.getName() + "ARG_HIDE_TOOLBAR";
    private static final String ARG_WANTS_BACK = SimpleStepFragment.class.getName() + ".ARG_WANTS_BACK";
    private static final String ARG_HELP_STEP = SimpleStepFragment.class.getName() + "ARG_HELP_STEP";
    private static final String ARG_COMPACT = SimpleStepFragment.class.getName() + ".ARG_COMPACT";
    private static final String ARG_NEXT_CLASS = SimpleStepFragment.class.getName() + ".ARG_NEXT_CLASS";
    private static final String ARG_NEXT_ARGUMENTS = SimpleStepFragment.class.getName() + ".ARG_NEXT_ARGUMENTS";
    private static final String ARG_EXIT_ANIMATION_NAME = SimpleStepFragment.class.getName() + ".ARG_EXIT_ANIMATION_NAME";
    private static final String ARG_NEXT_WANTS_BACK_STACK = SimpleStepFragment.class.getName() + ".ARG_NEXT_WANTS_BACK_STACK";

    private UserSupport.OnboardingStep helpOnboardingStep;
    private OnboardingSimpleStepView stepView;
    private @Nullable ExitAnimationProvider exitAnimationProvider;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getArguments().containsKey(ARG_ANALYTICS_EVENT)) {
            //noinspection ConstantConditions
            Analytics.trackEvent(getArguments().getString(ARG_ANALYTICS_EVENT), null);
        }

        final String animationName = getArguments().getString(ARG_EXIT_ANIMATION_NAME);
        if (!TextUtils.isEmpty(animationName) && (getActivity() instanceof ExitAnimationProviderActivity)) {
            this.exitAnimationProvider = ((ExitAnimationProviderActivity) getActivity()).getExitAnimationProviderNamed(animationName);
        }

        final String helpStepName = getArguments().getString(ARG_HELP_STEP);
        this.helpOnboardingStep = UserSupport.OnboardingStep.fromString(helpStepName);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        this.stepView = new OnboardingSimpleStepView(this, inflater);

        final Bundle arguments = getArguments();

        stepView.setHeadingText(arguments.getString(ARG_HEADING));
        stepView.setSubheadingText(arguments.getString(ARG_SUBHEADING));
        if (arguments.containsKey(ARG_DIAGRAM_VIDEO)) {
            final Uri location = Uri.parse(arguments.getString(ARG_DIAGRAM_VIDEO));
            stepView.setDiagramVideo(location);
            stepView.setDiagramInset(arguments.getInt(ARG_DIAGRAM_INSET_START_RES, 0),
                                 arguments.getInt(ARG_DIAGRAM_INSET_END_RES, 0));

            if (arguments.containsKey(ARG_DIAGRAM_RES)) {
                stepView.setDiagramImage(arguments.getInt(ARG_DIAGRAM_RES));
            }
        } else if (arguments.containsKey(ARG_DIAGRAM_RES)) {
            stepView.setDiagramImage(arguments.getInt(ARG_DIAGRAM_RES));
            stepView.setDiagramInset(arguments.getInt(ARG_DIAGRAM_INSET_START_RES, 0),
                                 arguments.getInt(ARG_DIAGRAM_INSET_END_RES, 0));
            stepView.setDiagramEdgeToEdge(arguments.getBoolean(ARG_DIAGRAM_EDGE_TO_EDGE, true));
        }

        stepView.setCompact(arguments.getBoolean(ARG_COMPACT, false));

        stepView.setPrimaryOnClickListener(this::next);
        stepView.setWantsSecondaryButton(false);

        if (arguments.getBoolean(ARG_HIDE_TOOLBAR, false)) {
            stepView.hideToolbar();
        } else {
            stepView.setToolbarWantsBackButton(arguments.getBoolean(ARG_WANTS_BACK, false));
            if (arguments.containsKey(ARG_HELP_STEP)) {
                stepView.setToolbarOnHelpClickListener(this::help);
            }
        }

        return stepView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stepView.destroy();
        this.stepView = null;
    }


    public void showNextFragment() {
        final Bundle arguments = getArguments();
        final String nextClassName = arguments.getString(ARG_NEXT_CLASS);
        if (TextUtils.isEmpty(nextClassName)) {
            getFragmentManager().popBackStack();
            return;
        }

        try {
            //noinspection unchecked
            final Class<Fragment> fragmentClass = (Class<Fragment>) Class.forName(nextClassName);
            final Fragment fragment = fragmentClass.newInstance();

            final Bundle nextArguments = arguments.getParcelable(ARG_NEXT_ARGUMENTS);
            fragment.setArguments(nextArguments);

            final boolean wantsBackStackEntry = arguments.getBoolean(ARG_NEXT_WANTS_BACK_STACK, true);
            ((FragmentNavigation) getActivity()).pushFragment(fragment, null, wantsBackStackEntry);
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve next step fragment class", e);
        }
    }

    public void next(@NonNull final View sender) {
        if (exitAnimationProvider != null) {
            exitAnimationProvider.executeAnimation(stepView, this::showNextFragment);
        } else {
            showNextFragment();
        }
    }

    public void help(@NonNull final View sender) {
        UserSupport.showForOnboardingStep(getActivity(), helpOnboardingStep);
    }


    public final static class Builder {
        private final Context context;
        private final Bundle arguments;

        public Builder(@NonNull final Context context) {
            this.context = context;
            this.arguments = new Bundle();
        }


        //region Analytics

        public Builder setAnalyticsEvent(@NonNull final String event) {
            arguments.putString(ARG_ANALYTICS_EVENT, event);
            return this;
        }

        //endregion


        //region Text & Diagrams

        public Builder setHeadingText(@Nullable final String text) {
            arguments.putString(ARG_HEADING, text);
            return this;
        }

        public Builder setHeadingText(@StringRes final int resId) {
            return setHeadingText(context.getString(resId));
        }

        public Builder setSubheadingText(@Nullable final String text) {
            arguments.putString(ARG_SUBHEADING, text);
            return this;
        }

        public Builder setSubheadingText(@StringRes final int resId) {
            return setSubheadingText(context.getString(resId));
        }

        public Builder setDiagramVideo(@Nullable final Uri location) {
            if (location != null) {
                arguments.putString(ARG_DIAGRAM_VIDEO, location.toString());
            } else {
                arguments.remove(ARG_DIAGRAM_VIDEO);
            }
            return this;
        }

        public Builder setDiagramImage(@DrawableRes @ColorRes final int resId) {
            arguments.putInt(ARG_DIAGRAM_RES, resId);
            return this;
        }

        public Builder setDiagramInset(@DimenRes final int startInsetRes, @DimenRes final int endInsetRes) {
            final Resources resources = context.getResources();
            arguments.putInt(ARG_DIAGRAM_INSET_START_RES, resources.getDimensionPixelSize(startInsetRes));
            arguments.putInt(ARG_DIAGRAM_INSET_END_RES, resources.getDimensionPixelSize(endInsetRes));
            return this;
        }

        public Builder setDiagramEdgeToEdge(final boolean edgeToEdge) {
            arguments.putBoolean(ARG_DIAGRAM_EDGE_TO_EDGE, edgeToEdge);
            return this;
        }

        //endregion


        //region Buttons & Toolbar

        public Builder setButtonText(@Nullable final String text) {
            arguments.putString(ARG_BUTTON_TEXT, text);
            return this;
        }

        public Builder setButtonText(@StringRes final int resId) {
            return setButtonText(context.getString(resId));
        }

        public Builder setHideToolbar(final boolean hideToolbar) {
            arguments.putBoolean(ARG_HIDE_TOOLBAR, hideToolbar);
            return this;
        }

        public Builder setWantsBack(final boolean wantsBack) {
            arguments.putBoolean(ARG_WANTS_BACK, wantsBack);
            return this;
        }

        public Builder setHelpStep(@NonNull final UserSupport.OnboardingStep step) {
            arguments.putString(ARG_HELP_STEP, step.toString());
            return this;
        }

        public Builder setCompact(final boolean compact) {
            arguments.putBoolean(ARG_COMPACT, compact);
            return this;
        }

        //endregion


        //region Next Fragment

        public Builder setNextFragmentClass(@NonNull final Class<? extends Fragment> nextClass) {
            arguments.putString(ARG_NEXT_CLASS, nextClass.getName());
            return this;
        }

        public Builder setNextFragmentArguments(@NonNull final Bundle nextFragmentArguments) {
            arguments.putParcelable(ARG_NEXT_ARGUMENTS, nextFragmentArguments);
            return this;
        }

        public Builder setExitAnimationName(@NonNull final String name) {
            arguments.putString(ARG_EXIT_ANIMATION_NAME, name);
            return this;
        }

        public Builder setNextWantsBackStackEntry(final boolean wantsEntry) {
            arguments.putBoolean(ARG_NEXT_WANTS_BACK_STACK, wantsEntry);
            return this;
        }

        //endregion


        public Bundle toArguments() {
            return arguments;
        }

        public SimpleStepFragment toFragment() {
            final SimpleStepFragment fragment = new SimpleStepFragment();
            fragment.setArguments(arguments);
            return fragment;
        }
    }

    /**
     * To handle variety of ExitAnimations based on name
     */
    public interface ExitAnimationProviderActivity {
        SimpleStepFragment.ExitAnimationProvider getExitAnimationProviderNamed(@NonNull final String name);
    }

    /**
     * Provides a dynamic animation on a static step's contents
     * before the step is exited by the user pressing continue.
     *
     * @see SimpleStepFragment.Builder#setExitAnimationName(String)
     */
    public interface ExitAnimationProvider {
        /**
         * Performs an animation on a given static step container.
         * <p/>
         * The provider must run the provided onComplete
         * Runnable after all animation is completed.
         *
         * @param view          The simple step view whose contents must be animated.
         * @param onComplete    The completion handler provided by the static step fragment.
         *
         * @see OnboardingSimpleStepView
         */
        void executeAnimation(@NonNull OnboardingSimpleStepView view, @NonNull Runnable onComplete);
    }
}
