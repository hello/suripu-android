package is.hello.sense.flows.expansions.ui.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.mvp.view.SenseView;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.recycler.DividerItemDecoration;

public class ExpansionListView extends SenseView {

    private final RecyclerView recyclerView;

    public ExpansionListView(@NonNull final Activity activity, @NonNull final ArrayRecyclerAdapter adapter) {
        super(activity);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_expansion_list_rv);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.recyclerView.addItemDecoration(new DividerItemDecoration(context));
        this.recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
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
