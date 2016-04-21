package is.hello.sense.api.model.v2;

import com.google.gson.annotations.SerializedName;

import org.joda.time.Instant;

import is.hello.sense.api.model.ApiResponse;

/**
 * Created by jimmy on 4/8/16.
 */
public class SleepSoundActionStop extends ApiResponse {

    @SerializedName("order")
    private final Long order;

    public SleepSoundActionStop() {
        this.order = Instant.now().getMillis();
    }

    public Long getOrder() {
        return order;
    }
}
