package is.hello.sense.api.model.v2.alarms;


import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.api.model.ApiResponse;

public class AlarmGroups extends ApiResponse {

    @SerializedName("expansions")
    private ArrayList<Alarm> expansions;
    @SerializedName("voice")
    private ArrayList<Alarm> voice;
    @SerializedName("classic")
    private ArrayList<Alarm> classic;

    public AlarmGroups(){
        this.classic = new ArrayList<>();
        this.voice = new ArrayList<>();
        this.expansions = new ArrayList<>();
    }

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

    /**
     * Currently we never add any alarms to the {@link AlarmGroups#expansions} list like in iOS
     * but the ideal behavior would be to add it to original list if modifying an existing alarm.
     * @return {@link AlarmGroups} to be used for saving.
     */
    public static AlarmGroups from(@NonNull final List<Alarm> alarms){
        final AlarmGroups alarmGroups = new AlarmGroups();
        final ArrayList<Alarm> voiceAlarms = alarmGroups.getVoice();
        final ArrayList<Alarm> classicAlarms = alarmGroups.getClassic();
        for(final Alarm alarm : alarms){
            switch (alarm.getSource()){
                case VOICE_SERVICE:
                    voiceAlarms.add(alarm);
                    break;
                case MOBILE_APP:
                case OTHER:
                    classicAlarms.add(alarm);
                    break;
                default:
                    // don't add unknown source alarms
                    break;
            }
        }
        return alarmGroups;
    }
}
