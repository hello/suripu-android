package is.hello.sense.api.sessions;

import android.support.annotation.Nullable;

public class TransientApiSessionManager extends ApiSessionManager {
    private OAuthSession session;

    @Override
    public void setOAuthSession(@Nullable OAuthSession session) {
        this.session = session;
    }

    @Nullable
    @Override
    public OAuthSession getOAuthSession() {
        return this.session;
    }
}
