package is.hello.sense.flows.nightmode.ui.fragments;

import android.support.annotation.NonNull;

import is.hello.sense.flows.nightmode.ui.views.NightModeLocationPermission;
import is.hello.sense.flows.nightmode.ui.views.NightModeView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.permissions.LocationPermission;

/**
 * Control night mode settings
 */

public class NightModeFragment extends PresenterFragment<NightModeView>
        implements NightModeView.Listener {

    private final LocationPermission locationPermission = new NightModeLocationPermission(this);
    @Override
    public void initializePresenterView() {
        if(presenterView == null) {
            presenterView = new NightModeView(getActivity());
            presenterView.setRadioGroupListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(hasPresenterView()) {
            presenterView.setScheduledModeEnabled(locationPermission.isGranted());
        }
    }

    @Override
    public void offModeSelected() {
        //todo
    }

    @Override
    public void onModeSelected() {
        //todo
    }

    @Override
    public void scheduledModeSelected() {
        //todo
    }

    @Override
    public boolean onLocationPermissionLinkIntercepted() {
            locationPermission.requestPermissionWithDialog();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            if(hasPresenterView()) {
                presenterView.setScheduledModeEnabled(true);
            }
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }
}
