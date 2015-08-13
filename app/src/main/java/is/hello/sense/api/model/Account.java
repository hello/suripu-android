package is.hello.sense.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

public class Account extends ApiResponse implements Cloneable {
    @Expose(deserialize = false, serialize = true)
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("tz")
    private int timeZoneOffset;

    @SerializedName("name")
    private String name;

    @SerializedName("gender")
    private Gender gender;

    @SerializedName("height")
    private Integer height;

    @SerializedName("weight")
    private Integer weight;

    @SerializedName("dob")
    private LocalDate birthDate;

    @SerializedName("password")
    private String password;

    @SerializedName("last_modified")
    private DateTime lastModified;

    @SerializedName("email_verified")
    private boolean emailVerified;

    @SerializedName("lat")
    private Double latitude;

    @SerializedName("long")
    private Double longitude;


    public static Account createDefault() {
        Account newAccount = new Account();
        newAccount.setHeight(177);
        newAccount.setWeight(68039);
        newAccount.setTimeZoneOffset(DateTimeZone.getDefault()
                                                 .getOffset(DateTimeUtils.currentTimeMillis()));
        return newAccount;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTimeZoneOffset() {
        return timeZoneOffset;
    }

    public void setTimeZoneOffset(int timeZoneOffset) {
        this.timeZoneOffset = timeZoneOffset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(DateTime lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }




    public final Account clone() {
        try {
            return (Account) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", timeZoneOffset=" + timeZoneOffset +
                ", name='" + name + '\'' +
                ", gender=" + gender +
                ", height=" + height +
                ", weight=" + weight +
                ", birthDate=" + birthDate +
                ", password='" + password + '\'' +
                ", lastModified=" + lastModified +
                ", emailVerified=" + emailVerified +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public static class Preferences {
        @SerializedName("PUSH_ALERT_CONDITIONS")
        public boolean pushAlertConditions = true;

        @SerializedName("PUSH_SCORE")
        public boolean pushScore = true;

        @SerializedName("TIME_TWENTY_FOUR_HOUR")
        public boolean use24Time = false;

        @SerializedName("TEMP_CELSIUS")
        public boolean useCelsius = false;

        @SerializedName("WEIGHT_METRIC")
        public boolean useMetricWeight = false;

        @SerializedName("HEIGHT_METRIC")
        public boolean useMetricHeight = false;

        @SerializedName("ENHANCED_AUDIO")
        public boolean enhancedAudioEnabled = false;


        @Override
        public String toString() {
            return "Account.Preferences{" +
                    "pushAlertConditions=" + pushAlertConditions +
                    ", pushScore=" + pushScore +
                    ", use24Time=" + use24Time +
                    ", useCelsius=" + useCelsius +
                    ", useMetricWeight=" + useMetricWeight +
                    ", useMetricHeight=" + useMetricHeight +
                    ", enhancedAudioEnabled=" + enhancedAudioEnabled +
                    '}';
        }
    }
}
