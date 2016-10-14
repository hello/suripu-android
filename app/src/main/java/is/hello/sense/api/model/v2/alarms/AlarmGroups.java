package is.hello.sense.api.model.v2.alarms;


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
}
