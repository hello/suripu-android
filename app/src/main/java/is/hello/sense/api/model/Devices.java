package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class Devices extends ApiResponse {
    @SerializedName("senses")
    public final ArrayList<SenseDevice> senses;

    @SerializedName("pills")
    public final ArrayList<Device> sleepPills;


    public Devices(@NonNull ArrayList<SenseDevice> senses,
                   @NonNull ArrayList<Device> sleepPills) {
        this.senses = senses;
        this.sleepPills = sleepPills;
    }


    @Override
    public String toString() {
        return "Devices{" +
                "senses=" + senses +
                ", sleepPills=" + sleepPills +
                '}';
    }
}
