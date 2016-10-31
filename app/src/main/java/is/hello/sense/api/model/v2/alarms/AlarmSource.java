package is.hello.sense.api.model.v2.alarms;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import is.hello.sense.R;
import is.hello.sense.api.gson.Enums;

public enum AlarmSource implements Enums.FromString{

    MOBILE_APP(R.string.alarm_source_mobile_app),
    VOICE_SERVICE(R.string.alarm_source_voice),
    OTHER(R.string.device_unknown);

    @StringRes
    public final int displayRes;

    AlarmSource(@StringRes final int sourceRes){
        this.displayRes = sourceRes;
    }

    public static AlarmSource fromString(@NonNull final String value){
        return Enums.fromString(value, values(), MOBILE_APP);
    }
}
