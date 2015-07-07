package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.ArrayList;

import is.hello.sense.util.markup.text.MarkupString;

public class TimelineBuilder {
    private final Timeline timeline = new Timeline();

    public TimelineBuilder() {
        timeline.events = new ArrayList<>();
        timeline.metrics = new ArrayList<>();
    }

    public TimelineBuilder setScore(int score, @NonNull ScoreCondition condition) {
        timeline.score = score;
        timeline.scoreCondition = condition;
        return this;
    }

    public TimelineBuilder setMessage(MarkupString message) {
        timeline.message = message;
        return this;
    }

    public TimelineBuilder setDate(DateTime date) {
        timeline.date = date;
        return this;
    }

    public TimelineBuilder addEvent(TimelineEvent event) {
        timeline.events.add(event);
        return this;
    }

    public TimelineBuilder addMetric(TimelineMetric metric) {
        timeline.metrics.add(metric);
        return this;
    }

    public Timeline build() {
        return timeline;
    }
}
