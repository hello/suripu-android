package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.markup.text.MarkupString;

public class TimelineEventBuilder {
    private final TimelineEvent event = new TimelineEvent();

    public TimelineEventBuilder() {
        event.validActions = new ArrayList<>();
        setShiftedTimestamp(DateFormatter.now());
        setDuration(60, TimeUnit.SECONDS);
        setSleepDepth(60, TimelineEvent.SleepState.MEDIUM);
        setType(TimelineEvent.Type.IN_BED);
    }

    public TimelineEventBuilder setShiftedTimestamp(DateTime dateTime) {
        event.timestamp = dateTime.withZone(DateTimeZone.UTC);
        event.timezoneOffset = dateTime.getZone().getOffset(Instant.now());
        return this;
    }

    public TimelineEventBuilder setDuration(int duration, @NonNull TimeUnit unit) {
        event.durationMillis = unit.toMillis(duration);
        return this;
    }

    public TimelineEventBuilder setMessage(MarkupString message) {
        event.message = message;
        return this;
    }

    public TimelineEventBuilder setSleepDepth(int depth, TimelineEvent.SleepState sleepState) {
        event.sleepDepth = depth;
        event.sleepState = sleepState;
        return this;
    }

    public TimelineEventBuilder setType(TimelineEvent.Type eventType) {
        event.type = eventType;
        return this;
    }

    public TimelineEventBuilder addAction(@NonNull TimelineEvent.Action action) {
        event.validActions.add(action);
        return this;
    }

    public TimelineEvent build() {
        return event;
    }
}
