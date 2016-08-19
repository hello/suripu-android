package is.hello.sense.interactors;

import android.support.annotation.NonNull;

import java.io.InvalidObjectException;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.v2.InsightInfo;
import is.hello.sense.graph.InteractorSubject;
import rx.Observable;

public class InsightInfoInteractor extends ValueInteractor<InsightInfo> {
    private final ApiService apiService;
    private String category;

    public final InteractorSubject<InsightInfo> insightInfo = this.subject;

    @Inject public InsightInfoInteractor(@NonNull ApiService apiService) {
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
