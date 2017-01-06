package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import is.hello.sense.R;
import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.recycler.DividerItemDecoration;
import is.hello.sense.ui.recycler.FirstAndLastItemMarginDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;

@SuppressLint("ViewConstructor")
public class VoiceView extends PresenterView {
    private final RecyclerView recyclerView;
    final InsetItemDecoration insetItemDecorationForWelcome = new InsetItemDecoration();

    public VoiceView(@NonNull final Activity activity,
                     @NonNull final VoiceCommandsAdapter adapter) {
        super(activity);
        final Resources resources = context.getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        this.insetItemDecorationForWelcome.addBottomInset(0, resources.getDimensionPixelSize(R.dimen.x1));
        this.recyclerView = (RecyclerView) findViewById(R.id.view_voice_home_recycler);
        this.recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setItemAnimator(null);
        this.recyclerView.setLayoutManager(layoutManager);
        this.recyclerView.addItemDecoration(new FirstAndLastItemMarginDecoration(resources));
        final Rect dividerInset = new Rect(resources.getDimensionPixelOffset(R.dimen.x8), 0, 0, 0);
        this.recyclerView.addItemDecoration(new DividerItemDecoration(activity,
                                                                      dividerInset));
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

    public void setInsetForWelcomeCard(final boolean add) {
        if (add) {
            this.recyclerView.addItemDecoration(insetItemDecorationForWelcome);
        } else {
            this.recyclerView.removeItemDecoration(insetItemDecorationForWelcome);
        }
    }

    public void scrollUp() {
        recyclerView.smoothScrollToPosition(0);
    }
}
