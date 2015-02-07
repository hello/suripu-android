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
    private boolean isRepeated;


    @JsonProperty("enabled")
    private boolean isEnabled;

    @JsonProperty("editable")
    private boolean isEditable;


    @JsonProperty("day_of_week")
    private Set<Integer> daysOfWeek;

    @JsonProperty("sound")
    private Sound sound;

    @JsonProperty("smart")
    private boolean isSmart;


    //region Validation

    /**
     * Make sure there is only one alarm per user per day
     */
    public static boolean validateAlarms(final List<Alarm> alarms) {
        final Set<Integer> alarmDays = new HashSet<>();
        for (final Alarm alarm : alarms) {
            if (!alarm.isEnabled()) {
                continue;
            }

            if (!alarm.isRepeated) {
                if (!validateNonRepeatingAlarm(alarm)) {
                    return false;
                }

                if (alarm.isSmart) {
                    final DateTime expectedRingTime = new DateTime(alarm.year, alarm.month, alarm.dayOfMonth, alarm.hourOfDay, alarm.minuteOfHour, DateTimeZone.UTC);
                    if (alarmDays.contains(expectedRingTime.getDayOfWeek())) {
                        return false;
                    }

                    alarmDays.add(expectedRingTime.getDayOfWeek());
                }
            } else {
                if (!alarm.isSmart) {
                    continue;
                }

                for (final Integer dayOfWeek : alarm.getDaysOfWeek()) {
                    if (alarmDays.contains(dayOfWeek)) {
                        return false;
                    }

                    alarmDays.add(dayOfWeek);
                }

            }
        }

        return true;
    }

    public static boolean validateNonRepeatingAlarm(final Alarm alarm) {
        if (alarm.isRepeated) {
            return true;
        }

        try {
            new DateTime(alarm.year, alarm.month, alarm.dayOfMonth, alarm.hourOfDay, alarm.minuteOfHour, DateTimeZone.UTC);
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    public boolean isTooSoon() {
        LocalTime now = LocalTime.now(DateTimeZone.getDefault());
        int minuteCutOff = now.getMinuteOfHour() + FUTURE_CUT_OFF_MINUTES;
        LocalTime alarmTime = getTime();
        return (alarmTime.getHourOfDay() == now.getHourOfDay() &&
                alarmTime.getMinuteOfHour() >= now.getMinuteOfHour() &&
                alarmTime.getMinuteOfHour() <= minuteCutOff);
    }

    //endregion


    public Alarm() {
        this.id = UUID.randomUUID().toString();
        this.hourOfDay = 7;
        this.minuteOfHour = 30;
        this.isRepeated = true;
        this.isEnabled = true;
        this.isEditable = true;
        this.isSmart = true;
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
    public void fireOnce() {
        DateTime today = DateTime.now(DateTimeZone.getDefault());
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

    public boolean isSmart() {
        return this.isSmart;
    }

    public void setSmart(final boolean isSmart) {
        this.isSmart = isSmart;
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
        if (Lists.isEmpty(daysOfWeek)) {
            if (isSmart()) {
                return context.getString(R.string.alarm_never);
            } else {
                DateTime today = DateTime.now(DateTimeZone.getDefault());
                if (year == today.getYear() && month == today.getMonthOfYear() && dayOfMonth == today.getDayOfMonth()) {
                    return context.getString(R.string.alarm_today);
                } else {
                    return context.getString(R.string.alarm_tomorrow);
                }
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
                ", isRepeated=" + isRepeated +
                ", isEnabled=" + isEnabled +
                ", isEditable=" + isEditable +
                ", daysOfWeek=" + daysOfWeek +
                ", sound=" + sound +
                ", isSmart=" + isSmart +
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
