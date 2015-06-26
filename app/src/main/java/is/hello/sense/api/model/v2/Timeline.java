package is.hello.sense.api.model.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.api.model.Condition;
import is.hello.sense.util.markup.text.MarkupString;

public class Timeline extends ApiResponse {
    @JsonProperty("score")
    private int score;

    @JsonProperty("score_condition")
    private Condition scoreCondition;

    @JsonProperty("message")
    private MarkupString message;

    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private DateTime date;

    @JsonProperty("events")
    private ArrayList<TimelineEvent> events;

    @JsonProperty("metrics")
    private ArrayList<TimelineMetric> metrics;


    public int getScore() {
        return score;
    }

    public Condition getScoreCondition() {
        return scoreCondition;
    }

    public MarkupString getMessage() {
        return message;
    }

    public DateTime getDate() {
        return date;
    }

    public ArrayList<TimelineEvent> getEvents() {
        return events;
    }

    public ArrayList<TimelineMetric> getMetrics() {
        return metrics;
    }


    @Override
    public String toString() {
        return "Timeline{" +
                "score=" + score +
                ", scoreCondition=" + scoreCondition +
                ", message=" + message +
                ", date=" + date +
                ", events=" + events +
                ", metrics=" + metrics +
                '}';
    }
}
