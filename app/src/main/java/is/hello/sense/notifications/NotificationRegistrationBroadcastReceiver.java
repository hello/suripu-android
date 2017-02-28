package is.hello.sense.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;

import is.hello.sense.functional.Functions;
import rx.Observable;
import rx.schedulers.Schedulers;

public class NotificationRegistrationBroadcastReceiver extends BroadcastReceiver {

    static final String EXTRA_TOKEN = NotificationRegistrationBroadcastReceiver.class.getSimpleName()+ ".TOKEN";
    private static final String ACTION_REGISTER_TOKEN = NotificationRegistrationBroadcastReceiver.class.getSimpleName() + ".ACTION";
    private static final String ACTION_REMOVE_TOKEN = NotificationRegistrationBroadcastReceiver.class.getSimpleName() + ".ACTION_REMOVE_TOKEN";

    public static Intent getRegisterIntent(@Nullable final String token) {
        return new Intent(ACTION_REGISTER_TOKEN)
                .putExtra(EXTRA_TOKEN, token);
    }

    public static Intent getRemoveTokenIntent() {
        return new Intent(ACTION_REMOVE_TOKEN);
    }

    public static IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_REGISTER_TOKEN);
        intentFilter.addAction(ACTION_REMOVE_TOKEN);
        return intentFilter;
    }

    @Override
    public void onReceive(@NonNull final Context context,
                          @NonNull final Intent intent) {
        final String action = intent.getAction();
        if (ACTION_REGISTER_TOKEN.equals(action) && intent.hasExtra(EXTRA_TOKEN)) {
            trySendRegistrationToServer(context, intent);
        }
        else if (ACTION_REMOVE_TOKEN.equals(action)) {
            tryRemoveTokensAsync();
        }
    }

    /**
     * May not always successfully remove tokens.
     */
    void tryRemoveTokensAsync() {
        Observable.defer(() -> {
            try {
                tryRemoveTokensInternal();
                return Observable.empty();
            } catch (final IOException e) {
                return Observable.error(e);
            }
        }).subscribeOn(Schedulers.io())
                  .subscribe(Functions.NO_OP,
                             Functions.LOG_ERROR);
    }

    void trySendRegistrationToServer(@NonNull final Context context,
                                             @NonNull final Intent intent) {
        String token = intent.getStringExtra(EXTRA_TOKEN);
        if(token == null) {
            token = FirebaseInstanceId.getInstance().getToken();
        }

        if(token != null) {
            sendRegistrationToServer(token, context);
        } else  {
            Log.e(NotificationRegistrationBroadcastReceiver.class.getSimpleName(),
                  "no Firebase token available. Failed to register to servers.");
        }
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(@NonNull final String token,
                                          @NonNull final Context context) {
        final NotificationRegistration notificationRegistration = new NotificationRegistration(context);
        notificationRegistration.register(token);
    }

    /**
     * If successful, a new token won't be generated until after
     * {@link FirebaseInstanceId#getToken()} is called.
     * @throws IOException if called on main thread
     */
    private void tryRemoveTokensInternal() throws IOException {
        FirebaseInstanceId.getInstance()
                          .deleteInstanceId();
    }
}
