package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;

public class SleepSounds extends ApiResponse {

    @SerializedName("sounds")
    private List<Sound> sounds;

    @SerializedName("state")
    private State state;

    public List<Sound> getSounds() {
        return sounds;
    }

    public State getState() {
        return state;
    }

    public enum State implements Enums.FromString {
        OK(),
        SOUNDS_NOT_DOWNLOADED(),    // Sounds have not *yet* been downloaded to Sense, but should be.
        SENSE_UPDATE_REQUIRED(),    // Sense cannot play sounds because it has old firmware
        FEATURE_DISABLED();         // User doesn't have this feature flipped.

        State() {
        }

        public static State fromString(@NonNull String string) {
            return Enums.fromString(string, values(), OK);
        }
    }
}
