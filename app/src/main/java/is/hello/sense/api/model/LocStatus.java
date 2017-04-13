package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.google.android.gms.common.api.Status;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class LocStatus implements Serializable {
    @SerializedName("status")
    private final Status status;

    public LocStatus(@Nullable final Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
