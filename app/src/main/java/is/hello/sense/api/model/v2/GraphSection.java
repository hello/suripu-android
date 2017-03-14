package is.hello.sense.api.model.v2;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.DateFormatter;

public class GraphSection extends ApiResponse {
    private static final Float DO_NOT_SHOW_VALUE = -2f;
    private static final int TITLE_POSITION = 0;

    @SerializedName("values")
    private final List<Float> values;

    @SerializedName("titles")
    private final List<String> titles;

    @SerializedName("highlighted_values")
    private final List<Integer> highlightedValues;

    @SerializedName("highlighted_title")
    @Nullable
    private Integer highlightedTitle;

    static GraphSection withHighlightedTitle(@NonNull final GraphSection graphSection) {
        final GraphSection section = new GraphSection();
        section.highlightedTitle = graphSection.highlightedTitle;
        return section;
    }

    public static boolean canShow(@NonNull final Float value) {
        return !DO_NOT_SHOW_VALUE.equals(value);
    }

    GraphSection() {
        this.titles = new ArrayList<>();
        this.values = new ArrayList<>();
        this.highlightedValues = new ArrayList<>();
        this.highlightedTitle = 0;
    }

    public void addValue(@Nullable final Float value) {
        this.values.add(value);
    }

    public void addTitle(@Nullable final String title) {
        if (title == null) {
            return;
        }
        titles.add(title);
    }

    public void addHighlightedValues(final int index) {
        this.highlightedValues.add(index);
    }

    public List<Float> getValues() {
        return values;
    }

    @Nullable
    public Float getValue(final int index) {
        if (index < 0 || index >= values.size()) {
            Log.e(getClass().getName(), String.format("GraphSection.values %d out of bounds index", index));
            return null;
        }
        return values.get(index);
    }

    public List<String> getTitles() {
        return titles;
    }

    public List<Integer> getHighlightedValues() {
        return highlightedValues;
    }

    @Nullable
    public Integer getHighlightedTitle() {
        return highlightedTitle;
    }

    /**
     * todo propagate throw errors to display dialog to error and not render graph
     * currently defaults to 0 if any expected errors are caught
     *
     * @return int 0 to 6 representing first day of week Sun to Sat of first month
     */
    int getFirstDayOfMonthOffset() {
        int offset = 0;
        try {
            if (titles == null || titles.isEmpty()) {
                throw new IllegalStateException("GraphSection title required to determine first day of month offset");
            }
            final String monthTitle = titles.get(TITLE_POSITION);
            final int monthValue = DateFormatter.getMonthInt(monthTitle);
            offset = DateFormatter.getFirstDayOfMonthValue(monthValue) - 1;
        } catch (final ParseException e) {
            Log.e(getClass().getName(), "Problem parsing month: " + e.getLocalizedMessage());
        } catch (final IllegalStateException e) {
            Log.e(getClass().getName(), e.getLocalizedMessage());
        }

        return offset;
    }

    /**
     * @param numValues number of values to add to end of {@link GraphSection#values} that will not be rendered
     * @return updated {@link GraphSection} instance
     */
    GraphSection withDoNotShowValues(final int numValues) {
        for (int i = 0; i < numValues; i++) {
            values.add(DO_NOT_SHOW_VALUE);
        }
        return this;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof GraphSection)) {
            return false;
        }

        final GraphSection otherGraphSection = (GraphSection) obj;
        return titles.size() == otherGraphSection.titles.size() && values.equals(otherGraphSection.values);

    }

    @Override
    public String toString() {
        return "GraphSection{" +
                "\nvalues=" + Arrays.toString(values.toArray()) +
                ", \ntitles=" + Arrays.toString(titles.toArray()) +
                ", \nhighlightedValues=" + Arrays.toString(highlightedValues.toArray()) +
                ", \nhighlightedTitle=" + highlightedTitle +
                "\n}";
    }

}
