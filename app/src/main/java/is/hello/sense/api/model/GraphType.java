package is.hello.sense.api.model;

import android.content.res.Resources;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;

import is.hello.sense.R;
import is.hello.sense.ui.widget.graphing.drawables.GraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.HistogramGraphDrawable;
import is.hello.sense.ui.widget.graphing.drawables.LineGraphDrawable;

public enum GraphType {
    HISTOGRAM {
        @Override
        public GraphDrawable createDrawable(@NonNull Resources resources) {
            return new HistogramGraphDrawable(resources);
        }

        @Override
        public @DrawableRes int getGridDrawable() {
            return R.drawable.graph_grid_fill_top_down;
        }
    },
    TIME_SERIES_LINE {
        @Override
        public GraphDrawable createDrawable(@NonNull Resources resources) {
            LineGraphDrawable lineGraph = new LineGraphDrawable(resources);
            lineGraph.setTopInset(resources.getDimensionPixelSize(R.dimen.gap_outer));
            return lineGraph;
        }

        @Override
        public @DrawableRes int getGridDrawable() {
            return R.drawable.graph_grid_fill_bottom_up;
        }
    };

    public abstract GraphDrawable createDrawable(@NonNull Resources resources);

    public abstract @DrawableRes int getGridDrawable();

    @JsonCreator
    public static GraphType fromString(@Nullable String string) {
        return Enums.fromString(string, values(), TIME_SERIES_LINE);
    }
}
