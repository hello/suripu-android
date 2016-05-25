package is.hello.sense.api.model;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;


import is.hello.sense.api.gson.Exclude;
import is.hello.sense.api.model.v2.MultiDensityImage;

public class Account extends ApiResponse implements Cloneable {
    @Expose(deserialize = false, serialize = true)
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("tz")
    private int timeZoneOffset;

    @SerializedName("firstname")
    private String firstName;

    @SerializedName("lastname")
    private String lastName;

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

    @SerializedName("created")
    @Exclude
    private LocalDate created;

    @SerializedName("last_modified")
    private DateTime lastModified;

    @SerializedName("email_verified")
    private boolean emailVerified;

    @SerializedName("lat")
    private Double latitude;

    @SerializedName("long")
    private Double longitude;

    @SerializedName("profile_photo")
    private MultiDensityImage profilePhoto;

    @SerializedName("time_zone")
    private final String timeZone = DateTimeZone.getDefault().getID();

    public static Account createDefault() {
        Account newAccount = new Account();
        newAccount.setFirstName("");
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NonNull String name) {
        this.firstName = name;
    }

    public String getLastName() {
        return lastName;
    }

    //Not guaranteed to be @NonNull
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName(){
        final String nonNullLastName = lastName != null ? lastName : "";
        return String.format("%s %s", firstName,nonNullLastName);

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

    public LocalDate getCreated() {
        return created;
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

    public MultiDensityImage getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(@NonNull MultiDensityImage profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getProfilePhotoUrl(@NonNull Resources resources) {
        return this.profilePhoto != null ? this.profilePhoto.getUrl(resources) : "";
    }

    public String getTimeZone() {
        return timeZone;
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
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", gender=" + gender +
                ", height=" + height +
                ", weight=" + weight +
                ", birthDate=" + birthDate +
                ", password='" + password + '\'' +
                ", created=" + created +
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
