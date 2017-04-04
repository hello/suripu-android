package is.hello.sense.flows.nightmode.ui.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.view.View;

import javax.inject.Inject;

import is.hello.sense.flows.nightmode.NightMode;
import is.hello.sense.flows.nightmode.interactors.NightModeInteractor;
import is.hello.sense.flows.nightmode.ui.views.NightModeLocationPermission;
import is.hello.sense.flows.nightmode.ui.views.NightModeView;
import is.hello.sense.interactors.LocationInteractor;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.permissions.LocationPermission;
import is.hello.sense.util.Analytics;

/**
 * Control night mode settings
 */

public class NightModeFragment extends PresenterFragment<NightModeView>
        implements NightModeView.Listener,
        FragmentCompat.OnRequestPermissionsResultCallback {

    @Inject
    NightModeInteractor nightModeInteractor;
    @Inject
    LocationInteractor locationInteractor;

    private final LocationPermission locationPermission = new NightModeLocationPermission(this);
    private boolean wantsScheduledMode = false;

    @Override
    public void initializePresenterView() {
        if(presenterView == null) {
            presenterView = new NightModeView(getActivity());
            this.setInitialMode();
            presenterView.setRadioGroupListener(this);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindAndSubscribe(locationInteractor.locationSubject,
                         this::bindLocation,
                         this::presentLocationError);
    }

    private void presentLocationError(@NonNull
                                      final Throwable throwable) {

    }

    private void bindLocation(@Nullable
                              final Location location) {
        updateScheduledModeView();
        if (location != null && wantsScheduledMode) {
            this.wantsScheduledMode = false;
            this.setMode(NightMode.AUTO);
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
        this.locationInteractor.update();
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
        this.wantsScheduledMode = true;
        this.locationInteractor.update();
    }

    @Override
    public boolean onLocationPermissionLinkIntercepted() {
        if (!locationPermission.isGranted()) {
            locationPermission.requestPermissionWithDialog();
        } else if (!locationInteractor.hasLocation()) {
            locationPermission.showEnableServiceInstructionsDialog();
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (locationPermission.isGrantedFromResult(requestCode, permissions, grantResults)) {
            locationInteractor.update(); //todo doesn't prompt here because only tries to refresh
        } else {
            locationPermission.showEnableInstructionsDialog();
        }
    }

    private void updateScheduledModeView() {
        if(hasPresenterView()) {
            final boolean enabled = locationPermission.isGranted() && locationInteractor.hasLocation();
            presenterView.setScheduledModeEnabled(enabled);
        }
    }

    private void setMode(@NightMode final int mode) {
        if (nightModeInteractor == null || nightModeInteractor.getCurrentMode().equals(mode)) {
            return;
        }
        nightModeInteractor.setMode(mode);
        sendAnalytics(mode);
        getActivity().recreate();

    }

    private void sendAnalytics(@NightMode final int mode) {
        final String setting;
        switch (mode) {
            case NightMode.OFF:
                setting = Analytics.NightMode.PROP_OFF;
                break;
            case NightMode.ON:
                setting = Analytics.NightMode.PROP_ON;
                break;
            case NightMode.AUTO:
                setting = Analytics.NightMode.PROP_AUTO;
                break;
            default:
                setting = Analytics.NightMode.PROP_OFF;
        }
        Analytics.trackEvent(Analytics.NightMode.EVENT_CHANGED,
                             Analytics.createProperties(Analytics.NightMode.PROP_SETTING, setting));
    }
}
