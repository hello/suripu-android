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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm extends ApiResponse {
    @JsonProperty("year")
    private int year;

    @JsonProperty("month")
    private int month;

    @JsonProperty("day_of_month")
    private int day;

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


    public Alarm() {
        this.hourOfDay = 7;
        this.minuteOfHour = 30;
        this.isRepeated = true;
        this.isEnabled = true;
        this.isEditable = true;
        this.isSmart = false;
        this.daysOfWeek = new HashSet<>();
        this.sound = Sound.none();
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

    public boolean isSmart(){
        return this.isSmart;
    }

    public void setSmart(final boolean isSmart){
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
        if (daysOfWeek == null || daysOfWeek.isEmpty())
            return context.getString(R.string.never);

        List<String> days = new ArrayList<>();
        for (Integer dayOfWeek : daysOfWeek) {
            days.add(nameForDayOfWeek(context, dayOfWeek));
        }

        return context.getString(R.string.days_repeat_prefix) + TextUtils.join(context.getString(R.string.day_separator), days);
    }

    public int getYear(){
        return this.year;
    }

    public int getMonth(){
        return this.month;
    }

    public int getDay(){
        return this.day;
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

    public static class Utils{

        /**
         * Make sure there is only one alarm per user per day
         * @param alarms
         * @return
         */
        public static boolean isValidAlarms(final List<Alarm> alarms){
            final Set<Integer> alarmDays = new HashSet<Integer>();
            for(final Alarm alarm: alarms){
                if(!alarm.isRepeated){
                    if (!isValidNoneRepeatedAlarm(alarm)) {
                        return false;
                    }

                    if(alarm.isSmart){
                        final DateTime expectedRingTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, DateTimeZone.UTC);
                        if(alarmDays.contains(expectedRingTime.getDayOfWeek())){
                            return false;
                        }

                        alarmDays.add(expectedRingTime.getDayOfWeek());
                    }
                }else{
                    if(!alarm.isSmart) {
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

        public static boolean isValidNoneRepeatedAlarm(final Alarm alarm){
            if(alarm.isRepeated){
                return true;
            }

            try{
                final DateTime validDateTime = new DateTime(alarm.year, alarm.month, alarm.day, alarm.hourOfDay, alarm.minuteOfHour, DateTimeZone.UTC);
            }catch (Exception ex){
                return false;
            }

            return true;
        }

    }

    public static class Sound extends ApiResponse {
        @JsonProperty("id")
        public final long id;

        @JsonProperty("name")
        public final String name;

        @JsonProperty("url")
        public final String url;


        public static @NonNull Sound none() {
            return new Sound(0, "None", "");
        }

        public static @NonNull List<Sound> testSounds() {
            List<Sound> sounds = new ArrayList<>();

            sounds.add(Sound.none());
            sounds.add(new Sound(1, "Bells", ""));
            sounds.add(new Sound(2, "Birdsong", ""));
            sounds.add(new Sound(3, "Chime", ""));
            sounds.add(new Sound(4, "Waterfall", ""));

            return sounds;
        }

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
