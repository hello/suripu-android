package is.hello.sense.ui.handholding;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class Tutorial implements Serializable {
    public final String description;
    public final Interaction interaction;

    public Tutorial(@NonNull String description,
                    @NonNull Interaction interaction) {
        this.description = description;
        this.interaction = interaction;
    }


    @Override
    public String toString() {
        return "Tutorial{" +
                "description='" + description + '\'' +
                ", interaction=" + interaction +
                '}';
    }
}
