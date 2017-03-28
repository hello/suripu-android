package is.hello.sense.flows.nightmode.ui.fragments;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;

import javax.inject.Inject;

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
                case AppCompatDelegate.MODE_NIGHT_NO:
                    presenterView.setOffMode();
                    break;
                case AppCompatDelegate.MODE_NIGHT_YES:
                    presenterView.setAlwaysOnMode();
                    break;
                case AppCompatDelegate.MODE_NIGHT_AUTO:
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
        this.setMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Override
    public void onModeSelected() {
        this.setMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    @Override
    public void scheduledModeSelected() {
        this.setMode(AppCompatDelegate.MODE_NIGHT_AUTO);
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

    private void setMode(@AppCompatDelegate.NightMode final int mode) {
        if (nightModeInteractor == null || nightModeInteractor.getCurrentMode().equals(mode)) {
            return;
        }
        nightModeInteractor.setMode(mode);
        getActivity().recreate();

    }
}
