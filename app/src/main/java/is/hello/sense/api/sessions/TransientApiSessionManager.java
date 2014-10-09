package is.hello.sense.api.sessions;

import android.support.annotation.Nullable;

public class TransientApiSessionManager extends ApiSessionManager {
    private OAuthSession session;

    @Override
    protected void storeOAuthSession(@Nullable OAuthSession session) {
        this.session = session;
    }

    @Override
    protected @Nullable OAuthSession retrieveOAuthSession() {
        return this.session;
    }
}
