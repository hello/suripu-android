package is.hello.sense.graph.presenters;

import android.content.ComponentCallbacks2;

import java.util.List;

import javax.inject.Inject;

import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.Insight;
import is.hello.sense.graph.PresenterSubject;

public class InsightsPresenter extends Presenter {
    @Inject ApiService apiService;

    public final PresenterSubject<List<Insight>> insights = PresenterSubject.create();

    @Override
    protected void onReloadForgottenData() {
        update();
    }

    @Override
    protected boolean onForgetDataForLowMemory() {
        insights.forget();
        return true;
    }

    public void update() {
        apiService.currentInsights().subscribe(insights);
    }
}
