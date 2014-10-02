package is.hello.sense.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse extends ApiResponse {
    @JsonProperty("code")
    private int code;

    @JsonProperty("message")
    private String message;


    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    @Override
    public String toString() {
        return "ErrorResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
