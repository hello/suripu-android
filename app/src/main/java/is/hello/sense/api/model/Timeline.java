package is.hello.sense.api.model;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.DateTime;

import java.util.List;

import is.hello.sense.api.ApiService;
import is.hello.sense.util.markup.text.MarkupString;

@Deprecated
public class Timeline extends ApiResponse {
    @JsonProperty("score")
    private int score;

    @JsonProperty("message")
    private MarkupString message;

    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private DateTime date;

    @JsonProperty("segments")
    @JsonDeserialize(contentAs = TimelineSegment.class)
    private List<TimelineSegment> segments;

    @JsonProperty("insights")
    @JsonDeserialize(contentAs = PreSleepInsight.class)
    private List<PreSleepInsight> preSleepInsights;

    @JsonProperty("statistics")
    private Statistics statistics;


    public int getScore() {
        return score;
    }

    public MarkupString getMessage() {
        return message;
    }

    public DateTime getDate() {
        return date;
    }

    public List<TimelineSegment> getSegments() {
        return segments;
    }

    public List<PreSleepInsight> getPreSleepInsights() {
        return preSleepInsights;
    }

    public Statistics getStatistics() {
        return statistics;
    }


    @Override
    public String toString() {
        return "Timeline{" +
                "score=" + score +
                ", message='" + message + '\'' +
                ", date=" + date +
                ", segments=" + segments +
                ", preSleepInsights=" + preSleepInsights +
                ", statistics=" + statistics +
                '}';
    }


    public static class Statistics extends ApiResponse {
        @JsonProperty("total_sleep")
        private Integer totalSleep;

        @JsonProperty("sound_sleep")
        private Integer soundSleep;

        @JsonProperty("times_awake")
        private Integer timesAwake;

        @JsonProperty("time_to_sleep")
        private Integer timeToSleep;


        public @Nullable Integer getTotalSleep() {
            return totalSleep;
        }

        public @Nullable Integer getSoundSleep() {
            return soundSleep;
        }

        public @Nullable Integer getTimesAwake() {
            return timesAwake;
        }

        public @Nullable Integer getTimeToSleep() {
            return timeToSleep;
        }


        @Override
        public String toString() {
            return "Statistics{" +
                    "totalSleep=" + totalSleep +
                    ", soundSleep=" + soundSleep +
                    ", timesAwake=" + timesAwake +
                    ", timeToSleep=" + timeToSleep +
                    '}';
        }
    }
}
