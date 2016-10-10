package is.hello.sense.flows.expansions.ui.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;

public class ExpansionListView extends PresenterView {

    private final RecyclerView recyclerView;

    public ExpansionListView(@NonNull final Activity activity, @NonNull final ArrayRecyclerAdapter adapter) {
        super(activity);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_expansion_list_rv);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL, false));
        this.recyclerView.setAdapter(adapter);
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
