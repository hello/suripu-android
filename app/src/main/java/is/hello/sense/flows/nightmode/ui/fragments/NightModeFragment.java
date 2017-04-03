package is.hello.sense.flows.nightmode.ui.fragments;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import is.hello.sense.flows.nightmode.NightMode;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.views.NightModeLocationPermission;
import is.hello.sense.flows.nightmode.ui.views.NightModeView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.permissions.LocationPermission;

/**
 * Control night mode settings
 */

public class NightModeFragment extends PresenterFragment<NightModeView>
        implements NightModeView.Listener {

    @Inject
    NightModeInteractor nightModeInteractor;

    private final LocationPermission locationPermission = new NightModeLocationPermission(this);
    @Override
    public void initializePresenterView() {
        if(presenterView == null) {
            presenterView = new NightModeView(getActivity());
            this.setInitialMode();
            presenterView.setRadioGroupListener(this);
        }
    }

    private void setInitialMode() {
        if (nightModeInteractor !=null) {
            switch (nightModeInteractor.getCurrentMode()) {
                case NightMode.OFF:
                    presenterView.setOffMode();
                    break;
                case NightMode.ON:
                    presenterView.setAlwaysOnMode();
                    break;
                case NightMode.AUTO:
                    presenterView.setScheduledMode();
                    break;
                default:
                    debugLog("no case found for current night mode defaulting to off.");
                    presenterView.setOffMode();
            }
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
        this.setMode(NightMode.OFF);
    }

    @Override
    public void onModeSelected() {
        this.setMode(NightMode.ON);
    }

    @Override
    public void scheduledModeSelected() {
        this.setMode(NightMode.AUTO);
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

    private void setMode(@NightMode final int mode) {
        if (nightModeInteractor == null || nightModeInteractor.getCurrentMode().equals(mode)) {
            return;
        }
        nightModeInteractor.setMode(mode);
        getActivity().recreate();

    }
}
