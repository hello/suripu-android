package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.LocalTime;

import java.util.Set;

public class SmartAlarm extends ApiResponse {
    @JsonProperty("hour")
    private int hourOfDay;

    @JsonProperty("minute")
    private int minuteOfHour;

    @JsonProperty("repeated")
    private boolean isRepeated;


    @JsonProperty("enabled")
    private boolean isEnabled;

    @JsonProperty("editable")
    private boolean isEditable;


    @JsonProperty("day_of_week")
    private Set<Integer> daysOfWeek;

    @JsonProperty("sound")
    private Sound sound;


    public LocalTime getTime() {
        return new LocalTime(hourOfDay, minuteOfHour);
    }

    public void setTime(@NonNull LocalTime time) {
        this.hourOfDay = time.getHourOfDay();
        this.minuteOfHour = time.getMinuteOfHour();
    }


    public boolean isRepeated() {
        return isRepeated;
    }

    public void setRepeated(boolean isRepeated) {
        this.isRepeated = isRepeated;
    }


    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }


    public boolean isEditable() {
        return isEditable;
    }

    public Set<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    /**
     * @see org.joda.time.DateTimeConstants
     */
    public void setDaysOfWeek(Set<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }


    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }


    @Override
    public String toString() {
        return "SmartAlarm{" +
                "hourOfDay=" + hourOfDay +
                ", minuteOfHour=" + minuteOfHour +
                ", isRepeated=" + isRepeated +
                ", isEnabled=" + isEnabled +
                ", isEditable=" + isEditable +
                ", daysOfWeek=" + daysOfWeek +
                ", sound=" + sound +
                '}';
    }

    public static class Sound {
        @JsonProperty("id")
        public final long id;

        @JsonProperty("name")
        public final String name;


        public Sound(@JsonProperty("id") long id,
                     @JsonProperty("name") String name) {
            this.id = id;
            this.name = name;
        }


        @Override
        public String toString() {
            return "Sound{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}
