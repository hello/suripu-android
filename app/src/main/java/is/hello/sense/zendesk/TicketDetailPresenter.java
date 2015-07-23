package is.hello.sense.zendesk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.zendesk.sdk.model.network.CommentsResponse;
import com.zendesk.sdk.network.RequestProvider;
import com.zendesk.sdk.network.impl.ZendeskRequestProvider;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.SafeObserverWrapper;
import is.hello.sense.graph.presenters.Presenter;
import rx.Observable;
import rx.Subscription;

public class TicketDetailPresenter extends Presenter {
    // CommentsResponse doesn't implement Serializable,
    // so we can't use ValuePresenter<T>. Yay.
    public final PresenterSubject<CommentsResponse> comments = PresenterSubject.create();

    @Inject Context context;
    @Inject ApiService apiService;

    private final RequestProvider requestProvider = new ZendeskRequestProvider();
    private @Nullable Subscription updateSubscription;
    private String ticketId;

    //region Lifecycle

    @Inject public TicketDetailPresenter() {

    }

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
        if (updateSubscription != null) {
            updateSubscription.unsubscribe();
            this.updateSubscription = null;
        }

        if (ticketId != null) {
            Observable<CommentsResponse> updateObservable = ZendeskHelper.doAction(context, apiService.getAccount(),
                    callback -> requestProvider.getComments(ticketId, callback));
            this.updateSubscription = updateObservable.subscribe(new SafeObserverWrapper<>(comments));
        }
    }

    public void setTicketId(@NonNull String ticketId) {
        this.ticketId = ticketId;
        update();
    }

    //endregion
}
