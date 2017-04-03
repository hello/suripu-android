package is.hello.sense.util;

import android.Manifest;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.PermissionChecker;

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
    @Nullable
    public Location getLastKnownLocation() {
        if (PermissionChecker.checkSelfPermission(context,
                                                  Manifest.permission.ACCESS_COARSE_LOCATION) != PermissionChecker.PERMISSION_GRANTED) {
            return null;
        }
        final LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        final String locationProvider = locationManager.getBestProvider(criteria, true);
        if (locationProvider == null) {
            return null;
        }
        return locationManager.getLastKnownLocation(locationProvider);

    }
}
