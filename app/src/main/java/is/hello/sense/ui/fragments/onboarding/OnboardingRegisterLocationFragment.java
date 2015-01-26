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

import is.hello.sense.R;
import is.hello.sense.api.model.Account;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.AccountEditingFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.ResumeScheduler;

public class OnboardingRegisterLocationFragment extends AccountEditingFragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int RESOLUTION_REQUEST_CODE = 0x99;
    private final ResumeScheduler.Coordinator coordinator = new ResumeScheduler.Coordinator(this::isResumed);

    private Account account;

    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.account = getContainer().getAccount();

        if (savedInstanceState == null && getActivity() instanceof OnboardingActivity) {
            Analytics.trackEvent(Analytics.Onboarding.EVENT_ONBOARDING_LOCATION, null);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return new OnboardingSimpleStepViewBuilder(this, inflater, container)
                .setHeadingText(R.string.title_onboarding_register_location)
                .setSubheadingText(R.string.onboarding_register_info_explanation)
                .setDiagramImage(R.drawable.onboarding_map)
                .setPrimaryButtonText(R.string.action_set_location)
                .setPrimaryOnClickListener(ignored -> optIn())
                .setSecondaryOnClickListener(ignored -> getContainer().onAccountUpdated(this))
                .hideToolbar()
                .create();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESOLUTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            optIn();
        }
    }

    public void optIn() {
        coordinator.postOnResume(() -> {
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
        coordinator.postOnResume(() -> {
            LoadingDialogFragment.close(getFragmentManager());
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                account.setLocation(lastLocation.getLatitude(), lastLocation.getLongitude());
            }
            getContainer().onAccountUpdated(this);
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        coordinator.postOnResume(() -> {
            LoadingDialogFragment.close(getFragmentManager());
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        coordinator.postOnResume(() -> {
            LoadingDialogFragment.close(getFragmentManager());
            try {
                connectionResult.startResolutionForResult(getActivity(), RESOLUTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                ErrorDialogFragment.presentError(getFragmentManager(), e);
            }
        });
    }
}
