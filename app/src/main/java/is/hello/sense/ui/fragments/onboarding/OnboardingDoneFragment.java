package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;
import static is.hello.sense.ui.animation.PropertyAnimatorProxy.stop;

public class OnboardingDoneFragment extends Fragment {
    private final int SHOW_SECOND_MESSAGE = 2;
    private final int SHOW_COMPLETE_MESSAGE = 3;
    private final int DELAY = 2 * 1000;

    private final Handler stepHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case SHOW_SECOND_MESSAGE: {
                    showSecondMessage();
                    break;
                }

                case SHOW_COMPLETE_MESSAGE: {
                    showCompleteMessage();
                    break;
                }
            }
        }
    };

    private TextView message;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_END, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_done, container, false);

        this.message = (TextView) view.findViewById(R.id.fragment_onboarding_done_message);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        message.setVisibility(View.VISIBLE);
        message.setAlpha(1f);

        stepHandler.sendEmptyMessageDelayed(SHOW_SECOND_MESSAGE, DELAY);
    }

    @Override
    public void onPause() {
        super.onPause();

        stepHandler.removeMessages(SHOW_SECOND_MESSAGE);
        stepHandler.removeMessages(SHOW_COMPLETE_MESSAGE);

        stop(message);
    }

    public void showSecondMessage() {
        animate(message)
                .fadeOut(View.INVISIBLE)
                .addOnAnimationCompleted(finished -> {
                    message.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.onboarding_done_moon, 0, 0);
                    message.setText(R.string.onboarding_done_message_2);
                })
                .andThen()
                .fadeIn()
                .addOnAnimationCompleted(finished -> {
                    if (finished) {
                        stepHandler.sendEmptyMessageDelayed(SHOW_COMPLETE_MESSAGE, DELAY);
                    }
                })
                .start();
    }

    public void showCompleteMessage() {
        ((OnboardingActivity) getActivity()).showHomeActivity();
    }
}
