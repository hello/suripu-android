package is.hello.sense.zendesk;

import android.content.Context;
import android.support.annotation.NonNull;

import com.zendesk.sdk.model.network.AnonymousIdentity;
import com.zendesk.sdk.model.network.Identity;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import is.hello.sense.api.model.Account;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

public class ZendeskHelper {
    private static final String APP_ID = "mobile_sdk_client_510ec7f059736bb60c17";
    private static final String CLIENT_ID = "7bb3a86905b08e1083752af7b1d4a430afb1a051b6dae18a";
    private static final String HOST = "https://helloinc.zendesk.com";

    public static Observable<ZendeskConfig> initializeIfNeeded(@NonNull Context context,
                                                               @NonNull Observable<Account> account) {
        ZendeskConfig config = ZendeskConfig.INSTANCE;
        if (config.isInitialized()) {
            return Observable.just(config);
        }

        return account.flatMap(a -> Observable.create(subscriber -> {
            config.init(context, HOST, CLIENT_ID, APP_ID, new ZendeskCallback<String>() {
                @Override
                public void onSuccess(String ignored) {
                    Identity identity = new AnonymousIdentity.Builder()
                            .withExternalIdentifier(a.getId())
                            .withNameIdentifier(a.getName())
                            .withEmailIdentifier(a.getEmail())
                            .build();
                    config.setIdentity(identity);

                    subscriber.onNext(config);
                    subscriber.onCompleted();
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    subscriber.onError(new ZendeskException(errorResponse));
                }
            });
        }));
    }

    public static <T> Observable<T> doAction(@NonNull Context context,
                                             @NonNull Observable<Account> account,
                                             @NonNull Action1<ZendeskCallback<T>> command) {
        return initializeIfNeeded(context, account).flatMap(ignored -> {
            return Observable.create(s -> command.call(new CallbackAdapter<>(s)));
        });
    }

    static class CallbackAdapter<T> extends ZendeskCallback<T> {
        private final Subscriber<? super T> subscriber;

        CallbackAdapter(@NonNull Subscriber<? super T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSuccess(T response) {
            subscriber.onNext(response);
            subscriber.onCompleted();
        }

        @Override
        public void onError(ErrorResponse errorResponse) {
            ZendeskException e = new ZendeskException(errorResponse);
            subscriber.onError(e);
        }
    }
}
