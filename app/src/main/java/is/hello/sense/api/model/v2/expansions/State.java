package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;

public enum State implements Enums.FromString {
    NOT_CONNECTED(R.string.expansions_state_not_connected),
    CONNECTED_ON(R.string.expansions_state_connected_on),
    CONNECTED_OFF(R.string.expansions_state_connected_off),
    REVOKED(R.string.expansions_state_revoked),
    NOT_CONFIGURED(R.string.expansions_state_not_configured),
    NOT_AVAILABLE(R.string.expansions_state_not_available),
    UNKNOWN(R.string.expansions_state_unknown);

    @StringRes
    public final int displayValue;

    State(@StringRes final int displayValue){
        this.displayValue = displayValue;
    }

    public static State fromString(@Nullable final String string) {
        return Enums.fromString(string, values(), UNKNOWN);
    }

    public static class Request extends ApiResponse{
        @SerializedName("state")
        public final State state;

        Request(@NonNull final State state){
            this.state = state;
        }

        public static Request with(@NonNull final State state) {
            return new State.Request(state);
        }
    }
}
