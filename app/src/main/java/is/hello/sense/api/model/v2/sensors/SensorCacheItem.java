package is.hello.sense.api.model.v2.sensors;

import android.support.annotation.NonNull;

public class SensorCacheItem {
    private static final int ELAPSED_TIME_FOR_UPDATE_MS = 60000; // 1 minute
    private final SensorsDataResponse sensorsDataResponse;
    private final long lastUpdated;

    public SensorCacheItem(@NonNull final SensorsDataResponse sensorsDataResponse) {
        this.sensorsDataResponse = sensorsDataResponse;
        this.lastUpdated = System.currentTimeMillis();
    }

    public SensorsDataResponse getSensorsDataResponse() {
        return sensorsDataResponse;
    }

    public boolean isExpired(){
        return System.currentTimeMillis() - lastUpdated > ELAPSED_TIME_FOR_UPDATE_MS;
    }
}
