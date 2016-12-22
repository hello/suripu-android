package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public class DeviceOTAState extends ApiResponse {

    @SerializedName("status")
    public final OtaState state;

    public DeviceOTAState(final OtaState state){
        this.state = state;
    }

    public enum OtaState implements Enums.FromString {
        NOT_REQUIRED(R.string.device_ota_state_not_required),
        REQUIRED(R.string.device_ota_state_required),
        RESPONSE_SENT(R.string.device_ota_state_response_sent),
        IN_PROGRESS(R.string.device_ota_state_in_progress),
        COMPLETE(R.string.device_ota_state_complete),
        ERROR(R.string.device_ota_state_error),
        UNKNOWN(R.string.device_state_unknown);

        public final @StringRes
        int state;

        OtaState(final @StringRes int state){
            this.state = state;
        }

        public static OtaState fromString(@NonNull String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }

}
