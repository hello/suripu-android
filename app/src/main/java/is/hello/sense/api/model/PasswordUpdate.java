package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PasswordUpdate extends ApiResponse {
    @JsonProperty("current_password")
    private String currentPassword;

    @JsonProperty("new_password")
    private String newPassword;

    public PasswordUpdate(@NonNull String currentPassword, @NonNull String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }


    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
