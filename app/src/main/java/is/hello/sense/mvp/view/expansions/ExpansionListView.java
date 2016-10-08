package is.hello.sense.mvp.view.expansions;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;

public class ExpansionListView extends PresenterView {

    private final RecyclerView recyclerView;

    public ExpansionListView(@NonNull final Activity activity) {
        super(activity);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_expansion_list_rv);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_list;
    }

    @Override
    public void releaseViews() {
        recyclerView.setAdapter(null);
    }
}
