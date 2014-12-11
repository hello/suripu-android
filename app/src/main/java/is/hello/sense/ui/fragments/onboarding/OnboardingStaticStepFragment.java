package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.HelpUtil;
import is.hello.sense.util.Analytics;

public class OnboardingStaticStepFragment extends Fragment {
    private static final String ARG_SUB_LAYOUT_ID = OnboardingStaticStepFragment.class.getName() + ".ARG_SUB_LAYOUT_ID";
    private static final String ARG_NEXT_CLASS = OnboardingStaticStepFragment.class.getName() + ".ARG_NEXT_CLASS";
    private static final String ARG_NEXT_ARGUMENTS = OnboardingStaticStepFragment.class.getName() + ".ARG_NEXT_ARGUMENTS";
    private static final String ARG_ANALYTICS_EVENT = OnboardingStaticStepFragment.class.getName() + ".ARG_ANALYTICS_EVENT";
    private static final String ARG_HIDE_HELP = OnboardingStaticStepFragment.class.getName() + ".ARG_HIDE_HELP";
    private static final String ARG_EXIT_ANIMATION_NAME = OnboardingStaticStepFragment.class.getName() + ".ARG_EXIT_ANIMATION_NAME";

    private LinearLayout container;
    private @Nullable ExitAnimationProvider exitAnimationProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getArguments().containsKey(ARG_ANALYTICS_EVENT)) {
            Analytics.event(getArguments().getString(ARG_ANALYTICS_EVENT), null);
        }

        String animationName = getArguments().getString(ARG_EXIT_ANIMATION_NAME);
        if (!TextUtils.isEmpty(animationName)) {
            OnboardingActivity onboardingActivity = (OnboardingActivity) getActivity();
            this.exitAnimationProvider = onboardingActivity.getExitAnimationProviderNamed(animationName);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_static_step, container, false);

        this.container = (LinearLayout) view.findViewById(R.id.fragment_onboarding_static_step_container);

        ViewGroup contentContainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_static_step_content);
        inflater.inflate(getArguments().getInt(ARG_SUB_LAYOUT_ID), contentContainer, true);

        Button next = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        next.setOnClickListener(this::next);

        Button help = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        if (getArguments().getBoolean(ARG_HIDE_HELP, false)) {
            help.setVisibility(View.GONE);
        } else {
            help.setOnClickListener(this::help);
        }

        return view;
    }


    public void showNextFragment() {
        try {
            //noinspection unchecked
            Class<Fragment> fragmentClass = (Class<Fragment>) Class.forName(getArguments().getString(ARG_NEXT_CLASS));
            Fragment fragment = fragmentClass.newInstance();
            fragment.setArguments(getArguments().getParcelable(ARG_NEXT_ARGUMENTS));
            ((OnboardingActivity) getActivity()).showFragment(fragment, null, true);
        } catch (ClassNotFoundException | java.lang.InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not resolve next step fragment class", e);
        }
    }

    public void next(@NonNull View sender) {
        if (exitAnimationProvider != null) {
            exitAnimationProvider.executeAnimation(container, this::showNextFragment);
        } else {
            showNextFragment();
        }
    }

    public void help(@NonNull View sender) {
        HelpUtil.showHelp(getActivity());
    }


    public static class Builder {
        public final Bundle arguments = new Bundle();

        public Builder setLayout(@LayoutRes int layout) {
            arguments.putInt(ARG_SUB_LAYOUT_ID, layout);
            return this;
        }

        public Builder setNextFragmentClass(@NonNull Class<? extends Fragment> nextClass) {
            arguments.putString(ARG_NEXT_CLASS, nextClass.getName());
            return this;
        }

        public Builder setNextFragmentArguments(@NonNull Bundle nextFragmentArguments) {
            arguments.putParcelable(ARG_NEXT_ARGUMENTS, nextFragmentArguments);
            return this;
        }

        public Builder setAnalyticsEvent(@NonNull String event) {
            arguments.putString(ARG_ANALYTICS_EVENT, event);
            return this;
        }

        public Builder setHideHelp(boolean hideHelp) {
            arguments.putBoolean(ARG_HIDE_HELP, hideHelp);
            return this;
        }

        public Builder setExitAnimationName(@NonNull String name) {
            arguments.putString(ARG_EXIT_ANIMATION_NAME, name);
            return this;
        }

        public @NonNull OnboardingStaticStepFragment build() {
            OnboardingStaticStepFragment fragment = new OnboardingStaticStepFragment();
            fragment.setArguments(arguments);
            return fragment;
        }
    }


    /**
     * Provides a dynamic animation on a static step's contents
     * before the step is exited by the user pressing continue.
     *
     * @see is.hello.sense.ui.fragments.onboarding.OnboardingStaticStepFragment.Builder#setExitAnimationName(String)
     */
    public interface ExitAnimationProvider {
        /**
         * Performs an animation on a given static step container.
         * <p/>
         * The provider must run the provided onComplete
         * Runnable after all animation is completed.
         *
         * @param container     The container to perform the animation on.
         * @param onComplete    The completion handler provided by the static step fragment.
         *
         * @see is.hello.sense.R.id#fragment_onboarding_step_continue
         * @see is.hello.sense.R.id#fragment_onboarding_step_help
         */
        void executeAnimation(@NonNull LinearLayout container, @NonNull Runnable onComplete);
    }
}
