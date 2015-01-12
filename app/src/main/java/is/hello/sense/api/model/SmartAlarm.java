package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartAlarm extends ApiResponse {
    @JsonProperty("year")
    private int year;

    @JsonProperty("month")
    private int month;

    @JsonProperty("day_of_month")
    private int dayOfMonth;

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


    public SmartAlarm() {
        this.hourOfDay = 7;
        this.minuteOfHour = 30;
        this.isRepeated = true;
        this.isEnabled = true;
        this.isEditable = true;
        this.daysOfWeek = new HashSet<>();
    }


    @JsonIgnore
    public LocalTime getTime() {
        return new LocalTime(hourOfDay, minuteOfHour);
    }

    @JsonIgnore
    public void setTime(@NonNull LocalTime time) {
        this.hourOfDay = time.getHourOfDay();
        this.minuteOfHour = time.getMinuteOfHour();
    }

    @JsonIgnore
    public void fireOnceTomorrow() {
        DateTime tomorrow = DateTime.now().withTimeAtStartOfDay().plusDays(1);
        this.year = tomorrow.getYear();
        this.month = tomorrow.getMonthOfYear();
        this.dayOfMonth = tomorrow.getDayOfMonth();

        setRepeated(false);
        getDaysOfWeek().clear();
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

    /**
     * @see org.joda.time.DateTimeConstants
     */
    public Set<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public static @NonNull String nameForDayOfWeek(@NonNull Context context, int dayOfWeek) {
        switch (dayOfWeek) {
            case DateTimeConstants.MONDAY:
                return context.getString(R.string.day_monday);

            case DateTimeConstants.TUESDAY:
                return context.getString(R.string.day_tuesday);

            case DateTimeConstants.WEDNESDAY:
                return context.getString(R.string.day_wednesday);

            case DateTimeConstants.THURSDAY:
                return context.getString(R.string.day_thursday);

            case DateTimeConstants.FRIDAY:
                return context.getString(R.string.day_friday);

            case DateTimeConstants.SATURDAY:
                return context.getString(R.string.day_saturday);

            case DateTimeConstants.SUNDAY:
                return context.getString(R.string.day_sunday);

            default:
                throw new IllegalArgumentException("Unknown day of week " + dayOfWeek);
        }
    }

    public @NonNull String getDaysOfWeekSummary(@NonNull Context context) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return context.getString(R.string.smart_alarm_non_repeating);
        }

        List<String> days = new ArrayList<>();
        for (Integer dayOfWeek : daysOfWeek) {
            days.add(nameForDayOfWeek(context, dayOfWeek));
        }

        return context.getString(R.string.smart_alarm_days_repeat_prefix) + TextUtils.join(context.getString(R.string.smart_alarm_day_separator), days);
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
                "year=" + year +
                ", month=" + month +
                ", dayOfMonth=" + dayOfMonth +
                ", hourOfDay=" + hourOfDay +
                ", minuteOfHour=" + minuteOfHour +
                ", isRepeated=" + isRepeated +
                ", isEnabled=" + isEnabled +
                ", isEditable=" + isEditable +
                ", daysOfWeek=" + daysOfWeek +
                ", sound=" + sound +
                '}';
    }

    public static class Sound extends ApiResponse {
        @JsonProperty("id")
        public final long id;

        @JsonProperty("name")
        public final String name;

        @JsonProperty("url")
        public final String url;


        public Sound(@JsonProperty("id") long id,
                     @JsonProperty("name") String name,
                     @JsonProperty("url") String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }


        @Override
        public String toString() {
            return "Sound{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }
}
