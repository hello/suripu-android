package is.hello.sense.api.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import is.hello.sense.R;
import is.hello.sense.util.Errors;
import retrofit.RetrofitError;

public class ApiException extends Exception implements Errors.Reporting {
    private final ErrorResponse errorResponse;
    private final RetrofitError networkStackError;

    public static boolean isNetworkError(@Nullable Throwable e) {
        return (e != null &&
                e instanceof ApiException &&
                ((ApiException) e).isNetworkError());
    }

    public static boolean statusEquals(@Nullable Throwable e, int status) {
        if (e == null) {
            return false;
        }

        if (!(e instanceof ApiException)) {
            return false;
        }

        Integer errorStatus = ((ApiException) e).getStatus();
        return (errorStatus != null && errorStatus == status);
    }

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


    public @Nullable Integer getStatus() {
        if (networkStackError.getResponse() != null)
            return networkStackError.getResponse().getStatus();
        else
            return null;
    }

    public boolean isNetworkError() {
        return (networkStackError.getKind() == RetrofitError.Kind.NETWORK);
    }


    @Override
    public String getMessage() {
        if (errorResponse != null && !TextUtils.isEmpty(errorResponse.getMessage())) {
            return errorResponse.getMessage();
        } else {
            return networkStackError.getMessage();
        }
    }

    @Nullable
    @Override
    public String getContextInfo() {
        String url = networkStackError.getUrl();
        Integer status = getStatus();
        if (status != null) {
            return status + ": " + url;
        } else {
            return url;
        }
    }

    @NonNull
    @Override
    public Errors.Message getDisplayMessage() {
        if (isNetworkError()) {
            return Errors.Message.from(R.string.error_network_unavailable);
        } else if (getErrorResponse() != null) {
            RegistrationError registrationError = RegistrationError.fromString(getErrorResponse().getMessage());
            if (registrationError != RegistrationError.UNKNOWN) {
                return Errors.Message.from(registrationError.messageRes);
            }
        }

        return Errors.Message.from(getMessage());
    }
}
