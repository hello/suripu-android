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

import is.hello.sense.api.model.LocStatus;
import is.hello.sense.api.model.UserLocation;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.interactors.PersistentPreferencesInteractor;
import is.hello.sense.interactors.ValueInteractor;
import is.hello.sense.util.Analytics;
import rx.Observable;

public class LocationInteractor extends ValueInteractor<LocStatus>
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private final Context context;
    private final GoogleApiClient apiClient;
    private final PersistentPreferencesInteractor persistentPreferencesInteractor;
    private Status status;

    public InteractorSubject<LocStatus> statusSubject = subject;

    public LocationInteractor(@NonNull final Context context,
                              @NonNull final PersistentPreferencesInteractor prefs) {
        this.context = context;
        this.apiClient = new GoogleApiClient.Builder(this.context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        this.persistentPreferencesInteractor = prefs;
    }

    //region ValueInteractor
    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<LocStatus> provideUpdateObservable() {
        return Observable.just(new LocStatus(this.status));
    }
    //endregion

    // region ConnectionCallbacks
    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        if (hasPermissions()) {
            //noinspection MissingPermission
            final Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            if (location == null) {
                startLocationUpdates();
            } else {
                handleResult(location);
            }
        }
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Analytics.trackError(getClass().getSimpleName() + " onConnectionSuspended - " + i, null, null, null, false);
        handleResult(null);
    }
    //endregion

    //region OnConnectionFailedListener
    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Analytics.trackError(getClass().getSimpleName() + " onConnectionFailed - " + connectionResult.toString(), null, null, null, false);
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
    public void forget() {
        this.status = null;
        this.subject.forget();
    }

    /**
     * Start looking for the users location. Will stop on success.
     */
    public void start() {
        if (this.apiClient.isConnected()) {
            stopLocationUpdates();
            startLocationUpdates();
        } else if (!this.apiClient.isConnecting()) {
            this.apiClient.connect();
        }
    }

    public void stop() {
        if (this.apiClient.isConnecting()) {
            this.apiClient.disconnect();
        } else if (this.apiClient.isConnected()) {
            stopLocationUpdates();
            this.apiClient.disconnect();

        }
    }

    private void startLocationUpdates() {
        final LocationRequest currentRequest = getLocationRequest();
        LocationServices.SettingsApi
                .checkLocationSettings(
                        apiClient,
                        getLocationSettingsRequest(currentRequest))
                .setResultCallback(locationSettingsResult -> {
                    final Status status = locationSettingsResult.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            if (hasPermissions()) {
                                //noinspection MissingPermission
                                LocationServices.FusedLocationApi.requestLocationUpdates(apiClient,
                                                                                         currentRequest,
                                                                                         LocationInteractor.this);
                                this.status = status;
                                update();
                            }
                            break;
                        default:
                            this.status = status;
                            update();
                            break;
                    }
                });
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                apiClient,
                this);
    }

    private void handleResult(@Nullable final Location location) {
        if (location == null) {
            this.status = null;
            this.update();
        } else {
            this.persistentPreferencesInteractor.saveUserLocation(new UserLocation(location.getLatitude(), location.getLongitude()));
        }
    }

    public UserLocation getCurrentUserLocation() {
        return this.persistentPreferencesInteractor.getUserLocation();
    }

    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @NonNull
    private LocationRequest getLocationRequest() {
        final int priority;
        if (this.getCurrentUserLocation() != null) {
            priority = LocationRequest.PRIORITY_LOW_POWER;
        } else {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
        }
        return new LocationRequest()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(priority);
    }

    @NonNull
    private LocationSettingsRequest getLocationSettingsRequest(@NonNull final LocationRequest request) {
        return new LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .build();
    }

    //endregion
}
