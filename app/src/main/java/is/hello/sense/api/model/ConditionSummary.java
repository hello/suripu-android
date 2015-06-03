package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import is.hello.sense.util.Markdown;

public class ConditionSummary {
    @JsonProperty("average_condition")
    private Condition averageCondition;

    @JsonProperty("bad_sensor")
    private PreSleepInsight.Sensor badSensor;

    @JsonProperty("bad_sensor_condition")
    private Condition badSensorCondition;

    @JsonProperty("message")
    private CharSequence message;


    public static ConditionSummary calculateSummary(@NonNull Markdown markdown, @NonNull List<PreSleepInsight> insights) {
        int conditionSum = 0;
        PreSleepInsight.Sensor badSensor = PreSleepInsight.Sensor.UNKNOWN;
        Condition badSensorCondition = Condition.IDEAL;
        CharSequence message = "All of the conditions in your room were good last night.";
        for (PreSleepInsight insight : insights) {
            conditionSum += insight.getCondition().ordinal();

            if (insight.getCondition() != Condition.IDEAL) {
                badSensor = insight.getSensor();
                message = markdown.toSpanned(insight.getMessage());
                badSensorCondition = insight.getCondition();
            }
        }

        int conditionAverage = (int) Math.ceil(conditionSum / (float) insights.size());
        int conditionOrdinal = Math.min(Condition.IDEAL.ordinal(), conditionAverage);
        Condition averageCondition = Condition.values()[conditionOrdinal];

        return new ConditionSummary(averageCondition, badSensor, badSensorCondition, message);
    }

    public ConditionSummary(@NonNull Condition averageCondition,
                            @NonNull PreSleepInsight.Sensor badSensor,
                            @NonNull Condition badSensorCondition,
                            @NonNull CharSequence message) {
        this.averageCondition = averageCondition;
        this.badSensor = badSensor;
        this.badSensorCondition = badSensorCondition;
        this.message = message;
    }

    public Condition getAverageCondition() {
        return averageCondition;
    }

    public PreSleepInsight.Sensor getBadSensor() {
        return badSensor;
    }

    public Condition getBadSensorCondition() {
        return badSensorCondition;
    }

    public CharSequence getMessage() {
        return message;
    }
}
