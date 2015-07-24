package is.hello.sense.zendesk;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zendesk.sdk.feedback.ZendeskFeedbackConfiguration;
import com.zendesk.sdk.feedback.impl.ZendeskFeedbackConnector;
import com.zendesk.sdk.model.CreateRequest;
import com.zendesk.sdk.model.CustomField;
import com.zendesk.sdk.model.Request;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.sdk.network.impl.ZendeskRequestProvider;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import is.hello.sense.BuildConfig;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SupportTopic;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.api.sessions.OAuthSession;
import is.hello.sense.functional.Lists;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import is.hello.sense.graph.presenters.Presenter;
import is.hello.sense.util.Analytics;
import rx.Observable;
import rx.Subscription;

public class TicketsPresenter extends Presenter {
    private static final long CUSTOM_FIELD_ID_TOPIC = 24321669L;

    @Inject Context context;
    @Inject ApiService apiService;
    @Inject ApiSessionManager sessionManager;

    // Request doesn't implement Serializable,
    // so we can't use ValuePresenter<T>. Yay.
    public final PresenterSubject<List<Request>> tickets = PresenterSubject.create();
    private @Nullable Subscription updateSubscription;

    //region Lifecycle

    @Override
    protected boolean onForgetDataForLowMemory() {
        tickets.forget();

        return true;
    }

    @Override
    protected void onReloadForgottenData() {
        update();
    }
    //endregion


    //region Updating

    public void update() {
        if (updateSubscription != null) {
            updateSubscription.unsubscribe();
            this.updateSubscription = null;
        }

        Observable<List<Request>> updateObservable = ZendeskHelper.doAction(context, apiService.getAccount(), callback -> {
            ZendeskRequestProvider provider = new ZendeskRequestProvider();
            provider.getRequests("new,open,pending,hold,solved", callback);
        });
        this.updateSubscription = updateObservable.subscribe(new SafeObserverWrapper<>(tickets));
    }

    //endregion


    public Observable<ZendeskConfig> initializeIfNeeded() {
        return ZendeskHelper.initializeIfNeeded(context, apiService.getAccount());
    }

    public Observable<CreateRequest> createTicket(@NonNull SupportTopic onTopic,
                                                  @NonNull String text,
                                                  @NonNull List<String> attachmentTokens) {
        return ZendeskHelper.doAction(context, apiService.getAccount(), callback -> {
            CustomField topicId = new CustomField(CUSTOM_FIELD_ID_TOPIC, onTopic.topic);
            ZendeskConfig.INSTANCE.setCustomFields(Lists.newArrayList(topicId));

            ZendeskFeedbackConfiguration configuration = new ZendeskFeedbackConfiguration() {
                @Override
                public List<String> getTags() {
                    return Lists.newArrayList(Build.MODEL, Build.VERSION.RELEASE);
                }

                @Override
                public String getAdditionalInfo() {
                    OAuthSession session = sessionManager.getSession();
                    String accountId = session != null ? session.getAccountId() : "";
                    return String.format(Locale.US, "Id: %s\nSense Id: %s", accountId, Analytics.getSenseId());
                }

                @Override
                public String getRequestSubject() {
                    return "Android Ticket for Sense " + BuildConfig.VERSION_NAME;
                }
            };

            ZendeskFeedbackConnector connector = new ZendeskFeedbackConnector(context, configuration);
            connector.sendFeedback(text, attachmentTokens, callback);
        });
    }
}
