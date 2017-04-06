package is.hello.sense.flows.generic.ui.interactors;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStates;


import is.hello.sense.api.model.UserLocation;
import is.hello.sense.interactors.PersistentPreferencesInteractor;

public class LocationInteractor
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private final Context context;
    private final GoogleApiClient apiClient;
    private Listener listener;

    private final PersistentPreferencesInteractor persistentPreferencesInteractor;

    public LocationInteractor(@NonNull final Context context,
                              @NonNull final PersistentPreferencesInteractor prefs) {
        this.context = context;
        this.apiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        this.persistentPreferencesInteractor = prefs;
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        if (this.listener == null) {
            return;
        }
        //todo check permission
        final Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        handleResult(location);
    }

    @Override
    public void onConnectionSuspended(final int i) {
        handleResult(null);
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        handleResult(null);
    }

    public void setListener(@NonNull final Listener listener) {
        this.listener = listener;
    }

    public void requestLocation() {
        this.apiClient.connect();
        hasLocationSettings();
    }

    private void handleResult(@Nullable final Location location) {
        this.apiClient.disconnect();
        final UserLocation userLocation = location == null ? null : new UserLocation(location.getLatitude(), location.getLongitude());
        this.persistentPreferencesInteractor.saveUserLocation(userLocation);
        if (listener == null) {
            return;
        }
        this.listener.onUserLocationReceived(userLocation != null);
    }

    private void hasLocationSettings() {
        final LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(createLocationRequest());

        LocationServices.SettingsApi.checkLocationSettings(apiClient,
                                                           builder.build())
                                    .setResultCallback(result -> {
                                        final Status status = result.getStatus();
                                        final LocationSettingsStates states = result.getLocationSettingsStates();
                                        Log.e("LocInt", "Status: " + status.getStatusMessage());
                                        Log.e("LocInt", "States: " + states.toString());

                                    });

    }

    protected LocationRequest createLocationRequest() {
        final LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    protected LocationSettingsRequest buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(createLocationRequest());
        return builder.build();
    }

    public interface Listener {
        void onUserLocationReceived(final boolean hasLatLong);
    }
}
