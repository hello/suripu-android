package is.hello.sense.mvp.view.home;

import android.animation.Animator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.adapter.ParallaxRecyclerScrollListener;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.util.DateFormatter;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

public final class InsightsView extends PresenterView {

    private static final float UNFOCUSED_CONTENT_SCALE = 0.90f;
    private static final float FOCUSED_CONTENT_SCALE = 1f;
    private static final float UNFOCUSED_CONTENT_ALPHA = 0.95f;
    private static final float FOCUSED_CONTENT_ALPHA = 1f;
    private InsightsAdapter insightsAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private DateFormatter dateFormatter;
    private Picasso picasso;


    public InsightsView(@NonNull final Activity activity,
                        @NonNull final DateFormatter dateFormatter,
                        @NonNull final Picasso picasso) {
        super(activity);
        this.dateFormatter = dateFormatter;
        this.picasso = picasso;
    }

    @NonNull
    @Override
    public final View createView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_insights, container, false);

        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.fragment_insights_refresh_container);
        Styles.applyRefreshLayoutStyle(swipeRefreshLayout);

        this.progressBar = (ProgressBar) view.findViewById(R.id.fragment_insights_progress);

        final Resources resources = context.getResources();
        this.recyclerView = (RecyclerView) view.findViewById(R.id.fragment_insights_recycler);
        recyclerView.setHasFixedSize(false);
        recyclerView.addOnScrollListener(new ParallaxRecyclerScrollListener());
        recyclerView.setItemAnimator(null);
        recyclerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.Style.ROUNDED_EDGES));
        recyclerView.addItemDecoration(new BottomInsetDecoration(resources));
        return view;
    }
    @Override
    public final void detach() {
        super.detach();
        this.swipeRefreshLayout.setOnRefreshListener(null);
        this.dateFormatter = null;
        this.picasso = null;
        this.insightsAdapter = null;
        this.recyclerView = null;
        this.swipeRefreshLayout = null;
        this.progressBar = null;
    }

    public final void setInsightsAdapter(@NonNull final InsightsAdapter.InteractionListener interactionListener) {
        this.insightsAdapter = new InsightsAdapter(context, dateFormatter, interactionListener, picasso);
        recyclerView.setAdapter(insightsAdapter);
    }

    public final void updateWhatsNewState() {
        if (insightsAdapter != null) {
            insightsAdapter.updateWhatsNewState();
        }
    }

    public final void setSwipeRefreshLayoutRefreshListener(@NonNull final SwipeRefreshLayout.OnRefreshListener listener) {
        swipeRefreshLayout.setOnRefreshListener(listener);
    }

    public final void setRefreshing(final boolean refreshing) {
        swipeRefreshLayout.setRefreshing(refreshing);
    }

    public final void showCards(@Nullable final Question question,
                                @NonNull final List<Insight> insights) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.bindQuestion(question);
        insightsAdapter.bindInsights(insights);
    }

    public final void insightsUnavailable(@Nullable final Throwable e,
                                          @NonNull final InsightsAdapter.OnRetry onRetry) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.insightsUnavailable(e, onRetry);
    }

    public final void questionsUnavailable(@Nullable final Throwable e) {
        progressBar.setVisibility(View.GONE);
        insightsAdapter.questionUnavailable(e);
    }

    public Animator getAnimator() {
        if (recyclerView != null) {
            return createRecyclerEnter();
        }
        return createRecyclerExit();
    }

    public void clearCurrentQuestion() {
        insightsAdapter.clearCurrentQuestion();
    }

    private Animator createRecyclerEnter() {
        return animatorFor(recyclerView)
                .scale(UNFOCUSED_CONTENT_SCALE)
                .alpha(UNFOCUSED_CONTENT_ALPHA)
                .addOnAnimationCompleted(finished -> {
                    // If we don't reset this now, Views#getFrameInWindow(View, Rect) will
                    // return a subtly broken value, and the exit transition will be broken.
                    if (recyclerView != null) {
                        recyclerView.setScaleX(FOCUSED_CONTENT_SCALE);
                        recyclerView.setScaleY(FOCUSED_CONTENT_SCALE);
                        recyclerView.setAlpha(FOCUSED_CONTENT_ALPHA);
                    }
                });
    }

    private Animator createRecyclerExit() {
        return animatorFor(recyclerView)
                .addOnAnimationWillStart(animator -> {
                    // Ensure visual consistency.
                    if (recyclerView != null) {
                        recyclerView.setScaleX(UNFOCUSED_CONTENT_SCALE);
                        recyclerView.setScaleY(UNFOCUSED_CONTENT_SCALE);
                        recyclerView.setAlpha(UNFOCUSED_CONTENT_ALPHA);
                    }
                })
                .scale(FOCUSED_CONTENT_SCALE)
                .alpha(FOCUSED_CONTENT_ALPHA);
    }


    static class BottomInsetDecoration extends RecyclerView.ItemDecoration {
        private final int bottomPadding;

        public BottomInsetDecoration(@NonNull final Resources resources) {
            this.bottomPadding = resources.getDimensionPixelSize(R.dimen.x1);
        }

        @Override
        public void getItemOffsets(final Rect outRect, final View view, final RecyclerView parent, final RecyclerView.State state) {
            final int position = parent.getChildAdapterPosition(view);
            if (position == parent.getAdapter().getItemCount() - 1) {
                outRect.bottom = bottomPadding;
            }
        }
    }
}
