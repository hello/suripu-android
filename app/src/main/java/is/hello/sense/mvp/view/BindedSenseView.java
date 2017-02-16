package is.hello.sense.mvp.view;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;

public abstract class BindedSenseView<T extends android.databinding.ViewDataBinding> extends SenseView {

    protected final T binding;

    public BindedSenseView(@NonNull final Activity activity) {
        super(activity);
        this.binding = DataBindingUtil.bind(getChildAt(0));
        if (this.binding == null) {
            throw new NullPointerException("BindedPresenterView failed to bind for: " + getClass().getSimpleName());
        }
    }
}
