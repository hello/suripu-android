package is.hello.sense.api.model;

import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;

import is.hello.sense.api.ApiService;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account extends ApiResponse {
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
    private Long height;

    @JsonProperty("weight")
    private Long weight;

    @JsonProperty("dob")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = ApiService.DATE_FORMAT)
    private DateTime birthDate;

    @JsonProperty("password")
    private String password;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private DateTime lastModified;


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

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    public DateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(DateTime birthDate) {
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
                ", password=" + !TextUtils.isEmpty(password) +
                ", lastModified=" + lastModified +
                '}';
    }
}
