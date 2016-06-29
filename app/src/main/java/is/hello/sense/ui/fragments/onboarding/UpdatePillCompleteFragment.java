package is.hello.sense.ui.fragments.onboarding;


import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public class UpdatePillCompleteFragment extends Fragment {

    private TextView message;

    public static Fragment newInstance() {
        return new UpdatePillCompleteFragment();
    }

    public UpdatePillCompleteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_register_complete, container, false);
        this.message = (TextView) view.findViewById(R.id.fragment_onboarding_done_message);
        this.message.setText(R.string.message_sleep_pill_updated);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        animatorFor(message)
                .setDuration(2000)
                .withStartDelay(1000)
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(isComplete -> {
                    if (isComplete) {
                        complete();
                    }
                })
                .start();
    }

    public void complete() {
        ((OnboardingActivity) getActivity()).showHomeActivity(OnboardingActivity.FLOW_REGISTER);
    }
}


