package is.hello.sense.flows.home.ui.views;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.squareup.picasso.Picasso;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.adapter.InsightsAdapter;
import is.hello.sense.ui.adapter.ParallaxRecyclerScrollListener;
import is.hello.sense.util.DateFormatter;

import static is.hello.go99.animators.MultiAnimator.animatorFor;

@SuppressLint("ViewConstructor")
public final class InsightsView extends PresenterView {

    private static final float UNFOCUSED_CONTENT_SCALE = 0.90f;
    private static final float FOCUSED_CONTENT_SCALE = 1f;
    private static final float UNFOCUSED_CONTENT_ALPHA = 0.95f;
    private static final float FOCUSED_CONTENT_ALPHA = 1f;
    private final RecyclerView recyclerView;
    private final ProgressBar progressBar;
    private final InsightsAdapter insightsAdapter;


    public InsightsView(@NonNull final Activity activity,
                        @NonNull final DateFormatter dateFormatter,
                        @NonNull final Picasso picasso,
                        @NonNull final InsightsAdapter.InteractionListener listener) {
        super(activity);

        this.progressBar = (ProgressBar) findViewById(R.id.fragment_insights_progress);
        this.recyclerView = (RecyclerView) findViewById(R.id.fragment_insights_recycler);
        recyclerView.setHasFixedSize(false);
        setUpStandardRecyclerViewDecorations(recyclerView,
                                             new LinearLayoutManager(context));
        recyclerView.addOnScrollListener(new ParallaxRecyclerScrollListener());
        this.insightsAdapter = new InsightsAdapter(context, dateFormatter, listener, picasso);
        recyclerView.setAdapter(insightsAdapter);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_insights;
    }

    @Override
    public final void releaseViews() {
    }

    public void scrollUp() {
        recyclerView.smoothScrollToPosition(0);
    }

    public final void updateWhatsNewState() {
        if (insightsAdapter != null) {
            insightsAdapter.updateWhatsNewState();
        }
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

    @NonNull
    public Animator getAnimator(final boolean isEnter) {
        if(isEnter) {
            return createRecyclerEnter();
        } else {
            return createRecyclerExit();
        }
    }

    public final void clearCurrentQuestion() {
        insightsAdapter.clearCurrentQuestion();
    }

    private Animator createRecyclerEnter() {
        return animatorFor(recyclerView)
                .scale(UNFOCUSED_CONTENT_SCALE)
                .alpha(UNFOCUSED_CONTENT_ALPHA)
                .addOnAnimationCompleted(finished -> {
                    // If we don't reset this now, Views#getFrameInWindow(View, Rect) will
                    // return a subtly broken value, and the exit transition will be broken.
                    recyclerView.setScaleX(FOCUSED_CONTENT_SCALE);
                    recyclerView.setScaleY(FOCUSED_CONTENT_SCALE);
                    recyclerView.setAlpha(FOCUSED_CONTENT_ALPHA);

                });
    }

    private Animator createRecyclerExit() {
        return animatorFor(recyclerView)
                .addOnAnimationWillStart(animator -> {
                    // Ensure visual consistency.
                    recyclerView.setScaleX(UNFOCUSED_CONTENT_SCALE);
                    recyclerView.setScaleY(UNFOCUSED_CONTENT_SCALE);
                    recyclerView.setAlpha(UNFOCUSED_CONTENT_ALPHA);

                })
                .scale(FOCUSED_CONTENT_SCALE)
                .alpha(FOCUSED_CONTENT_ALPHA);
    }
}
