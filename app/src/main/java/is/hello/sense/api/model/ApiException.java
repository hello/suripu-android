package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import retrofit.RetrofitError;

public class ApiException extends Exception {
    private final ErrorResponse errorResponse;
    private final RetrofitError networkStackError;

    public ApiException(@Nullable ErrorResponse errorResponse, @NonNull RetrofitError networkStackError) {
        super(networkStackError);

        this.errorResponse = errorResponse;
        this.networkStackError = networkStackError;
    }


    public @Nullable ErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public @NonNull RetrofitError getNetworkStackError() {
        return networkStackError;
    }


    public String getUrl() {
        return networkStackError.getUrl();
    }

    public @Nullable Integer getStatus() {
        if (networkStackError.getResponse() != null)
            return networkStackError.getResponse().getStatus();
        else
            return null;
    }

    public @Nullable String getReason() {
        if (networkStackError.getResponse() != null)
            return networkStackError.getResponse().getReason();
        else
            return null;
    }


    @Override
    public String getMessage() {
        if (errorResponse != null && !TextUtils.isEmpty(errorResponse.getMessage())) {
            return errorResponse.getMessage();
        } else {
            return networkStackError.getMessage();
        }
    }
}
