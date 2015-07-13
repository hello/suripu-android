package is.hello.sense.api.model.v2;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.markup.text.MarkupString;

public class TimelineEventBuilder {
    private final TimelineEvent event = new TimelineEvent();

    public static TimelineEvent randomEvent(@NonNull Random random, @NonNull TimelineEvent.Type type) {
        TimelineEventBuilder builder = new TimelineEventBuilder();
        builder.setDuration(random.nextInt(3600), TimeUnit.SECONDS);

        TimelineEvent.SleepState[] sleepStates = TimelineEvent.SleepState.values();
        builder.setSleepDepth(random.nextInt(100), sleepStates[random.nextInt(sleepStates.length)]);

        TimelineEvent.Type[] types = TimelineEvent.Type.values();
        builder.setType(type);

        TimelineEvent.Action[] actions = TimelineEvent.Action.values();
        for (int i = 0, limit = random.nextInt(actions.length); i < limit; i++) {
            builder.addAction(actions[i]);
        }

        return builder.build();
    }

    public static TimelineEvent randomEvent(@NonNull Random random) {
        TimelineEvent.Type[] types = TimelineEvent.Type.values();
        TimelineEvent.Type type = types[random.nextInt(types.length)];
        return randomEvent(random, type);
    }

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
