package is.hello.sense.api.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class PasswordUpdate extends ApiResponse {
    @SerializedName("current_password")
    private String currentPassword;

    @SerializedName("new_password")
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
