package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.OnBackPressedInterceptor;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.ViewAnimator;
import is.hello.sense.util.Analytics;

/**
 * Show user explanation for requiring location permission
 */
public class SetLocationFragment extends SenseFragment
        implements OnBackPressedInterceptor {

    private final ViewAnimator viewAnimator = new ViewAnimator();
    private static final String ARG_SHOW_SKIP = SetLocationFragment.class.getSimpleName() + ".ARG_SHOW_SKIP";

    /**
     * @param showSkip true will display the 'Skip Sense Setup' action.
     * @return fragment that will allow user to enable location permission.
     */
    public static SetLocationFragment newInstance(final boolean showSkip) {
        final Bundle args = new Bundle();
        args.putBoolean(ARG_SHOW_SKIP, showSkip);
        final SetLocationFragment fragment = new SetLocationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private OnboardingSimpleStepView view;
    private final LocationPermission permission = new LocationPermission(this);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_LOCATION, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        final boolean showSkip;
        if (getArguments() != null && getArguments().getBoolean(ARG_SHOW_SKIP)) {
            showSkip = true;
        } else {
            showSkip = false;
        }
        final View animatedView = viewAnimator.inflateView(inflater, container, R.layout.sense_ble_view, R.id.blue_box_view);
        view = new OnboardingSimpleStepView(this, inflater)
                .setAnimatedView(animatedView)
                .setHeadingText(R.string.action_pair_your_sense)
                .setSubheadingText(R.string.message_onboarding_register_location)
                .setPrimaryButtonText(R.string.action_set_location)
                .setSecondaryButtonText(R.string.action_why_is_this_required)
                .setPrimaryOnClickListener(this::setLocation)
                .setSecondaryOnClickListener(this::onHelp)
                .setWantsSecondaryButton(showSkip)
                .setToolbarWantsBackButton(false);

        return view;
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewAnimator.onViewCreated(getActivity(), R.animator.bluetooth_sleep_pill_ota_animator);

    }


    @Override
    public void onResume() {
        super.onResume();
        viewAnimator.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewAnimator.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewAnimator.onDestroyView();
        if (view != null) {
            this.view.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            finishFlow();
        } else {
            permission.showEnableInstructionsDialog();
        }
    }

    private void onHelp(final View ignored) {
        permission.requestPermissionWithDialog();
    }

    private void setLocation(final View primaryButton) {
        if (!permission.isGranted()) {
            permission.requestPermission();
        } else {
            finishFlow();
        }

    }

    @Override
    public boolean onInterceptBackPressed(@NonNull final Runnable defaultBehavior) {
        cancelFlow();
        return true;
    }
}
