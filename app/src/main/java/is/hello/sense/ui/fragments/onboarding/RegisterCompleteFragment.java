package is.hello.sense.ui.fragments.onboarding;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.segment.analytics.Properties;

import java.lang.ref.WeakReference;

import is.hello.sense.R;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.util.Analytics;

import static is.hello.go99.Anime.cancelAll;

public class RegisterCompleteFragment extends Fragment {

    private final StepHandler stepHandler = new StepHandler(this);

    private TextView message;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final Properties properties = Analytics.createBluetoothTrackingProperties(getActivity());
            Analytics.trackEvent(Analytics.Onboarding.EVENT_END, properties);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_register_complete, container, false);
        this.message = (TextView) view.findViewById(R.id.fragment_onboarding_done_message);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void onPause() {
        super.onPause();

        stepHandler.cancelPending();

        cancelAll(message);
    }

    private void init() {
        message.setVisibility(View.VISIBLE);
        message.setAlpha(1f);
        stepHandler.postShowComplete();

    }

    public void complete() {
        ((OnboardingActivity) getActivity()).showHomeActivity(OnboardingActivity.FLOW_REGISTER);
    }


    static class StepHandler extends Handler {
        static final int SHOW_COMPLETE_MESSAGE = 3;
        static final int DELAY = 2 * 1000;

        private final WeakReference<RegisterCompleteFragment> fragment;

        public StepHandler(@NonNull final RegisterCompleteFragment fragment) {
            super(Looper.getMainLooper());
            this.fragment = new WeakReference<>(fragment);
        }

        void cancelPending() {
            removeMessages(SHOW_COMPLETE_MESSAGE);
        }

        void postShowComplete() {
            sendEmptyMessageDelayed(SHOW_COMPLETE_MESSAGE, DELAY);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {

                case SHOW_COMPLETE_MESSAGE: {
                    final RegisterCompleteFragment fragment = this.fragment.get();
                    if (fragment != null) {
                        fragment.complete();
                    }
                    break;
                }

                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
}
