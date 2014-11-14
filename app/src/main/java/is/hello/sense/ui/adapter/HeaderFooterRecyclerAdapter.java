package is.hello.sense.ui.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class HeaderFooterRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int HEADER_TYPE_START = -1;
    private final int FOOTER_TYPE_START = Integer.MIN_VALUE;

    private final RecyclerView.Adapter wrappedAdapter;

    private final List<View> headerViews = new ArrayList<>();
    private final List<View> footerViews = new ArrayList<>();

    public HeaderFooterRecyclerAdapter(RecyclerView.Adapter wrappedAdapter) {
        this.wrappedAdapter = wrappedAdapter;
        wrappedAdapter.registerAdapterDataObserver(new DataSetChangeForwarder());
    }


    //region Properties

    public void addHeaderView(@Nullable View headerView) {
        headerViews.add(headerView);
        notifyItemInserted(headerViews.size() - 1);
    }

    public void addFooterView(@Nullable View footerView) {
        footerViews.add(footerView);
        notifyItemInserted(getItemCount() - 1);
    }

    //endregion


    //region Overrides

    public int getPositionInWrapper(int position) {
        return Math.max(0, position - headerViews.size());
    }


    /**
     * Returns whether or not a given viewType encodes a header or footer view index.
     * <p/>
     * All negative view types are reserved for the header-footer recycler adapter.
     */
    private boolean isViewTypeHeaderOrFooterIndex(int viewType) {
        return viewType < 0;
    }

    /**
     * Returns whether or not a given viewType integer encodes a header view index or a footer view index.
     *
     * @see #getHeaderIndexFromViewType(int)
     * @see #getFooterIndexFromViewType(int)
     */
    private boolean isViewTypeHeader(int viewType) {
        return viewType >= -headerViews.size();
    }

    /**
     * Converts a given viewType integer into an index for the headerViews list.
     * <p/>
     * Headers are encoded as negative numbers decrementing from -1.
     * This means -1 is index 0 in headerViews, 0 is index 1, and so forth.
     * This leaves all positive view types available for the wrapped adapter.
     */
    private int getHeaderIndexFromViewType(int viewType) {
        return -(viewType - HEADER_TYPE_START);
    }

    /**
     * Converts a given viewType integer into an index for the footerViews list.
     * <p/>
     * Footers are encoded as negative numbers incrementing from Integer.MIN_VALUE.
     * This means that Integer.MIN_VALUE is index 0 footerViews, Integer.MIN_VALUE + 1
     * is index 1, and so forth. This leaves all positive view types available for
     * the wrapped adapter.
     */
    private int getFooterIndexFromViewType(int viewType) {
        return viewType - FOOTER_TYPE_START;
    }

    @Override
    public int getItemViewType(int position) {
        if (!headerViews.isEmpty() && position < headerViews.size()) {
            return HEADER_TYPE_START - position;
        } else if (!footerViews.isEmpty() && position > wrappedAdapter.getItemCount()) {
            return FOOTER_TYPE_START + (position - wrappedAdapter.getItemCount() - headerViews.size());
        } else {
            int wrappedItemViewType = wrappedAdapter.getItemViewType(getPositionInWrapper(position));

            if (wrappedItemViewType < 0)
                throw new IllegalArgumentException("Wrapped adapters cannot use negative item view types");

            return wrappedItemViewType;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (isViewTypeHeaderOrFooterIndex(viewType)) {
            if (isViewTypeHeader(viewType)) {
                return new HeaderFooterViewHolder(headerViews.get(getHeaderIndexFromViewType(viewType)));
            } else {
                return new HeaderFooterViewHolder(footerViews.get(getFooterIndexFromViewType(viewType)));
            }
        } else {
            return wrappedAdapter.onCreateViewHolder(viewGroup, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HeaderFooterViewHolder) {
            return;
        }

        //noinspection unchecked
        wrappedAdapter.onBindViewHolder(viewHolder, getPositionInWrapper(position));
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (!(holder instanceof HeaderFooterViewHolder)) {
            //noinspection unchecked
            wrappedAdapter.onViewRecycled(holder);
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (!(holder instanceof HeaderFooterViewHolder)) {
            //noinspection unchecked
            wrappedAdapter.onViewAttachedToWindow(holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if (!(holder instanceof HeaderFooterViewHolder)) {
            //noinspection unchecked
            wrappedAdapter.onViewDetachedFromWindow(holder);
        }
    }

    @Override
    public int getItemCount() {
        int count = wrappedAdapter.getItemCount();

        count += headerViews.size();
        count += footerViews.size();

        return count;
    }


    private static class HeaderFooterViewHolder extends RecyclerView.ViewHolder {
        private HeaderFooterViewHolder(View itemView) {
            super(itemView);
        }
    }

    //endregion


    private class DataSetChangeForwarder extends RecyclerView.AdapterDataObserver {
        public void onChanged() {
            notifyDataSetChanged();
        }

        public void onItemRangeChanged(int positionStart, int itemCount) {
            positionStart += headerViews.size();
            notifyItemRangeChanged(positionStart, itemCount);
        }

        public void onItemRangeInserted(int positionStart, int itemCount) {
            positionStart += headerViews.size();
            notifyItemRangeInserted(positionStart, itemCount);
        }

        public void onItemRangeRemoved(int positionStart, int itemCount) {
            positionStart += headerViews.size();
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            fromPosition += headerViews.size();
            toPosition += headerViews.size();
            for (int i = 0; i < itemCount; i++) {
                notifyItemMoved(fromPosition, toPosition + i);
            }
        }
    }
}
