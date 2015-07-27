package is.hello.sense.zendesk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zendesk.service.ErrorResponse;

import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;

public class ZendeskException extends Exception implements Errors.Reporting {
    private final ErrorResponse response;

    public ZendeskException(@NonNull ErrorResponse response) {
        super(response.getReason());
        this.response = response;
    }

    @Nullable
    @Override
    public String getContextInfo() {
        return response.getStatus() + ": " + response.getUrl();
    }

    @NonNull
    @Override
    public StringRef getDisplayMessage() {
        return StringRef.from(response.getReason());
    }
}
