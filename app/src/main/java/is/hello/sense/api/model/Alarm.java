package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.util.DateFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm extends ApiResponse {
    public static final int FUTURE_CUT_OFF_MINUTES = 5;


    @JsonProperty("id")
    private String id;

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
    private boolean repeated;


    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("editable")
    private boolean editable;


    @JsonProperty("day_of_week")
    private Set<Integer> daysOfWeek;

    @JsonProperty("sound")
    private Sound sound;

    @JsonProperty("smart")
    private boolean smart;


    public Alarm() {
        this.id = UUID.randomUUID().toString();
        this.hourOfDay = 7;
        this.minuteOfHour = 30;
        this.repeated = true;
        this.enabled = true;
        this.editable = true;
        this.smart = true;
        this.daysOfWeek = new HashSet<>();
    }


    public String getId() {
        return id;
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
    public DateTime getExpectedRingTime() {
        return new DateTime(year, month, dayOfMonth, hourOfDay, minuteOfHour, DateTimeZone.UTC);
    }

    @JsonIgnore
    public void setRingOnce() {
        DateTime today = DateFormatter.now();
        if (getTime().isBefore(today.toLocalTime())) {
            DateTime tomorrow = today.plusDays(1);
            this.year = tomorrow.getYear();
            this.month = tomorrow.getMonthOfYear();
            this.dayOfMonth = tomorrow.getDayOfMonth();
        } else {
            this.year = today.getYear();
            this.month = today.getMonthOfYear();
            this.dayOfMonth = today.getDayOfMonth();
        }

        setRepeated(false);
        getDaysOfWeek().clear();
    }


    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean isRepeated) {
        this.repeated = isRepeated;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.enabled = isEnabled;
    }

    public boolean isSmart() {
        return this.smart;
    }

    public void setSmart(final boolean isSmart) {
        this.smart = isSmart;
    }

    public boolean isEditable() {
        return editable;
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
        if (Lists.isEmpty(daysOfWeek)) {
            if (isSmart()) {
                return context.getString(R.string.smart_alarm_never);
            } else {
                return context.getString(R.string.alarm_never);
            }
        }

        List<Integer> orderedDays = Lists.sorted(daysOfWeek, (leftDay, rightDay) -> {
            // Joda-Time considers Sunday to be day 7, which is generally
            // not how it works in English speaking countries.
            int leftCorrected = (leftDay == DateTimeConstants.SUNDAY) ? 0 : leftDay;
            int rightCorrected = (rightDay == DateTimeConstants.SUNDAY) ? 0 : rightDay;
            return Functions.compareInts(leftCorrected, rightCorrected);
        });
        List<String> days = Lists.map(orderedDays, day -> nameForDayOfWeek(context, day));
        String daysString = TextUtils.join(context.getString(R.string.alarm_day_separator), days);
        if (isSmart()) {
            return context.getString(R.string.smart_alarm_days_repeat_prefix) + daysString;
        } else {
            return context.getString(R.string.alarm_days_repeat_prefix) + daysString;
        }
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }


    @Override
    public String toString() {
        return "Alarm{" +
                "id='" + id + '\'' +
                ", year=" + year +
                ", month=" + month +
                ", dayOfMonth=" + dayOfMonth +
                ", hourOfDay=" + hourOfDay +
                ", minuteOfHour=" + minuteOfHour +
                ", repeated=" + repeated +
                ", enabled=" + enabled +
                ", editable=" + editable +
                ", daysOfWeek=" + daysOfWeek +
                ", sound=" + sound +
                ", smart=" + smart +
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
