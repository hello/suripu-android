package is.hello.sense.ui.fragments.onboarding;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.util.Analytics;

/**
 * Show user explanation for requiring location permission
 */

public class OnboardingSetLocationFragment extends SenseFragment {

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
        view = new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_onboarding_register_location)
                .setSubheadingText(R.string.message_onboarding_register_location)
                .setPrimaryButtonText(R.string.action_set_location)
                .setSecondaryButtonText(R.string.action_skip)
                .setDiagramImage(R.drawable.onboarding_set_location_diagram)
                .setPrimaryOnClickListener(this::setLocation)
                .setSecondaryOnClickListener(this::onSkip)
                .setWantsSecondaryButton(true)
                .setToolbarWantsBackButton(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

    private void onSkip(final View ignored) {
        finishFlow();
    }

    private void setLocation(final View primaryButton) {
        if (!permission.isGranted()) {
            permission.requestPermissionWithDialog();
        } else {
            finishFlow();
        }
    }
}
