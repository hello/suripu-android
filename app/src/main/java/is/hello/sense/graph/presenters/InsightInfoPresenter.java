package is.hello.sense.graph.presenters;

import android.support.annotation.NonNull;

import java.io.InvalidObjectException;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class InsightInfoPresenter extends ValuePresenter<InsightInfo> {
    private final ApiService apiService;
    private String category;

    public final PresenterSubject<InsightInfo> insightInfo = this.subject;

    @Inject public InsightInfoPresenter(@NonNull ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return (category != null);
    }

    @Override
    protected Observable<InsightInfo> provideUpdateObservable() {
        return apiService.insightInfo(category).flatMap(insights -> {
            if (insights.isEmpty()) {
                return Observable.error(new InvalidObjectException("No insight info found."));
            } else {
                return Observable.just(insights.get(0));
            }
        });
    }

    public void setCategory(@NonNull String category) {
        this.category = category;
        update();
    }
}
