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
import is.hello.sense.ui.widget.BarTrendGraphView;
import is.hello.sense.ui.widget.BubbleTrendGraphView;
import is.hello.sense.ui.widget.GridTrendGraphView;
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
    private ArrayList<GraphSection> sections;

    @SerializedName("condition_ranges")
    @VisibleForTesting
    List<ConditionRange> conditionRanges;

    @SerializedName("annotations")
    private List<Annotation> annotations;

    @VisibleForTesting
    public Graph(@NonNull String title,
                 @NonNull DataType dataType,
                 @NonNull GraphType graphType) {
        this.title = title;
        this.dataType = dataType;
        this.graphType = graphType;
    }

    public Graph(Graph graph) {
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

    /**
     * The quarter graph response from /v2/trends/LAST_3_MONTHS uses each {@link GraphSection} to
     * represent one month of data, rather than one week. This method will break apart each
     * {@link GraphSection} by creating a new one for every 7 days of data and then add them to a
     * new {@link Graph}.
     *
     * @return List of Graphs, each to be used with a seperate {@link GridTrendGraphView}
     */
    public ArrayList<Graph> convertToQuarterGraphs() {
        ArrayList<Graph> graphs = new ArrayList<>();
        for (GraphSection graphSection : sections) {
            Graph graph = new Graph(this);
            for (int i = 0; i < graphSection.getValues().size(); i++) {
                final GraphSection temp;
                if (i % 7 == 0) {
                    temp = new GraphSection(graphSection);
                    graph.addSection(temp);
                } else {
                    temp = graph.getSections().get(graph.getSections().size() - 1);
                }
                temp.addValue(graphSection.getValues().get(i));
            }
            for (int highlightedIndex : graphSection.getHighlightedValues()) {
                int section = highlightedIndex / 6;
                int cell = highlightedIndex % 7;
                graph.getSections().get(section).addHighlightedValues(cell);
            }
            graphs.add(graph);
        }
        return graphs;
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

    public boolean isGrid() {
        return getGraphType() == GraphType.GRID;
    }

    public float getMinValue() {
        return minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public ArrayList<GraphSection> getSections() {
        return sections;
    }

    public void addSection(GraphSection section) {
        this.sections.add(section);
    }

    public List<ConditionRange> getConditionRanges() {
        return conditionRanges;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public Condition getConditionForValue(float value) {
        for (final ConditionRange conditionRange : conditionRanges) {
            if (value >= conditionRange.getMinValue() && value <= conditionRange.getMaxValue()) {
                return conditionRange.getCondition();
            }
        }

        return Condition.UNKNOWN;
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

        public static GraphType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), EMPTY);
        }
    }

    public enum DataType implements Enums.FromString {
        NONE,
        SCORES,
        HOURS {
            @Override
            public CharSequence renderAnnotation(@NonNull Annotation annotation) {
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue(), 2),
                                                     BarTrendGraphView.BarGraphDrawable.HOUR_SYMBOL,
                                                     Styles.UNIT_STYLE_SUBSCRIPT);
            }
        },
        PERCENTS {
            @Override
            public CharSequence renderAnnotation(@NonNull Annotation annotation) {
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue() * 100, 0),
                                                     BubbleTrendGraphView.BubbleGraphDrawable.PERCENT_SYMBOL,
                                                     Styles.UNIT_STYLE_SUBSCRIPT);
            }
        };

        public boolean wantsConditionTinting() {
            return (this == SCORES);
        }

        public CharSequence renderAnnotation(@NonNull Annotation annotation) {
            return Styles.createTextValue(annotation.getValue(), 0);
        }

        public static DataType fromString(@Nullable String string) {
            return Enums.fromString(string, values(), NONE);
        }
    }

}
