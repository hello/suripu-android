package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import is.hello.sense.api.gson.Enums;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;
import is.hello.sense.ui.widget.graphing.BarTrendGraphView;
import is.hello.sense.ui.widget.graphing.BubbleTrendGraphView;
import is.hello.sense.ui.widget.graphing.GridTrendGraphView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;

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
                Graph quarterGraph = graphs.get(i);
                sections += quarterGraph.getSections().size();
            }
            for (int i = 1; i < graphs.size(); i += 2) {
                Graph quarterGraph = graphs.get(i);
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
        for (GraphSection graphSection : sections) {
            final String monthTitle = graphSection.getTitles().get(0);
            int offset = 0;
            try {
                final int monthValue = DateFormatter.getMonthInt(monthTitle);
                offset = DateFormatter.getFirstDayOfMonthValue(monthValue) - 1;
            } catch (ParseException e) {
                Log.e(getClass().getName(), "Problem parsing month: " + e.getLocalizedMessage());
            }
            final Graph graph = new Graph(this);
            if (offset > 0) {
                final GraphSection temp = new GraphSection(graphSection);
                graph.addSection(temp);
                for (int i = 0; i < offset; i++) {
                    temp.addValue(-2f);
                }
            }
            for (int i = offset; i < graphSection.getValues().size() + offset; i++) {
                final GraphSection temp;
                if (i % 7 == 0) {
                    temp = new GraphSection(graphSection);
                    graph.addSection(temp);
                } else {
                    temp = graph.getSections().get(graph.getSections().size() - 1);
                }
                temp.addValue(graphSection.getValues().get(i - offset));

            }
            for (int i = 0; i < graphSection.getTitles().size(); i++) {
                final String title = graphSection.getTitles().get(i);
                graph.getSections().get(i).addTitle(title);
            }
            for (int highlightedIndex : graphSection.getHighlightedValues()) {
                highlightedIndex += offset;
                final int section = highlightedIndex / 7;
                final int cell = highlightedIndex % 7;
                graph.getSections().get(section).addHighlightedValues(cell);
            }
            graphs.add(graph);
        }
        return graphs;
    }

    public void addSection(GraphSection section) {
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

    public boolean isGrid() {
        return getGraphType() == GraphType.GRID;
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
                return Styles.assembleReadingAndUnit(Styles.createTextValue(annotation.getValue(), 1),
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
