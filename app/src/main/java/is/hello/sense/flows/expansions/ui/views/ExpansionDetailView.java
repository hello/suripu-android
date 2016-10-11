package is.hello.sense.flows.expansions.ui.views;

import android.app.Activity;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;

public class ExpansionDetailView extends PresenterView {

    public ExpansionDetailView(@NonNull Activity activity) {
        super(activity);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail;
    }

    @Override
    public void releaseViews() {

    }
}
