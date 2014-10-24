package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;

public class OnboardingStaticStepFragment extends Fragment {
    private static final String ARG_SUB_LAYOUT_ID = OnboardingStaticStepFragment.class.getName() + ".ARG_SUB_LAYOUT_ID";
    private static final String ARG_NEXT_CLASS = OnboardingStaticStepFragment.class.getName() + ".ARG_NEXT_CLASS";
    private static final String ARG_ARGUMENTS = OnboardingStaticStepFragment.class.getName() + ".ARG_ARGUMENTS";

    public static Bundle getStaticArguments(@LayoutRes int sublayoutResId,
                                            @NonNull Class<? extends Fragment> fragmentClass,
                                            @Nullable Bundle fragmentArguments) {
        Bundle arguments = new Bundle();
        arguments.putInt(ARG_SUB_LAYOUT_ID, sublayoutResId);
        arguments.putString(ARG_NEXT_CLASS, fragmentClass.getName());
        arguments.putParcelable(ARG_ARGUMENTS, fragmentArguments);
        return arguments;
    }

    public static OnboardingStaticStepFragment newInstance(@LayoutRes int sublayoutResId,
                                                           @NonNull Class<? extends Fragment> fragmentClass,
                                                           @Nullable Bundle fragmentArguments) {
        OnboardingStaticStepFragment fragment = new OnboardingStaticStepFragment();
        fragment.setArguments(getStaticArguments(sublayoutResId, fragmentClass, fragmentArguments));
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_static_step, container, false);

        ViewGroup subcontainer = (ViewGroup) view.findViewById(R.id.fragment_onboarding_static_step_container);
        inflater.inflate(getArguments().getInt(ARG_SUB_LAYOUT_ID), subcontainer, true);

        Button next = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        next.setOnClickListener(this::next);

        Button help = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        help.setOnClickListener(this::next);

        return view;
    }


    public void next(@NonNull View sender) {
        try {
            //noinspection unchecked
            Class<Fragment> fragmentClass = (Class<Fragment>) Class.forName(getArguments().getString(ARG_NEXT_CLASS));
            Fragment fragment = fragmentClass.newInstance();
            fragment.setArguments(getArguments().getParcelable(ARG_ARGUMENTS));
            ((OnboardingActivity) getActivity()).showFragment(fragment, true);
        } catch (ClassNotFoundException | java.lang.InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Could not resolve next step fragment class", e);
        }
    }
}
