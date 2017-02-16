package is.hello.sense.flows.notification.ui.views;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;

import is.hello.sense.R;
import is.hello.sense.databinding.BindableRecyclerviewBinding;
import is.hello.sense.flows.notification.ui.adapters.NotificationSettingsAdapter;
import is.hello.sense.mvp.view.BindedPresenterView;

@SuppressLint("ViewConstructor")
public class NotificationView extends BindedPresenterView<BindableRecyclerviewBinding> {
    //todo provide adapter
    public NotificationView(@NonNull final Activity activity,
                            @NonNull final NotificationSettingsAdapter adapter) {
        super(activity);
        this.binding.bindableRecyclerview.setLayoutManager(new LinearLayoutManager(activity));
        this.binding.bindableRecyclerview.setHasFixedSize(true);
        this.binding.bindableRecyclerview.setItemAnimator(null);
        this.binding.bindableRecyclerview.setAdapter(adapter);
    }

    //region BindedPresenterView
    @Override
    protected int getLayoutRes() {
        return R.layout.bindable_recyclerview;
    }

    @Override
    public void releaseViews() {
        this.binding.bindableRecyclerview.setAdapter(null);
    }
}
