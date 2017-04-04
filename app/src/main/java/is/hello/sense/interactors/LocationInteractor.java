package is.hello.sense.interactors;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.util.LocationUtil;
import is.hello.sense.util.PersistentLocationManager;
import rx.Observable;

public class LocationInteractor extends ValueInteractor {

    private final PersistentLocationManager locationManager;
    private final LocationUtil locationUtil;
    public InteractorSubject<Location> locationSubject = this.subject;

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

    @Nullable
    public Location getLastKnownLocation() {
        update(); //todo decide if should not auto update when called
        return (Location) subject.getValue();
    }

    public boolean hasLocation() {
        return subject.getValue() != null;
    }
}
