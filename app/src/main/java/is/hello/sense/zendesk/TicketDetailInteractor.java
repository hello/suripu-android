package is.hello.sense.zendesk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zendesk.sdk.model.Comment;
import com.zendesk.sdk.model.EndUserComment;
import com.zendesk.sdk.model.network.CommentsResponse;
import com.zendesk.sdk.network.RequestProvider;
import com.zendesk.sdk.network.impl.ZendeskRequestProvider;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.graph.InteractorSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import is.hello.sense.interactors.Interactor;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class TicketDetailInteractor extends Interactor {
    // CommentsResponse doesn't implement Serializable,
    // so we can't use ValueInteractor<T>. Yay.
    public final InteractorSubject<CommentsResponse> comments = InteractorSubject.create();

    @Inject
    Context context;
    @Inject
    ApiService apiService;

    private
    @Nullable
    RequestProvider requestProvider; // Cannot be initialized ahead of time.
    private
    @NonNull
    Subscription updateSubscription = Subscriptions.empty();
    private String ticketId;

    //region Lifecycle

    @Override
    protected boolean onForgetDataForLowMemory() {
        comments.forget();

        return true;
    }

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    //endregion


    //region Updating

    public void update() {
        this.updateSubscription.unsubscribe();
        this.updateSubscription = Subscriptions.empty();

        if (ticketId != null) {
            final Observable<CommentsResponse> updateObservable =
                    ZendeskHelper.doAction(context, apiService.getAccount(false), callback -> {
                        if (requestProvider == null) {
                            this.requestProvider = new ZendeskRequestProvider();
                        }
                        requestProvider.getComments(ticketId, callback);
                    });
            this.updateSubscription.unsubscribe();
            this.updateSubscription = updateObservable.subscribe(new SafeObserverWrapper<>(comments));
        }
    }

    public void setTicketId(@NonNull final String ticketId) {
        this.ticketId = ticketId;
        update();
    }

    //endregion


    //region Submitting

    public Observable<Comment> submitComment(@NonNull final String text, @NonNull final List<String> attachmentTokens) {
        logEvent("submitComment()");

        return ZendeskHelper.doAction(context, apiService.getAccount(false), callback -> {
            if (requestProvider == null) {
                this.requestProvider = new ZendeskRequestProvider();
            }

            final EndUserComment comment = new EndUserComment();
            comment.setValue(text);
            comment.setAttachments(attachmentTokens);
            requestProvider.addComment(ticketId, comment, callback);
        });
    }

    //endregion
}
