package is.hello.sense.graph.presenters;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.InvalidObjectException;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.InsightInfo;
import is.hello.sense.graph.PresenterSubject;
import rx.Observable;

public class InsightInfoPresenter extends ValuePresenter<InsightInfo> {
    @Inject ApiService apiService;

    public final PresenterSubject<InsightInfo> insightInfo = this.subject;

    private InsightCategory insightCategory;


    //region State Saving


    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        super.onRestoreState(savedState);

        String insightCategoryString = savedState.getString("insightCategory");
        if (insightCategoryString != null) {
            this.insightCategory = InsightCategory.fromString(insightCategoryString);
        }
    }

    @Nullable
    @Override
    public Bundle onSaveState() {
        Bundle savedState = super.onSaveState();

        if (savedState != null && insightCategory != null) {
            savedState.putString("insightCategory", insightCategory.toString());
        }

        return savedState;
    }


    //endregion

    //region Updating

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<InsightInfo> provideUpdateObservable() {
        return apiService.insightInfo(getInsightCategory()).flatMap(insights -> {
            if (insights.isEmpty()) {
                return Observable.error(new InvalidObjectException("No insights"));
            } else {
                return Observable.just(insights.get(0));
            }
        });
    }

    //endregion


    //region Properties

    public InsightCategory getInsightCategory() {
        return insightCategory;
    }

    public void setInsightCategory(InsightCategory insightCategory) {
        this.insightCategory = insightCategory;
        update();
    }

    //endregion
}
