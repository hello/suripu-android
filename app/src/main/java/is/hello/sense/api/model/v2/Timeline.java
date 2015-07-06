package is.hello.sense.api.model.v2;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import java.util.ArrayList;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.markup.text.MarkupString;

public class Timeline extends ApiResponse {
    @VisibleForTesting
    @JsonProperty("score")
    @Nullable Integer score;

    @VisibleForTesting
    @JsonProperty("score_condition")
    ScoreCondition scoreCondition;

    @VisibleForTesting
    @JsonProperty("message")
    MarkupString message;

    @VisibleForTesting
    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    DateTime date;

    @VisibleForTesting
    @JsonProperty("events")
    ArrayList<TimelineEvent> events;

    @VisibleForTesting
    @JsonProperty("metrics")
    ArrayList<TimelineMetric> metrics;


    public @Nullable Integer getScore() {
        return score;
    }

    public ScoreCondition getScoreCondition() {
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
