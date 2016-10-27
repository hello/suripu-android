package is.hello.sense.api.model.v2.alarms;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiResponse;

public class AlarmGroups extends ApiResponse {

    @SerializedName("expansions")
    private ArrayList<Alarm> expansions;
    @SerializedName("voice")
    private ArrayList<Alarm> voice;
    @SerializedName("classic")
    private ArrayList<Alarm> classic;


    public ArrayList<Alarm> getExpansions() {
        return expansions;
    }

    public ArrayList<Alarm> getVoice() {
        return voice;
    }

    public ArrayList<Alarm> getClassic() {
        return classic;
    }

    public int getTotalSize(){
        int total = 0;

        total += classic != null ? classic.size() : 0;
        total += expansions != null ? expansions.size() : 0;
        total += voice != null ? voice.size() : 0;
        return total;
    }

    public static ArrayList<Alarm> getAll(@NonNull final AlarmGroups alarmGroups) {
        final ArrayList<Alarm> alarms = new ArrayList<>(alarmGroups.getTotalSize());
        alarms.addAll(alarmGroups.getClassic());
        alarms.addAll(alarmGroups.getVoice());
        alarms.addAll(alarmGroups.getExpansions());
        return alarms;
    }
}
