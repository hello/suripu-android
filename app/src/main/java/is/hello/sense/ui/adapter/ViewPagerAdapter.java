package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ViewPagerAdapter<T> extends PagerAdapter implements View.OnClickListener {
    private final LayoutInflater inflater;
    private final @LayoutRes int layoutRes;
    private final List<T> data = new ArrayList<>();

    public ViewPagerAdapter(@NonNull Context context, @LayoutRes int layoutRes) {
        this.inflater = LayoutInflater.from(context);
        this.layoutRes = layoutRes;
    }

    //region Lifecycle

    protected abstract void configureView(@NonNull View view, int position);

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = inflater.inflate(layoutRes, container, false);
        configureView(view, position);
        view.setTag(position);
        view.setOnClickListener(this);
        container.addView(view, 0);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    //endregion


    //region Data

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public int getItemPosition(Object object) {
        //noinspection SuspiciousMethodCalls
        return data.indexOf(object);
    }

    public T getItem(int position) {
        return data.get(position);
    }

    public boolean addAll(Collection<? extends T> ts) {
        boolean added = data.addAll(ts);
        notifyDataSetChanged();
        return added;
    }

    public void clear() {
        data.clear();
        notifyDataSetChanged();
    }

    //endregion


    //region Click Support

    public @Nullable OnItemViewClickedListener onItemViewClickedListener;

    @Override
    public void onClick(@NonNull View view) {
        int position = (Integer) view.getTag();
        if (onItemViewClickedListener != null) {
            onItemViewClickedListener.onItemViewClicked(view, position);
        }
    }

    public interface OnItemViewClickedListener {
        void onItemViewClicked(@NonNull View view, int position);
    }

    //endregion
}
