package is.hello.sense.api.fb.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class FacebookProfile extends ApiResponse{

    @SerializedName("picture")
    private FacebookProfilePicture picture;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String lastName;

    @SerializedName("email")
    private String email;

    @SerializedName("gender")
    private String gender;

    public FacebookProfile(@NonNull final FacebookProfilePicture picture,
                           @NonNull final String firstName,
                           @NonNull final String lastName,
                           @NonNull final String email,
                           @NonNull final String gender){
        this.picture = picture;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.gender = gender;
    }

    public FacebookProfilePicture getPicture() {
        return picture;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public String getPictureUrl(){
        return picture.getImageUrl();
    }

}
