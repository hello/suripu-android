package is.hello.sense.interactors;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.util.LocationUtil;
import is.hello.sense.util.PersistentLocationManager;
import rx.Observable;

public class LocationInteractor extends ValueInteractor {

    private final PersistentLocationManager locationManager;
    private final LocationUtil locationUtil;

    public LocationInteractor(@NonNull final PersistentLocationManager persistentLocationManager,
                              @NonNull final LocationUtil locationUtil) {
        this.locationManager = persistentLocationManager;
        this.locationUtil = locationUtil;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    /**
     * @return location object if possible.
     * First tries to update location and stores it if found.
     * Falls back to last stored location which may be empty.
     */
    @Nullable
    @Override
    protected Observable<Location> provideUpdateObservable() {
        Location location = locationUtil.getLastKnownLocation();
        if (location != null) {
            locationManager.storeLocation(location);
        } else {
            location = locationManager.retrieveLocation();
        }
        return Observable.just(location);
    }

    public Location getLastKnownLocation() {
        update();
        return (Location) subject.getValue(); //todo if no previous value this will return nothing
    }
}
