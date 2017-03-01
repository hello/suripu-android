package is.hello.sense.flows.generic.ui.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;

import is.hello.sense.R;
import is.hello.sense.databinding.BindableRecyclerviewBinding;
import is.hello.sense.mvp.view.BindedPresenterView;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.recycler.DividerItemDecoration;

@SuppressLint("ViewConstructor")
public class ListView extends BindedPresenterView<BindableRecyclerviewBinding> {

    public ListView(@NonNull final Activity activity) {
        super(activity);
        this.binding.bindableRecyclerview.setLayoutManager(new LinearLayoutManager(activity));
        this.binding.bindableRecyclerview.setHasFixedSize(true);
        this.binding.bindableRecyclerview.setItemAnimator(null);
        this.binding.bindableRecyclerview.addItemDecoration(new DividerItemDecoration(getContext()));
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.bindable_recyclerview;
    }

    @Override
    public void releaseViews() {
        this.binding.bindableRecyclerview.setAdapter(null);
    }

    public void setAdapter(@Nullable final ArrayRecyclerAdapter adapter) {
        this.binding.bindableRecyclerview.setAdapter(adapter);
    }

}
