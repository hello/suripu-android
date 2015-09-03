package is.hello.sense.ui.fragments.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditor;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;

public class OnboardingRegisterLocationFragment extends InjectionFragment
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int RESOLUTION_REQUEST_CODE = 0x99;

    @Inject PreferencesPresenter preferences;

    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_LOCATION, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepView(this, inflater)
                .setHeadingText(R.string.title_onboarding_register_location)
                .setSubheadingText(R.string.onboarding_register_info_explanation)
                .setDiagramImage(R.drawable.onboarding_map)
                .setPrimaryButtonText(R.string.action_set_location)
                .setPrimaryOnClickListener(ignored -> optIn())
                .setSecondaryOnClickListener(ignored -> optOut())
                .hideToolbar();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESOLUTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            optIn();
        }
    }

    public void optOut() {
        preferences.edit()
                   .putBoolean(PreferencesPresenter.LOCATION_ENABLED, false)
                   .apply();
        AccountEditor.getContainer(this).onAccountUpdated(this);
    }

    public void optIn() {
        preferences.edit()
                   .putBoolean(PreferencesPresenter.LOCATION_ENABLED, true)
                   .apply();

        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.show(getFragmentManager());
            if (googleApiClient == null) {
                this.googleApiClient = new GoogleApiClient.Builder(getActivity())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
            googleApiClient.connect();
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        stateSafeExecutor.execute(() -> {
            final AccountEditor.Container container = AccountEditor.getContainer(this);
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                container.getAccount().setLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
            }
            container.onAccountUpdated(this);
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.close(getFragmentManager());
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        stateSafeExecutor.execute(() -> {
            LoadingDialogFragment.close(getFragmentManager());
            try {
                connectionResult.startResolutionForResult(getActivity(), RESOLUTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                ErrorDialogFragment.presentError(getFragmentManager(), e);
            }
        });
    }
}
