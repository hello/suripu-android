package is.hello.sense.flows.generic.ui.interactors;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;


import is.hello.sense.api.model.UserLocation;
import is.hello.sense.interactors.PersistentPreferencesInteractor;

public class LocationInteractor
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private final Context context;
    private final GoogleApiClient apiClient;
    private final LocationRequest locationRequest;
    private final LocationSettingsRequest locationSettingsRequest;
    private final PersistentPreferencesInteractor persistentPreferencesInteractor;

    public LocationInteractor(@NonNull final Context context,
                              @NonNull final PersistentPreferencesInteractor prefs) {
        this.context = context;
        this.apiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        this.locationRequest = new LocationRequest()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.locationSettingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(this.locationRequest)
                .build();
        this.persistentPreferencesInteractor = prefs;
    }

    // region ConnectionCallbacks
    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        if (hasPermissions()) {
            //noinspection MissingPermission
            handleResult(LocationServices.FusedLocationApi.getLastLocation(apiClient));
        }
    }

    @Override
    public void onConnectionSuspended(final int i) {
        handleResult(null);
    }
    //endregion

    //region OnConnectionFailedListener
    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        handleResult(null);
    }

    //endregion

    //region LocationListener
    @Override
    public void onLocationChanged(final Location location) {
        handleResult(location);

    }
    //endregion

    //region methods
    public void resume() {
        this.apiClient.connect();
        startLocationUpdates();
    }

    public void pause() {
        if (this.apiClient.isConnected()) {
            stopLocationUpdates();
        }
        this.apiClient.disconnect();
    }

    private void startLocationUpdates() {
        LocationServices.SettingsApi
                .checkLocationSettings(
                        apiClient,
                        locationSettingsRequest)
                .setResultCallback(locationSettingsResult -> {
                    final Status status = locationSettingsResult.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            if (hasPermissions()) {
                                //noinspection MissingPermission
                                LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, LocationInteractor.this);
                            }
                            break;
                    }
                });
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                apiClient,
                this).setResultCallback(status -> {
        });
    }


    private void handleResult(@Nullable final Location location) {
        final UserLocation userLocation = location == null ? null : new UserLocation(location.getLatitude(), location.getLongitude());
        this.persistentPreferencesInteractor.saveUserLocation(userLocation);
    }

    public UserLocation getCurrentUserLocation() {
        return this.persistentPreferencesInteractor.getUserLocation();
    }

    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    //endregion

}
