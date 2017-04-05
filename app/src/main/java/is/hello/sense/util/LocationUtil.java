package is.hello.sense.util;

import android.Manifest;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

import static android.content.Context.LOCATION_SERVICE;

public class LocationUtil {

    private final Context context;

    public LocationUtil(@NonNull final Context context) {
        this.context = context;
    }

    /**
     * @return null if {@link Manifest.permission#ACCESS_COARSE_LOCATION} not granted
     * or no providers matching criteria could be found.
     * Recommend listen for location updates because usually no location would be returned
     * right after enabling location settings on device for 1 - 2 minutes.
     */
    @Deprecated
    @SuppressWarnings("MissingPermission")
    @Nullable
    public Location getLastKnownLocation() {
        final LocationManager locationManager = getLocationManager();
        if (locationManager == null) {
            return null;
        }
        final String locationProvider = locationManager.getBestProvider(getDefaultCriteria(), true);
        if (locationProvider == null) {
            return null;
        }
        return locationManager.getLastKnownLocation(locationProvider);

    }

    @Deprecated
    @Nullable
    protected LocationManager getLocationManager() {
        if (PermissionChecker.checkSelfPermission(context,
                                                  Manifest.permission.ACCESS_COARSE_LOCATION) != PermissionChecker.PERMISSION_GRANTED) {
            return null;
        }
        return (LocationManager) context.getSystemService(LOCATION_SERVICE);
    }

    @Deprecated
    @NonNull
    protected Criteria getDefaultCriteria() {
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        return criteria;
    }

    @NonNull
    public Observable<Location> getLastKnownLocationV2() {
        final PublishSubject<Location> locationSubject = PublishSubject.create();

        setUpClient(locationSubject,
                    new GetLastKnownLocationCallback(locationSubject));
        return locationSubject;
    }

    @NonNull
    public Observable<Status> requestSettingsChange() {
        final PublishSubject<Status> statusSubject = PublishSubject.create();

        setUpClient(statusSubject,
                    new RequestLocationSettingChangeCallback(statusSubject));
        return statusSubject;
    }

    private void setUpClient(@NonNull final PublishSubject subject,
                             @NonNull final ClientConnectionCallBacks callBacks) {
        final GoogleApiClient client = createClient(callBacks,
                                                    callBacks);

        callBacks.setClient(client);

        subject.doOnUnsubscribe(() -> {
            if (client.isConnected() || client.isConnecting()) {
                client.disconnect();
            }
        });

        try {
            client.connect();
        } catch (final Throwable e) {
            subject.onError(e);
        }
    }

    private GoogleApiClient createClient(@NonNull final GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                                         @NonNull final GoogleApiClient.OnConnectionFailedListener connectionFailedListener) {
        return new GoogleApiClient.Builder(context).addApi(LocationServices.API)
                                                   .addConnectionCallbacks(connectionCallbacks)
                                                   .addOnConnectionFailedListener(connectionFailedListener)
                                                   .build();
    }

    private static class GetLastKnownLocationCallback extends ClientConnectionCallBacks<Location> {

        GetLastKnownLocationCallback(@NonNull final Observer<Location> observer) {
            super(observer);
        }

        @SuppressWarnings("MissingPermission")
        @Override
        protected void onClientConnected(@NonNull final Observer<Location> observer,
                                         @NonNull final GoogleApiClient client) {
            observer.onNext(LocationServices.FusedLocationApi.getLastLocation(client));
        }
    }

    private static class RequestLocationSettingChangeCallback extends ClientConnectionCallBacks<Status> {

        final LocationSettingsRequest locationSettingsRequest;

        RequestLocationSettingChangeCallback(@NonNull final Observer<Status> observer) {
            super(observer);
            final LocationRequest locationRequest = LocationRequest.create()
                           .setFastestInterval(1000) //millis
                           .setExpirationTime(5 * 1000) //millis
                           .setPriority(LocationRequest.PRIORITY_LOW_POWER);
            this.locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                                                                                .setAlwaysShow(true)
                                                                                .build();
        }

        @Override
        protected void onClientConnected(@NonNull final Observer<Status> observer,
                                         @NonNull final GoogleApiClient client) {
            LocationServices.SettingsApi.checkLocationSettings(client, locationSettingsRequest)
                                        .setResultCallback( callback -> {
                                            observer.onNext(callback.getStatus());
                                        });
        }
    }

    private static abstract class ClientConnectionCallBacks<T> implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        private final Observer<T> observer;
        @Nullable
        private GoogleApiClient client;

        ClientConnectionCallBacks(@NonNull final Observer<T> observer) {
            this.observer = observer;
        }

        protected abstract void onClientConnected(@NonNull final Observer<T> observer,
                                                  @NonNull final GoogleApiClient client);

        @Override
        public void onConnected(@Nullable final Bundle bundle) {
            if (client != null) {
                try {
                    onClientConnected(observer, client);
                } catch (final Throwable throwable) {
                    observer.onError(throwable);
                }
            }
        }

        @Override
        public void onConnectionSuspended(final int cause) {
            observer.onError(new Throwable(String.valueOf(cause)));
        }

        @Override
        public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
            observer.onError(new Throwable(connectionResult.getErrorMessage()));
        }

        public void setClient(@NonNull final GoogleApiClient client) {
            this.client = client;
        }
    }
}
