package is.hello.sense.flows.home.ui.views;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ProgressBar;

import is.hello.sense.R;
import is.hello.sense.api.model.v2.voice.VoiceCommandResponse;
import is.hello.sense.api.model.v2.voice.VoiceCommandTopic;
import is.hello.sense.flows.home.ui.adapters.VoiceCommandsAdapter;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.ui.recycler.FirstAndLastItemMarginDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;

@SuppressLint("ViewConstructor")
public class VoiceView extends PresenterView
        implements
        ArrayRecyclerAdapter.OnItemClickedListener<VoiceCommandTopic> {
    private final RecyclerView recyclerView;
    private final ProgressBar progressBar;
    final InsetItemDecoration insetItemDecorationForWelcome = new InsetItemDecoration();
    private final VoiceCommandsAdapter adapter;
    private final Listener listener;

    public VoiceView(@NonNull final Activity activity,
                     @NonNull final VoiceCommandsAdapter adapter,
                     @NonNull final Listener listener) {
        super(activity);
        this.adapter = adapter;
        this.listener = listener;
        this.adapter.setOnItemClickedListener(this);
        final Resources resources = context.getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        this.insetItemDecorationForWelcome.addBottomInset(0, resources.getDimensionPixelSize(R.dimen.x1));
        this.progressBar = (ProgressBar) findViewById(R.id.view_voice_home_progress);
        this.recyclerView = (RecyclerView) findViewById(R.id.view_voice_home_recycler);
        this.recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        this.recyclerView.setHasFixedSize(true);
        this.recyclerView.setItemAnimator(null);
        this.recyclerView.setLayoutManager(layoutManager);
        this.recyclerView.addItemDecoration(new FirstAndLastItemMarginDecoration(resources));
        this.recyclerView.setAdapter(this.adapter);

    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_voice_home;
    }

    @Override
    public void releaseViews() {
        this.recyclerView.setAdapter(null);
    }

    @Override
    public void onItemClicked(final int position,
                              final VoiceCommandTopic topic) {
        if (topic == null) {
            return;
        }
        this.listener.onTopicClicked(topic);
    }

    public void setInsetForWelcomeCard(final boolean add) {
        if (add) {
            this.recyclerView.addItemDecoration(insetItemDecorationForWelcome);
        } else {
            this.recyclerView.removeItemDecoration(insetItemDecorationForWelcome);
        }
    }

    public void scrollUp() {
        this.recyclerView.smoothScrollToPosition(0);
    }

    public void showWelcomeCard(@Nullable final OnClickListener onClickListener) {
        this.adapter.showWelcomeCard(onClickListener);
        this.setInsetForWelcomeCard(onClickListener != null);
    }

    public void bindVoiceCommands(@NonNull final VoiceCommandResponse response) {
        this.adapter.replaceAll(response.getVoiceCommandTopics());
        this.recyclerView.setVisibility(VISIBLE);
        this.progressBar.setVisibility(GONE);
    }

    public void showError() {
        this.adapter.showError();
        this.recyclerView.setVisibility(VISIBLE);
        this.progressBar.setVisibility(GONE);
    }

    public void showProgress() {
        this.progressBar.setVisibility(VISIBLE);
    }

    public interface Listener {
        void onTopicClicked(@NonNull final VoiceCommandTopic topic);
    }

}
