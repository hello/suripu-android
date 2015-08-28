package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

public abstract class ViewPagerAdapter<THolder extends ViewPagerAdapter.ViewHolder> extends PagerAdapter {
    public abstract THolder createViewHolder(ViewGroup container, int position);
    public abstract void bindViewHolder(THolder holder, int position);
    public void unbindViewHolder(THolder holder) {

    }

    @Override
    public final boolean isViewFromObject(View view, Object object) {
        ViewHolder holder = (ViewHolder) object;
        return (holder.itemView == view);
    }

    @Override
    public final Object instantiateItem(ViewGroup container, int position) {
        THolder holder = createViewHolder(container, position);
        container.addView(holder.itemView);
        bindViewHolder(holder, position);
        return holder;
    }

    @Override
    public final void destroyItem(ViewGroup container, int position, Object object) {
        @SuppressWarnings("unchecked")
        THolder holder = (THolder) object;
        unbindViewHolder(holder);
        container.removeView(holder.itemView);
    }


    public static class ViewHolder {
        public final View itemView;

        public ViewHolder(@NonNull View itemView) {
            this.itemView = itemView;
        }
    }
}
