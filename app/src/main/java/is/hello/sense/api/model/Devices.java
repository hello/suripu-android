package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import is.hello.sense.functional.Lists;

public class Devices extends ApiResponse {
    @SerializedName("senses")
    public final ArrayList<SenseDevice> senses;

    @SerializedName("pills")
    public final ArrayList<SleepPillDevice> sleepPills;


    public Devices(@NonNull ArrayList<SenseDevice> senses,
                   @NonNull ArrayList<SleepPillDevice> sleepPills) {
        this.senses = senses;
        this.sleepPills = sleepPills;
    }

    public SenseDevice getSense() {
        if (!Lists.isEmpty(senses)) {
            return senses.get(0);
        } else {
            return null;
        }
    }

    public SleepPillDevice getSleepPill() {
        if (!Lists.isEmpty(sleepPills)) {
            return sleepPills.get(0);
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        return "Devices{" +
                "senses=" + senses +
                ", sleepPills=" + sleepPills +
                '}';
    }
}
