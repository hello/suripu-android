package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import is.hello.sense.api.ApiService;
import is.hello.sense.units.UnitOperations;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account extends ApiResponse implements Cloneable {
    @JsonIgnore
    private String id;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("tz")
    private int timeZoneOffset;

    @JsonProperty("name")
    private String name;

    @JsonProperty("gender")
    private Gender gender;

    @JsonProperty("height")
    private Integer height;

    @JsonProperty("weight")
    private Integer weight;

    @JsonProperty("dob")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private LocalDate birthDate;

    @JsonProperty("password")
    private String password;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private DateTime lastModified;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    @JsonProperty("lat")
    private Double latitude;

    @JsonProperty("long")
    private Double longitude;


    public static Account createDefault() {
        Account newAccount = new Account();
        newAccount.setHeight(UnitOperations.inchesToCentimeters(64));
        newAccount.setWeight(UnitOperations.poundsToGrams(150));
        newAccount.setTimeZoneOffset(DateTimeZone.getDefault().getOffset(DateTimeUtils.currentTimeMillis()));
        return newAccount;
    }


    @JsonIgnore
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
                ", accountId='" + accountId + '\'' +
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
}
