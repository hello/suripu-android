package is.hello.sense.flows.nightmode.ui.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.model.UserLocation;
import is.hello.sense.flows.generic.ui.interactors.LocationInteractor;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.views.NightModeLocationPermission;
import is.hello.sense.flows.nightmode.ui.views.NightModeView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.ui.widget.SenseAlertDialog;

/**
 * Control night mode settings
 */
public class NightModeFragment extends PresenterFragment<NightModeView>
        implements NightModeView.Listener {

    @Inject
    NightModeInteractor nightModeInteractor;

    @Inject
    LocationInteractor locationInteractor;

    private final LocationPermission locationPermission = new NightModeLocationPermission(this);

    //region PresenterFragment
    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new NightModeView(getActivity());
            setInitialMode();
            presenterView.setListener(this);
        }
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.locationInteractor.start();
        if (hasPresenterView()) {
            final boolean hasLocationPermission = this.locationPermission.isGranted();
            final boolean currentModeIsAuto = this.nightModeInteractor.getCurrentMode() == AppCompatDelegate.MODE_NIGHT_AUTO;
            if (!hasLocationPermission && currentModeIsAuto) {
                this.presenterView.setOffMode();
            }
            this.presenterView.setScheduledModeEnabled(hasLocationPermission);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.locationInteractor.stop();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            if (hasPresenterView()) {
                presenterView.setScheduledModeEnabled(true);
            }
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }
    //endregion

    //region NightModeView.Listener
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
        final UserLocation userLocation = this.locationInteractor.getCurrentUserLocation();
        if (userLocation == null) {
            presentUserLocationError();
            this.locationInteractor.start();
        } else {
            setMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

    @Override
    public boolean onLocationPermissionLinkIntercepted() {
        locationPermission.requestPermissionWithDialog();
        return true;
    }
    //endregion

    //region methods
    private void presentUserLocationError() {
        if (presenterView == null) {
            return;
        }
        new SenseAlertDialog.Builder()
                .setTitle(R.string.nightmode_scheduled_error_title)
                .setMessage(R.string.nightmode_scheduled_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .build(getActivity())
                .show();
        if (nightModeInteractor.getCurrentMode().equals(AppCompatDelegate.MODE_NIGHT_YES)) {
            this.presenterView.setAlwaysOnMode();
        } else {
            this.presenterView.setOffMode();
        }
    }


    private void setInitialMode() {
        if (nightModeInteractor == null) {
            this.presenterView.setOffMode();
            return;
        }
        if (nightModeInteractor.getCurrentMode().equals(AppCompatDelegate.MODE_NIGHT_AUTO) && locationInteractor.getCurrentUserLocation() == null) {
            this.presenterView.setOffMode();
            this.setMode(AppCompatDelegate.MODE_NIGHT_NO);
            return;
        }

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
                presenterView.setOffMode();
        }

    }

    private void setMode(@AppCompatDelegate.NightMode final int mode) {
        if (nightModeInteractor == null || nightModeInteractor.getCurrentMode().equals(mode)) {
            return;
        }
        nightModeInteractor.setMode(mode);
        getActivity().recreate();

    }
    //endregion
}
