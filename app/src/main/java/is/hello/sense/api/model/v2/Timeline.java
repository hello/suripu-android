package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.jar.Attributes;

import is.hello.sense.api.model.ApiResponse;
import is.hello.sense.util.markup.text.MarkupString;

public class Timeline extends ApiResponse {
    @VisibleForTesting
    @SerializedName("score")
    @Nullable Integer score;

    @VisibleForTesting
    @SerializedName("score_condition")
    ScoreCondition scoreCondition;

    @VisibleForTesting
    @SerializedName("message")
    MarkupString message;

    @VisibleForTesting
    @SerializedName("date")
    LocalDate date;

    @VisibleForTesting
    @SerializedName("events")
    ArrayList<TimelineEvent> events;

    @VisibleForTesting
    @SerializedName("metrics")
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

    public LocalDate getDate() {
        return date;
    }

    public ArrayList<TimelineEvent> getEvents() {
        return events;
    }

    public ArrayList<TimelineMetric> getMetrics() {
        return metrics;
    }

    public TimelineMetric getMetricWithName(@NonNull final TimelineMetric.Name name) {
        for (final TimelineMetric metric : metrics) {
            if (metric.getName().equals(name)) {
                return metric;
            }
        }
        return null;
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
