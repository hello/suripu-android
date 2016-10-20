package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;

@SuppressLint("ViewConstructor")
public class VoiceView extends PresenterView {
    private final RecyclerView recyclerView;

    public VoiceView(@NonNull final Activity activity,
                     @NonNull final VoiceCommandsAdapter adapter) {
        super(activity);
        final Resources resources = context.getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        final int inset = resources.getDimensionPixelSize(R.dimen.x1);
        final InsetItemDecoration insetItemDecoration = new InsetItemDecoration();
        insetItemDecoration.addTopInset(0, inset);
        insetItemDecoration.addBottomInset(adapter.getItemCount()-1, inset);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_voice_home_recycler);
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setItemAnimator(null);
        this.recyclerView.setLayoutManager(layoutManager);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(activity));
        this.recyclerView.addItemDecoration(insetItemDecoration);
        this.recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager,
                                                                          resources,
                                                                          FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        this.recyclerView.setAdapter(adapter);

    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_voice_home;
    }

    @Override
    public void releaseViews() {
        this.recyclerView.setAdapter(null);

    }
}
