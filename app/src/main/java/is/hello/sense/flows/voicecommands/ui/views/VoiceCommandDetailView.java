package is.hello.sense.flows.voicecommands.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;

import is.hello.sense.R;
import is.hello.sense.databinding.BindableRecyclerviewBinding;
import is.hello.sense.flows.voicecommands.ui.adapters.VoiceCommandDetailAdapter;
import is.hello.sense.mvp.view.BindedPresenterView;
import is.hello.sense.ui.recycler.InsetAllItemDecoration;
import is.hello.sense.util.Constants;

@SuppressLint("ViewConstructor")
public class VoiceCommandDetailView extends BindedPresenterView<BindableRecyclerviewBinding> {
    public VoiceCommandDetailView(@NonNull final Activity activity) {
        super(activity);
        setUpStandardRecyclerViewDecorations(binding.bindableRecyclerview, new LinearLayoutManager(activity));
        binding.bindableRecyclerview.addItemDecoration(new InsetAllItemDecoration(getResources(), R.dimen.x2, Constants.NONE));
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.bindable_recyclerview;
    }

    @Override
    public void releaseViews() {
        this.binding.bindableRecyclerview.setAdapter(null);
    }

    public void setAdapter(@NonNull final VoiceCommandDetailAdapter adapter) {
        this.binding.bindableRecyclerview.setAdapter(adapter);
    }
}
