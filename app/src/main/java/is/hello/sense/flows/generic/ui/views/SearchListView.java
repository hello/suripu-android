package is.hello.sense.flows.generic.ui.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.databinding.ViewSearchListBinding;
import is.hello.sense.flows.generic.ui.adapters.SimpleListAdapter;
import is.hello.sense.mvp.view.BindedPresenterView;
import is.hello.sense.ui.recycler.DividerItemDecoration;

@SuppressLint("ViewConstructor")
public class SearchListView extends BindedPresenterView<ViewSearchListBinding>
        implements TextWatcher {
    private final SimpleListAdapter adapter;
    private final RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView,
                                         final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING && listener != null) {
                listener.onScrolled();
            }
        }
    };

    private Listener listener = null;

    public SearchListView(@NonNull final Activity activity,
                          @NonNull final SimpleListAdapter adapter) {
        super(activity);
        this.adapter = adapter;
        this.binding.viewSearchListRecyclerview.setLayoutManager(new LinearLayoutManager(activity));
        this.binding.viewSearchListRecyclerview.setHasFixedSize(true);
        this.binding.viewSearchListRecyclerview.setItemAnimator(null);
        this.binding.viewSearchListRecyclerview.addItemDecoration(new DividerItemDecoration(getContext()));
        this.binding.viewSearchListRecyclerview.setAdapter(adapter);
        this.binding.viewSearchListSearch.addTextChangedListener(this);
        this.binding.viewSearchListClose.setOnClickListener(v -> SearchListView.this.binding.viewSearchListSearch.setText(null));
        this.binding.viewSearchListRecyclerview.addOnScrollListener(onScrollListener);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_search_list;
    }

    @Override
    public void releaseViews() {
        this.listener = null;
        this.adapter.setListener(null);
        this.binding.viewSearchListRecyclerview.removeOnScrollListener(onScrollListener);
        this.binding.viewSearchListClose.setOnClickListener(null);
        this.binding.viewSearchListSearch.removeTextChangedListener(this);
        this.binding.viewSearchListRecyclerview.setAdapter(null);
    }

    //region TextWatcher
    @Override
    public void beforeTextChanged(final CharSequence s,
                                  final int start,
                                  final int count,
                                  final int after) {

    }

    @Override
    public void onTextChanged(final CharSequence s,
                              final int start,
                              final int before,
                              final int count) {

    }

    @Override
    public void afterTextChanged(final Editable s) {
        final String text = s.toString();
        if (text.isEmpty()) {
            this.binding.viewSearchListClose.setVisibility(GONE);
        } else {
            this.binding.viewSearchListClose.setVisibility(VISIBLE);
        }
        this.adapter.setSearchParameters(text);
    }
    //endregion

    public void addAll(@NonNull final List<String> items) {
        this.adapter.addAll(items);
    }

    public void setInitialSelection(@Nullable final String initialSelection) {
        this.adapter.setInitialSelection(initialSelection);
    }

    public void setListener(@Nullable final Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onScrolled();
    }
}

