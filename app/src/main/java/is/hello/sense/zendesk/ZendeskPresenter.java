package is.hello.sense.zendesk;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.zendesk.sdk.feedback.ZendeskFeedbackConfiguration;
import com.zendesk.sdk.feedback.impl.ZendeskFeedbackConnector;
import com.zendesk.sdk.model.CreateRequest;
import com.zendesk.sdk.model.CustomField;
import com.zendesk.sdk.model.network.AnonymousIdentity;
import com.zendesk.sdk.model.network.Identity;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.util.Analytics;
import rx.Observable;
import rx.Subscriber;

public class ZendeskPresenter extends Presenter {
    private static final String APP_ID = "mobile_sdk_client_510ec7f059736bb60c17";
    private static final String CLIENT_ID = "7bb3a86905b08e1083752af7b1d4a430afb1a051b6dae18a";
    private static final String HOST = "https://helloinc.zendesk.com";

    private static final long CUSTOM_FIELD_ID_TOPIC = 24321669L;

    private final Context context;
    private final ApiService apiService;
    private final AtomicReference<Account> cachedAccount = new AtomicReference<>();

    //region Lifecycle

    @Inject public ZendeskPresenter(@NonNull Context context,
                                    @NonNull ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        cachedAccount.set(null);

        return true;
    }

    //endregion


    private Observable<Account> getSenseAccount() {
        logEvent("getSenseAccount()");

        Account cached = cachedAccount.get();
        if (cached != null) {
            return Observable.just(cached);
        } else {
            return apiService.getAccount().doOnNext(cachedAccount::set);
        }
    }

    private Observable<String> initializeSdkIfNeeded() {
        logEvent("initialize sdk");

        ZendeskConfig config = ZendeskConfig.INSTANCE;
        if (config.isInitialized()) {
            return Observable.just("already initialized");
        }

        return Observable.create(subscriber -> {
            config.init(context, HOST, CLIENT_ID, APP_ID, new CallbackAdapter<>(subscriber));
        });
    }

    public Observable<Pair<ZendeskConfig, Account>> initializeIfNeeded() {
        logEvent("initialize identity");

        ZendeskConfig config = ZendeskConfig.INSTANCE;
        Account cachedAccount = this.cachedAccount.get();
        if (config.isInitialized() && cachedAccount != null) {
            return Observable.just(Pair.create(config, cachedAccount));
        }

        Observable<Account> dependencies = Observable.combineLatest(
                initializeSdkIfNeeded(),
                getSenseAccount(),
                (ignored, account) -> account
        );
        return dependencies.map(account -> {
            Identity identity = new AnonymousIdentity.Builder()
                    .withExternalIdentifier(account.getId())
                    .withNameIdentifier(account.getName())
                    .withEmailIdentifier(account.getEmail())
                    .build();
            config.setIdentity(identity);

            return Pair.create(config, account);
        });
    }

    public Observable<ZendeskFeedbackConfiguration> prepareForFeedback(@NonNull SupportTopic supportTopic) {
        logEvent("prepareForFeedback(" + supportTopic + ")");

        return initializeIfNeeded().map(configAndAccount -> {
            logEvent("prepareForFeedback: done");

            ZendeskConfig config = configAndAccount.first;
            Account account = configAndAccount.second;

            CustomField topicId = new CustomField(CUSTOM_FIELD_ID_TOPIC, supportTopic.topic);
            config.setCustomFields(Lists.newArrayList(topicId));

            return new ZendeskFeedbackConfiguration() {
                @Override
                public List<String> getTags() {
                    return Lists.newArrayList(Build.MODEL, Build.VERSION.RELEASE);
                }

                @Override
                public String getAdditionalInfo() {
                    return String.format(Locale.US, "Id: %s\nSense Id: %s", account.getId(), Analytics.getSenseId());
                }

                @Override
                public String getRequestSubject() {
                    return "Android Ticket for Sense " + BuildConfig.VERSION_NAME;
                }
            };
        });
    }

    public Observable<CreateRequest> sendFeedback(@NonNull ZendeskFeedbackConfiguration config,
                                                  @NonNull String feedback,
                                                  @NonNull List<String> attachmentIds) {
        logEvent("sendFeedback()");

        return Observable.create(subscriber -> {
            if (!ZendeskConfig.INSTANCE.isInitialized()) {
                subscriber.onError(new IllegalStateException("sendFeedback() cannot be used before initializing the Zendesk SDK"));
                return;
            }

            ZendeskFeedbackConnector connector = new ZendeskFeedbackConnector(context, config);
            connector.sendFeedback(feedback, attachmentIds, new CallbackAdapter<>(subscriber));
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
