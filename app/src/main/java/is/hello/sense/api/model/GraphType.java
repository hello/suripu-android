package is.hello.sense.api.model;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;

import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.HistogramGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;

public enum GraphType {
    HISTOGRAM {
        @Override
        public GraphDrawable createDrawable(@NonNull Resources resources) {
            return new HistogramGraphDrawable(resources);
        }
    },
    TIME_SERIES_LINE {
        @Override
        public GraphDrawable createDrawable(@NonNull Resources resources) {
            return new LineGraphDrawable(resources);
        }
    };

    public abstract GraphDrawable createDrawable(@NonNull Resources resources);

    @JsonCreator
    public static GraphType fromString(@Nullable String string) {
        return Enums.fromString(string, values(), TIME_SERIES_LINE);
    }
}
