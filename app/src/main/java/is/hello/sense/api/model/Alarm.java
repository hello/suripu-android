package is.hello.sense.api.model;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.IllegalInstantException;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import is.hello.sense.BuildConfig;
import is.hello.sense.R;
import is.hello.sense.api.model.v2.alarms.AlarmSource;
import is.hello.sense.api.model.v2.expansions.Category;
import is.hello.sense.api.model.v2.expansions.ExpansionAlarm;
import is.hello.sense.functional.Functions;
import is.hello.sense.functional.Lists;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.DateFormatter.JodaWeekDay;
import is.hello.sense.util.IListObject;

public class Alarm extends ApiResponse {
    public static final int TOO_SOON_MINUTES = 2;

    @SerializedName("id")
    private String id;

    @SerializedName("year")
    private int year;

    @SerializedName("month")
    private int month;

    @SerializedName("day_of_month")
    private int dayOfMonth;

    @SerializedName("hour")
    private int hourOfDay;

    @SerializedName("minute")
    private int minuteOfHour;

    @SerializedName("repeated")
    private boolean repeated;


    @SerializedName("enabled")
    private boolean enabled;

    @SerializedName("editable")
    private boolean editable;


    @SerializedName("day_of_week")
    private Set<Integer> daysOfWeek;

    @SerializedName("sound")
    private Sound sound;

    @SerializedName("smart")
    private boolean smart;

    @SerializedName("source")
    private AlarmSource source;

    @SerializedName("expansions")
    private List<ExpansionAlarm> expansions;

    private transient AlarmTones alarmTones;


    public Alarm() {
        this.id = UUID.randomUUID().toString();
        this.hourOfDay = 7;
        this.minuteOfHour = 30;
        this.repeated = true;
        this.enabled = true;
        this.editable = true;
        this.smart = true;
        this.daysOfWeek = new HashSet<>();
        this.source = AlarmSource.MOBILE_APP;
    }


    public String getId() {
        return id;
    }

    public LocalTime getTime() {
        return new LocalTime(hourOfDay, minuteOfHour);
    }

    public
    @NonNull
    DateTime toTimeToday() {
        try {
            return new DateTime(DateTimeZone.getDefault())
                    .withHourOfDay(hourOfDay)
                    .withMinuteOfHour(minuteOfHour)
                    .withSecondOfMinute(0)
                    .withMillisOfSecond(0);
        } catch (IllegalFieldValueException e) {
            hourOfDay++; // For day light savings time
            return toTimeToday();
        } catch (IllegalInstantException e) {
            hourOfDay++; // For day light savings time
            return toTimeToday();
        }
    }

    public void setTime(@NonNull LocalTime time) {
        this.hourOfDay = time.getHourOfDay();
        this.minuteOfHour = time.getMinuteOfHour();
    }

    public DateTime getExpectedRingTime() {
        return new DateTime(year, month, dayOfMonth, hourOfDay, minuteOfHour, DateTimeZone.UTC);
    }

    public void setRingOnce() {
        final DateTime today = DateTime.now(DateTimeZone.getDefault());
        if (getTime().isBefore(today.toLocalTime())) {
            final DateTime tomorrow = today.plusDays(1);
            this.year = tomorrow.getYear();
            this.month = tomorrow.getMonthOfYear();
            this.dayOfMonth = tomorrow.getDayOfMonth();
        } else {
            this.year = today.getYear();
            this.month = today.getMonthOfYear();
            this.dayOfMonth = today.getDayOfMonth();
        }

        setRepeated(false);
        daysOfWeek.clear();
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

    @NonNull
    public List<ExpansionAlarm> getExpansions() {
        if (expansions == null || !BuildConfig.DEBUG) { //todo add support for prod
            expansions = new ArrayList<>();
        }
        return expansions;
    }

    @Nullable
    public ExpansionAlarm getExpansionAlarm(final Category category) {
        for (final ExpansionAlarm expansionAlarm : getExpansions()) {
            if (expansionAlarm.getCategory() == category) {
                return expansionAlarm;
            }
        }

        return null;
    }

    public void setExpansions(@NonNull final List<ExpansionAlarm> expansions) {
        this.expansions = expansions;
    }

    public AlarmSource getSource(){
        return source;
    }

    public void setSource(@NonNull final AlarmSource source) {
        this.source = source;
    }

    /**
     * @see org.joda.time.DateTimeConstants
     */
    public Set<Integer> getDaysOfWeek() {
        return daysOfWeek;
    }

    public void addDayOfWeek(@JodaWeekDay int dayOfWeek) {
        daysOfWeek.add(dayOfWeek);
        setRepeated(true);
    }

    public void setDaysOfWeek(@NonNull Collection<Integer> newDays) {
        daysOfWeek.clear();
        daysOfWeek.addAll(newDays);
        setRepeated(!daysOfWeek.isEmpty());
    }

    public void removeDayOfWeek(@JodaWeekDay int dayOfWeek) {
        daysOfWeek.remove(dayOfWeek);
        setRepeated(!daysOfWeek.isEmpty());
    }

    /**
     * @see org.joda.time.DateTimeConstants
     */
    public List<Integer> getSortedDaysOfWeek() {
        return Lists.sorted(daysOfWeek, (leftDay, rightDay) -> {
            // Joda-Time considers Sunday to be day 7, which is generally
            // not how it works in English speaking countries.
            final int leftCorrected = (leftDay == DateTimeConstants.SUNDAY) ? 0 : leftDay;
            final int rightCorrected = (rightDay == DateTimeConstants.SUNDAY) ? 0 : rightDay;
            return Functions.compareInts(leftCorrected, rightCorrected);
        });
    }

    //todo should be moved out to a ResMapper class
    public static @NonNull String nameForDayOfWeek(@NonNull Context context,
                                                   @JodaWeekDay int dayOfWeek) {
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

    //todo should be moved out to a ResMapper class
    public @NonNull String getDaysOfWeekSummary(@NonNull final Context context) {
        if (Lists.isEmpty(daysOfWeek)) {
            if(AlarmSource.VOICE_SERVICE.equals(source)){
                return context.getString(R.string.voice_alarm_never);
            } else if (isSmart()) {
                return context.getString(R.string.smart_alarm_never);
            } else {
                return context.getString(R.string.alarm_never);
            }
        }

        final String daysString = getRepeatSummary(context, true);
        if(AlarmSource.VOICE_SERVICE.equals(source)){
            return context.getString(R.string.voice_alarm_days_repeat_format, daysString);
        } else if (isSmart()) {
            return context.getString(R.string.smart_alarm_days_repeat_format, daysString);
        } else {
            return context.getString(R.string.alarm_days_repeat_format, daysString);
        }
    }

    //todo should be moved out to a ResMapper class
    public @NonNull String getRepeatSummary(@NonNull final Context context, final boolean longForm) {
        final int daysCount = daysOfWeek.size();
        if (daysCount == 0) {
            return context.getString(R.string.alarm_repeat_never);
        } else if (DateFormatter.isWeekend(daysOfWeek)) {
            return context.getString(R.string.alarm_repeat_weekends);
        } else if (DateFormatter.isWeekdays(daysOfWeek)) {
                return context.getString(R.string.alarm_repeat_weekdays);
            } else if (daysCount < 7) {
                if (longForm) {
                    final List<Integer> orderedDays = getSortedDaysOfWeek();
                    final List<String> days = Lists.map(orderedDays, day -> nameForDayOfWeek(context, day));
                    return TextUtils.join(context.getString(R.string.alarm_day_separator), days);
                } else {
                    return context.getResources().getQuantityString(R.plurals.alarm_repeat_days_count,
                                                                    daysCount, daysCount);
                }
            } else {
                return context.getString(R.string.alarm_repeat_everyday);
            }
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public int getMinuteOfHour() {
        return minuteOfHour;
    }

    public Sound getSound() {
        return sound;
    }

    public void setSound(Sound sound) {
        this.sound = sound;
    }

    public void setAlarmTones(final @NonNull ArrayList<Alarm.Sound> sounds) {
        this.alarmTones = new AlarmTones(sounds);
    }
    public AlarmTones getAlarmTones(){
        return this.alarmTones;
    }

    public Alarm.Sound getAlarmSoundWithId(final int id){
        return alarmTones != null ? alarmTones.getSoundWithId(id) : null;
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
                ", expansions=" + expansions +
                ", source=" + source +
                '}';
    }

    public static class AlarmTones implements IListObject {
        private final ArrayList<Sound> sounds;

        public AlarmTones(final @NonNull ArrayList<Alarm.Sound> sounds) {
            this.sounds = sounds;
        }

        public Alarm.Sound getSoundWithId(final int id) {
            for (final Alarm.Sound sound : sounds) {
                if (sound.getId() == id) {
                    return sound;
                }
            }
            return null;
        }

        @Override
        public List<? extends IListItem> getListItems() {
            return sounds;
        }
    }

    public static class Sound extends ApiResponse implements IListObject.IListItem {
        @SerializedName("id")
        public final long id;

        @SerializedName("name")
        public final String name;

        @SerializedName("url")
        public final String url;


        public Sound(long id, String name, String url) {
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

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getId() {
            return (int) id;
        }

        @Override
        public String getPreviewUrl() {
            return url;
        }
    }
}
