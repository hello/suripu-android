package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

/**
 * example response
 * {<br>
    "datetime_utc": 1469743680000, <br>
    "text": "what is the temperature", <br>
    "response_text": "It's currently 71 degrees.",<br>
    "command": "room_temperature",<br>
    "result": "ok"<br>
 }
 */
public class VoiceResponse extends ApiResponse{

    @SerializedName("datetime_utc")
    DateTime dateTime;

    @SerializedName("text")
    String text;

    @SerializedName("response_text")
    String responseText;

    @SerializedName("command")
    String command;

    @SerializedName("result")
    Result result;

    public VoiceResponse(final DateTime dateTime,
                         final String text,
                         final String responseText,
                         final String command,
                         final Result result) {
        this.dateTime = dateTime;
        this.text = text;
        this.responseText = responseText;
        this.command = command;
        this.result = result;
    }

    public enum Result implements Enums.FromString{
        NONE(R.string.voice_result_none),
        OK(R.string.voice_result_ok),
        REJECTED(R.string.voice_result_rejected),
        TRY_AGAIN(R.string.voice_result_try_again),
        UNKNOWN(R.string.voice_result_unknown);

        @StringRes
        public final int stringRes;

        Result(@StringRes final int stringRes){
            this.stringRes = stringRes;
        }

        public static Result fromString(@NonNull final String string) {
            return Enums.fromString(string, values(), UNKNOWN);
        }
    }
}
