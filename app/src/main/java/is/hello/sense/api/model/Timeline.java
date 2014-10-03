package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Timeline extends ApiResponse {
    @JsonProperty("score")
    private int score;

    @JsonProperty("message")
    private String message;

    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-M-d")
    private DateTime date;

    @JsonProperty("segments")
    @JsonDeserialize(contentAs = TimelineSegment.class)
    private List<TimelineSegment> segments;


    public int getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }

    public DateTime getDate() {
        return date;
    }

    public List<TimelineSegment> getSegments() {
        return segments;
    }


    public HashMap<String, TimelineSensor> calculateAverageSensorReadings() {
        HashMap<String, TimelineSensor> averages = new HashMap<>();

        for (TimelineSegment segment : getSegments()) {
            List<TimelineSensor> sensors = segment.getSensors();
            for (TimelineSensor sensor : sensors) {
                TimelineSensor average = averages.get(sensor.getName());
                if (average == null) {
                    average = new TimelineSensor();
                    average.setName(sensor.getName());
                    average.setUnit(sensor.getUnit());
                    averages.put(sensor.getName(), average);
                }

                average.setValue(sensor.getValue());
            }
        }

        int total = getSegments().size();
        for (Map.Entry<String, TimelineSensor> averagePair : averages.entrySet()) {
            TimelineSensor sensorAverage = averagePair.getValue();
            sensorAverage.setValue(sensorAverage.getValue() / total);
        }

        return averages;
    }


    @Override
    public String toString() {
        return "Timeline{" +
                "score=" + score +
                ", message='" + message + '\'' +
                ", date='" + date + '\'' +
                ", segments=" + segments +
                '}';
    }
}
