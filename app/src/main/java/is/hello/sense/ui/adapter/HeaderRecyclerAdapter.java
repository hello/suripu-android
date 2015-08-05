package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

public class HeaderRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static int VIEW_ID_HEADER_FOOTER = Integer.MIN_VALUE;

    @VisibleForTesting final List<View> headers = new ArrayList<>();
    @VisibleForTesting final List<View> footers = new ArrayList<>();
    @VisibleForTesting final RecyclerView.Adapter adapter;

    public HeaderRecyclerAdapter(@NonNull RecyclerView.Adapter<?> adapter) {
        if (adapter instanceof HeaderRecyclerAdapter) {
            throw new IllegalArgumentException("Cannot nest HeaderRecyclerAdapter.");
        }

        this.adapter = adapter;
        adapter.registerAdapterDataObserver(new ForwardingObserver());
    }


    //region Headers & Footers

    public HeaderRecyclerAdapter addHeader(@NonNull View header) {
        int oldSize = headers.size();
        headers.add(header);
        notifyItemInserted(oldSize);
        return this;
    }

    public HeaderRecyclerAdapter addFooter(@NonNull View footer) {
        int oldSize = getItemCount();
        footers.add(footer);
        notifyItemInserted(oldSize);
        return this;
    }

    //endregion


    //region Population

    @Override
    public int getItemCount() {
        return headers.size() + footers.size() + adapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        int headersSize = headers.size();
        if (position < headersSize ||
                position >= (headersSize + adapter.getItemCount())) {
            return VIEW_ID_HEADER_FOOTER;
        } else {
            return adapter.getItemViewType(position - headersSize);
        }
    }

    @Override
    public long getItemId(int position) {
        int headersSize = headers.size();
        if (position < headersSize) {
            return position - headersSize;
        } else if (position >= (headersSize + adapter.getItemCount())) {
            int shiftedPosition = position - headersSize - adapter.getItemCount();
            return Integer.MAX_VALUE - shiftedPosition;
        } else {
            return adapter.getItemViewType(position - headersSize);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_ID_HEADER_FOOTER) {
            return new HeaderFooterViewHolder(new FrameLayout(parent.getContext()));
        } else {
            return adapter.createViewHolder(parent, viewType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int headersSize = headers.size();
        if (holder instanceof HeaderFooterViewHolder) {
            View view;
            if (position < headersSize) {
                view = headers.get(position);
            } else {
                int shiftedPosition = position - headersSize - adapter.getItemCount();
                view = footers.get(shiftedPosition);
            }

            FrameLayout root = ((HeaderFooterViewHolder) holder).root;
            if (view.getParent() != root) {
                root.removeAllViews();
                root.addView(view);
            }
        } else {
            adapter.bindViewHolder(holder, position - headersSize);
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        if (holder instanceof HeaderFooterViewHolder) {
            FrameLayout root = ((HeaderFooterViewHolder) holder).root;
            root.removeAllViews();
        }
    }

    //endregion


    static class HeaderFooterViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout root;

        HeaderFooterViewHolder(@NonNull FrameLayout itemView) {
            super(itemView);

            this.root = itemView;
        }
    }

    class ForwardingObserver extends RecyclerView.AdapterDataObserver {
        public void onChanged() {
            notifyDataSetChanged();
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(headers.size() + positionStart, itemCount);
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(headers.size() + positionStart, itemCount);
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(headers.size() + positionStart, itemCount);
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            int offset = headers.size();
            for (int i = 0; i < itemCount; i++) {
                notifyItemMoved(offset + fromPosition + i, offset + toPosition + i);
            }
        }
    }
}
