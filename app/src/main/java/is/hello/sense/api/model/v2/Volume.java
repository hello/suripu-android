package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.util.IListObject;

public enum Volume implements Enums.FromString, IListObject.IListItem {
    High(100),
    Medium(50),
    Low(25),
    None(0);

    private static final int VolumeAccuracyOffset = 5;
    final int volume;

    Volume(final int volume) {
        this.volume = volume;
    }


    public int getVolume() {
        return volume;
    }

    public static Volume fromString(@NonNull final String string) {
        return Enums.fromString(string, values(), None);
    }

    public static Volume fromInt(@Nullable final Integer value) {
        if (value == null) {
            return None;
        }
        for (final Volume volume : values()) {
            if (volume.volume <= value + VolumeAccuracyOffset && volume.volume >= value - VolumeAccuracyOffset) {
                return volume;
            }
        }
        return None;
    }

    @Override
    public int getId() {
        return getVolume();
    }

    @Override
    public String getName() {
        return this.toString();
    }

    @Override
    public String getPreviewUrl() {
        return null;
    }
}
