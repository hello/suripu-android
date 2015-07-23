package is.hello.sense.graph.presenters;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.BuildConfig;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Account;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.functional.Lists;
import rx.Observable;

public class ZendeskPresenter extends Presenter {
    public static final String APP_ID = "mobile_sdk_client_510ec7f059736bb60c17";
    public static final String CLIENT_ID = "7bb3a86905b08e1083752af7b1d4a430afb1a051b6dae18a";
    public static final String HOST = "https://helloinc.zendesk.com";

    public static final long CUSTOM_FIELD_ID_TOPIC = 24321669L;

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
        Account cached = cachedAccount.get();
        if (cached != null) {
            return Observable.just(cached);
        } else {
            return apiService.getAccount().doOnNext(cachedAccount::set);
        }
    }

    private Observable<ZendeskConfig> initializeSdkIfNeeded() {
        ZendeskConfig config = ZendeskConfig.INSTANCE;
        if (config.isInitialized()) {
            return Observable.just(config);
        }

        logEvent("initialize sdk");

        return Observable.create(subscriber -> {
            ZendeskCallback<String> callback = new ZendeskCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    logEvent("sdk initialized");

                    subscriber.onNext(config);
                    subscriber.onCompleted();
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    ZendeskException e = new ZendeskException(errorResponse);
                    subscriber.onError(e);
                }
            };
            config.init(context, HOST, CLIENT_ID, APP_ID, callback);
        });
    }

    public Observable<Pair<ZendeskConfig, Account>> initializeIfNeeded() {
        ZendeskConfig existingConfig = ZendeskConfig.INSTANCE;
        Account cachedAccount = this.cachedAccount.get();
        if (existingConfig.isInitialized() && cachedAccount != null) {
            return Observable.just(Pair.create(existingConfig, cachedAccount));
        }

        logEvent("initialize identity");

        Observable<Pair<ZendeskConfig, Account>> dependencies = Observable.combineLatest(
                initializeSdkIfNeeded(),
                getSenseAccount(),
                Pair::new
        );
        return dependencies.map(configAndAccount -> {
            ZendeskConfig config = configAndAccount.first;
            Account account = configAndAccount.second;

            Identity identity = new AnonymousIdentity.Builder()
                    .withExternalIdentifier(account.getId())
                    .withNameIdentifier(account.getName())
                    .withEmailIdentifier(account.getEmail())
                    .build();
            config.setIdentity(identity);

            return configAndAccount;
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
                    String additionalInfo = "\n\n\n\n-----\n";
                    additionalInfo += "Id: " + account.getId();
                    additionalInfo += "\nSense Id: " + "";
                    return additionalInfo;
                }

                @Override
                public String getRequestSubject() {
                    return "Android Ticket for Sense " + BuildConfig.VERSION_NAME;
                }
            };
        });
    }

    public Observable<CreateRequest> submitFeedback(@NonNull ZendeskFeedbackConfiguration config,
                                                    @NonNull String feedback,
                                                    @NonNull List<String> attachmentIds) {
        return Observable.create(subscriber -> {
            ZendeskFeedbackConnector connector = new ZendeskFeedbackConnector(context, config);
            connector.sendFeedback(feedback, attachmentIds, new ZendeskCallback<CreateRequest>() {
                @Override
                public void onSuccess(CreateRequest createRequest) {
                    subscriber.onNext(createRequest);
                    subscriber.onCompleted();
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    ZendeskException e = new ZendeskException(errorResponse);
                    subscriber.onError(e);
                }
            });
        });
    }


    public static class ZendeskException extends Exception implements Errors.Reporting {
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
}
