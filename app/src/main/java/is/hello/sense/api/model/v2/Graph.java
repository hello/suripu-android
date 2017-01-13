package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;
import is.hello.sense.ui.widget.graphing.trends.BarTrendGraphView;
import is.hello.sense.ui.widget.graphing.trends.BubbleTrendGraphView;
import is.hello.sense.ui.widget.graphing.trends.GridTrendGraphView;
import is.hello.sense.ui.widget.util.Styles;

public class Graph extends ApiResponse {
    @SerializedName("time_scale")
    private Trends.TimeScale timeScale;

    @SerializedName("title")
    private String title;

    @SerializedName("data_type")
    private DataType dataType;

    @SerializedName("graph_type")
    private GraphType graphType;

    @SerializedName("min_value")
    private float minValue;

    @SerializedName("max_value")
    private float maxValue;

    @SerializedName("sections")
    private List<GraphSection> sections;

    @SerializedName("condition_ranges")
    @VisibleForTesting
    List<ConditionRange> conditionRanges;

    @SerializedName("annotations")
    private List<Annotation> annotations;

    private ArrayList<Graph> quarterGraphs;
    private int quarterSections = 0;

    @VisibleForTesting
    public Graph(@NonNull final String title,
                 @NonNull final DataType dataType,
                 @NonNull final GraphType graphType) {
        this.title = title;
        this.dataType = dataType;
        this.graphType = graphType;
    }

    public Graph(final Graph graph) {
        this.timeScale = graph.timeScale;
        this.title = graph.title;
        this.dataType = graph.dataType;
        this.graphType = graph.graphType;
        this.minValue = graph.minValue;
        this.maxValue = graph.maxValue;
        this.sections = new ArrayList<>();
        this.conditionRanges = graph.conditionRanges;
        this.annotations = graph.annotations;

    }

    public ArrayList<Graph> getQuarterGraphs() {
        if (quarterGraphs == null) {
            quarterGraphs = convertToQuarterGraphs();
        }
        return quarterGraphs;
    }

    public int getQuarterSections() {
        if (quarterSections == 0) {
            int sections = 0;
            final ArrayList<Graph> graphs = getQuarterGraphs();
            for (int i = 0; i < graphs.size(); i += 2) {
                final Graph quarterGraph = graphs.get(i);
                sections += quarterGraph.getSections().size();
            }
            for (int i = 1; i < graphs.size(); i += 2) {
                final Graph quarterGraph = graphs.get(i);
                quarterSections += quarterGraph.getSections().size();
            }
            if (sections > quarterSections) {
                quarterSections = sections;
            }
        }
        return quarterSections;
    }

    /**
     * The quarter graph response from /v2/trends/LAST_3_MONTHS uses each {@link GraphSection} to
     * represent one month of data, rather than one week. This method will break apart each
     * {@link GraphSection} by creating a new one for every 7 days of data and then add them to a
     * new {@link Graph}.
     *
     * @return List of Graphs, each to be used with a seperate {@link GridTrendGraphView}
     */
    public ArrayList<Graph> convertToQuarterGraphs() {
        final ArrayList<Graph> graphs = new ArrayList<>();
        final int DAYS_IN_WEEK = 7;
        for (final GraphSection graphSection : sections) {
            final int offset = graphSection.getFirstDayOfMonthOffset();
            final Graph graph = new Graph(this);
            if (offset > 0) {
                final GraphSection temp = GraphSection.withHighlightedTitle(graphSection)
                                                      .withDoNotShowValues(offset);
                graph.addSection(temp);
            }
            for (int i = offset; i < graphSection.getValues().size() + offset; i++) {
                final GraphSection temp;
                if (i % DAYS_IN_WEEK == 0) {
                    temp = GraphSection.withHighlightedTitle(graphSection);
                    graph.addSection(temp);
                } else {
                    temp = graph.getSection(graph.getSections().size() - 1);
                }
                temp.addValue(graphSection.getValue(i - offset));

            }
            for (int i = 0; i < graphSection.getTitles().size(); i++) {
                final String title = graphSection.getTitles().get(i);
                graph.getSection(i).addTitle(title);
            }
            for (int highlightedIndex : graphSection.getHighlightedValues()) {
                highlightedIndex += offset;
                final int sectionIndex = highlightedIndex / DAYS_IN_WEEK;
                final int cell = highlightedIndex % DAYS_IN_WEEK;
                graph.getSection(sectionIndex).addHighlightedValues(cell);
            }

            graphs.add(graph);
        }
        return graphs;
    }

    public void addSection(final GraphSection section) {
        this.sections.add(section);
    }

    public Trends.TimeScale getTimeScale() {
        return timeScale;
    }

    public String getTitle() {
        return title;
    }

    public DataType getDataType() {
        return dataType;
    }

    public GraphType getGraphType() {
        return graphType;
    }


    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public List<GraphSection> getSections() {
        return sections;
    }


    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public GraphSection getSection(final int index) {
        return sections.get(index);
    }

    public Condition getConditionForValue(final float value) {
        for (final ConditionRange conditionRange : conditionRanges) {
            if (value >= conditionRange.getMinValue() && value <= conditionRange.getMaxValue()) {
                return conditionRange.getCondition();
            }
        }

        return Condition.UNKNOWN;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Graph)) {
            return false;
        }
        final Graph otherGraph = (Graph) obj;
        if (title == null || !title.equals(otherGraph.title)) {
            return false;
        }
        if (timeScale != otherGraph.timeScale) {
            return false;
        }
        return minValue == otherGraph.minValue && maxValue == otherGraph.maxValue;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "timeScale=" + timeScale.toString() +
                ", title='" + title + '\'' +
                ", dataType='" + dataType + '\'' +
                ", graphType='" + graphType.toString() + '\'' +
                ", minValue='" + minValue + '\'' +
                ", maxValue='" + maxValue + '\'' +
                ", sections='" + sections.toString() + '\'' +
                ", conditionRanges='" + conditionRanges.toString() + '\'' +
                ", annotations='" + annotations.toString() + '\'' +
                '}';
    }

    public enum GraphType implements Enums.FromString {
        NO_DATA,
        EMPTY,
        GRID,
        BAR,
        BUBBLES;

        public static GraphType fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), EMPTY);
        }
    }

    public enum DataType implements Enums.FromString {
        NONE,
        SCORES,
        HOURS {
            @Override
            public CharSequence renderAnnotation(@NonNull final Annotation annotation) {
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue(), 1),
                                                     BarTrendGraphView.BarGraphDrawable.HOUR_SYMBOL,
                                                     Styles.UNIT_STYLE_SUBSCRIPT);
            }
        },
        PERCENTS {
            @Override
            public CharSequence renderAnnotation(@NonNull final Annotation annotation) {
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue() * 100, 0),
                                                     BubbleTrendGraphView.BubbleGraphDrawable.PERCENT_SYMBOL,
                                                     Styles.UNIT_STYLE_SUBSCRIPT);
            }
        };

        public boolean wantsConditionTinting() {
            return (this == SCORES);
        }

        public CharSequence renderAnnotation(@NonNull final Annotation annotation) {
            return Styles.createTextValue(annotation.getValue(), 0);
        }

        public static DataType fromString(@Nullable final String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }

}
