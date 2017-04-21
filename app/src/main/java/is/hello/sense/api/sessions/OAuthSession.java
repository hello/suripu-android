package is.hello.sense.api.sessions;

import android.content.Context;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

public class OAuthSession extends ApiResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("expires_in")
    private long expiresIn;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("account_id")
    private String accountId;


    public String getAccessToken() {
        return accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    /**
     * Only use when initially signing in or registering.
     * Do not rely on this account id to be in sync with account id stored on server.
     * Only because the session may persist longer over multiple releases.
     * See {@link is.hello.sense.util.InternalPrefManager#getAccountId(Context)}
     * @return potentially out of sync account id
     */
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return "OAuthSession{" +
                "accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
