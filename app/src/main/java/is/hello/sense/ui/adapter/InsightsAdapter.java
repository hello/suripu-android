package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;


import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.ApiException;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.ui.widget.ParallaxImageView;
import is.hello.sense.ui.widget.WhatsNewLayout;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.BaseViewHolder> {
    @VisibleForTesting
    static final int TYPE_BANNER = 0;
    @VisibleForTesting
    static final int TYPE_QUESTION = 1;
    @VisibleForTesting
    static final int TYPE_INSIGHT = 2;
    @VisibleForTesting
    static final int TYPE_ERROR = 3;

    private final Context context;
    private final Resources resources;
    private final LayoutInflater inflater;
    private final ApiService apiService;
    private final DateFormatter dateFormatter;
    private final InteractionListener interactionListener;
    private final Picasso picasso;

    private OnRetry onRetry;

    @Nullable
    private List<Insight> insights;
    private Question currentQuestion;
    private int loadingInsightPosition = RecyclerView.NO_POSITION;

    private boolean showWhatsNew = false;


    public InsightsAdapter(@NonNull final Context context,
                           @NonNull final DateFormatter dateFormatter,
                           @NonNull final InteractionListener interactionListener,
                           @NonNull final Picasso picasso,
                           @NonNull final ApiService apiService) {
        this.context = context;
        this.resources = context.getResources();
        this.dateFormatter = dateFormatter;
        this.picasso = picasso;
        this.apiService = apiService;
        this.inflater = LayoutInflater.from(context);
        this.interactionListener = interactionListener;
        this.showWhatsNew = WhatsNewLayout.shouldShow(context);
    }

    public void updateWhatsNewState() {
        if (isErrorState()) {
            return;
        }
        WhatsNewLayout.markShown(context);
        this.showWhatsNew = WhatsNewLayout.shouldShow(context);
        notifyDataSetChanged();
    }

    private boolean isErrorState() {
        return insights != null
                && insights.size() == 1
                && insights.get(0).isError();
    }


    //region Bindings

    public void bindQuestion(@Nullable final Question question) {
        interactionListener.onDismissLoadingIndicator();

        this.currentQuestion = question;
        this.loadingInsightPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    public void questionUnavailable(@Nullable final Throwable e) {
        Analytics.trackError(e, "Loading questions");
        Logger.error(getClass().getSimpleName(), "Could not load questions", e);

        this.currentQuestion = null;
        this.loadingInsightPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    public void bindInsights(@NonNull final List<Insight> insights) {
        interactionListener.onDismissLoadingIndicator();

        this.insights = insights;
        this.loadingInsightPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    public void insightsUnavailable(@Nullable final Throwable e, @NonNull final InsightsAdapter.OnRetry onRetry) {
        Analytics.trackError(e, "Loading Insights");
        Logger.error(getClass().getSimpleName(), "Could not load insights", e);
        interactionListener.onDismissLoadingIndicator();
        this.insights = new ArrayList<>();
        this.loadingInsightPosition = RecyclerView.NO_POSITION;
        this.currentQuestion = null;
        this.onRetry = onRetry;

        final String message;
        if (ApiException.isNetworkError(e)) {
            message = context.getString(R.string.error_insights_unavailable);
        } else {
            final StringRef messageRef = Errors.getDisplayMessage(e);
            if (messageRef != null) {
                message = messageRef.resolve(context);
            } else {
                message = context.getString(R.string.dialog_error_generic_message);
            }
        }
        final Insight errorInsight = Insight.createError(message);
        insights.add(errorInsight);

        notifyDataSetChanged();
    }


    public void clearCurrentQuestion() {
        this.currentQuestion = null;
        notifyDataSetChanged();
    }

    public void setLoadingInsightPosition(final int loadingInsightPosition) {
        if (this.loadingInsightPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(this.loadingInsightPosition);
        }

        this.loadingInsightPosition = loadingInsightPosition;

        if (loadingInsightPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(loadingInsightPosition);
        }
    }

    //endregion


    //region Adapter

    @Override
    public int getItemCount() {
        if (isErrorState()) {
            return 1;
        }
        int count = 0;
        if (insights != null) {
            count += insights.size();
        }

        if (currentQuestion != null) {
            count++;
        }

        if (showWhatsNew) {
            count++;
        }

        return count;
    }

    public Insight getInsightItem(final int position) {
        if (position == 0) {
            if (isErrorState() && insights != null) { // extra check to suppress warning.
                return insights.get(0);
            }
            if (showWhatsNew) {
                return null;
            }
            if (currentQuestion != null) {
                return null;
            }
        } else if (position == 1 && showWhatsNew && currentQuestion != null) {
            return null;
        }
        int offset = 0;
        if (showWhatsNew) {
            offset++;
        }
        if (currentQuestion != null) {
            offset++;
        }

        final int positionOffset = position - offset;
        if (insights != null && positionOffset < insights.size()) {
            return insights.get(positionOffset);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public int getItemViewType(final int position) {
        if (position == 0) {
            if (isErrorState()) {
                return TYPE_ERROR;
            }
            if (showWhatsNew) {
                return TYPE_BANNER;
            }
            if (currentQuestion != null) {
                return TYPE_QUESTION;
            }
        } else if (position == 1 && showWhatsNew && currentQuestion != null) {
            return TYPE_QUESTION;
        }
        return TYPE_INSIGHT;
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            final View view = recyclerView.getChildAt(i);
            final BaseViewHolder holder = (BaseViewHolder) recyclerView.getChildViewHolder(view);
            holder.unbind(true);
        }
    }
    //endregion


    //region Views

    @Override
    public BaseViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case TYPE_BANNER: {
                final WhatsNewLayout layout = new WhatsNewLayout(context);
                layout.setListener(() -> {
                    showWhatsNew = false;
                    notifyDataSetChanged();
                });
                return new WhatsNewViewHolder(layout);
            }
            case TYPE_QUESTION: {
                final View view = inflater.inflate(R.layout.sub_fragment_new_question, parent, false);
                return new QuestionViewHolder(view);
            }
            case TYPE_INSIGHT: {
                final View view = inflater.inflate(R.layout.item_insight, parent, false);
                return new InsightViewHolder(view);
            }
            case TYPE_ERROR: {
                final View view = inflater.inflate(R.layout.item_message_card, parent, false);
                final int margin = context.getResources().getDimensionPixelSize(R.dimen.x1);
                final RecyclerView.LayoutParams lp = ((RecyclerView.LayoutParams) view.getLayoutParams());
                lp.setMargins(margin, margin, margin, margin);
                return new ErrorViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(final BaseViewHolder holder, final int position) {
        holder.bind(position);
    }

    @Override
    public void onViewRecycled(final BaseViewHolder holder) {
        holder.unbind(false);
    }

    abstract class BaseViewHolder extends ParallaxRecyclerViewHolder {
        BaseViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        abstract void bind(final int position);

        void unbind(final boolean isFinal) {
        }

        @Override
        public void setParallaxPercent(final float percent) {

        }
    }

    class WhatsNewViewHolder extends BaseViewHolder {

        WhatsNewViewHolder(@NonNull final View itemView) {
            super(itemView);
        }

        @Override
        void bind(final int position) {

        }
    }

    class QuestionViewHolder extends BaseViewHolder {
        final TextView title;

        QuestionViewHolder(@NonNull final View view) {
            super(view);

            this.title = (TextView) view.findViewById(R.id.sub_fragment_new_question_title);

            final Button skip = (Button) view.findViewById(R.id.sub_fragment_new_question_skip);
            Views.setSafeOnClickListener(skip, this::skip);

            final Button answer = (Button) view.findViewById(R.id.sub_fragment_new_question_answer);
            Views.setSafeOnClickListener(answer, this::answer);
        }


        void skip(@NonNull final View ignored) {
            interactionListener.onSkipQuestion();
        }

        void answer(@NonNull final View ignored) {
            interactionListener.onAnswerQuestion();
        }

        @Override
        void bind(final int ignored) {
            title.setText(currentQuestion.getText());
        }
    }

    public class ErrorViewHolder extends BaseViewHolder implements View.OnClickListener {
        final TextView message;
        final Button action;

        ErrorViewHolder(@NonNull final View view) {
            super(view);

            final TextView title = (TextView) view.findViewById(R.id.item_message_card_title);
            title.setVisibility(View.GONE);

            this.message = (TextView) view.findViewById(R.id.item_message_card_message);
            this.action = (Button) view.findViewById(R.id.item_message_card_action);
            action.setOnClickListener(this);
        }

        @Override
        void bind(final int position) {
            action.setText(R.string.action_retry);
            message.setText(getInsightItem(position).getMessage());
        }

        @Override
        public void onClick(final View v) {
            if (onRetry != null) {
                onRetry.fetchInsights();
            }
        }

    }

    public class InsightViewHolder extends BaseViewHolder implements View.OnClickListener {
        final TextView body;
        final TextView date;
        final TextView category;
        final TextView share;
        public final ParallaxImageView image;

        InsightViewHolder(@NonNull final View view) {
            super(view);
            this.body = (TextView) view.findViewById(R.id.item_insight_body);
            this.date = (TextView) view.findViewById(R.id.item_insight_date);
            this.category = (TextView) view.findViewById(R.id.item_insight_category);
            this.image = (ParallaxImageView) view.findViewById(R.id.item_insight_image);
            this.share = (TextView) view.findViewById(R.id.item_insight_share);

            view.setOnClickListener(this);
        }

        @Override
        void bind(final int position) {
            final Insight insight = getInsightItem(position);
            final DateTime insightCreated = insight.getCreated();
            if (insightCreated == null && Insight.CATEGORY_IN_APP_ERROR.equals(insight.getCategory())) {
                date.setText(R.string.dialog_error_title);
                image.setVisibility(View.GONE);
            } else {
                final CharSequence insightDate = dateFormatter.formatAsRelativeTime(insightCreated);
                date.setText(insightDate);
                final String url = insight.getImageUrl(context.getResources());
                if (url != null) {
                    final int maxWidth = resources.getDisplayMetrics().widthPixels;
                    final int maxHeight = Math.round(maxWidth * image.getAspectRatioScale());
                    picasso.load(url)
                           .resize(maxWidth, maxHeight)
                           .into(image);
                } else {
                    picasso.cancelRequest(image);
                    image.setDrawable(null, true);
                }
                image.setVisibility(View.VISIBLE);
                category.setText(insight.getCategoryName());
                if (insight.getId() == null) {
                    share.setVisibility(View.GONE);
                    share.setOnClickListener(null);
                } else {
                    share.setVisibility(View.VISIBLE);
                    share.setOnClickListener(v -> {
                        interactionListener.showProgress(true);
                        apiService.shareInsight(insight.getInsightType())
                                  .doOnTerminate(() -> interactionListener.showProgress(false))
                                  .subscribe(shareUrl -> {
                                                 interactionListener.onShareUrl(shareUrl.getUrl());
                                             },
                                             throwable -> {
                                                 //todo error state
                                             });
                    });
                }
            }

            body.setText(Styles.darkenEmphasis(resources, insight.getMessage()));

            if (position == loadingInsightPosition) {
                itemView.setAlpha(0.8f);
                itemView.setClickable(false);
            } else {
                itemView.setAlpha(1f);
                itemView.setClickable(true);
            }
        }

        @Override
        void unbind(final boolean isFinal) {
            image.clearAnimation();

            if (isFinal) {
                image.setDrawable(null, false);
            } else {
                image.showLoadingView();
            }
        }

        @Override
        public void onClick(final View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                interactionListener.onInsightClicked(this);
            }
        }

        public Insight getInsight() {
            return getInsightItem(getAdapterPosition());
        }

        @Override
        public void setParallaxPercent(final float percent) {
            image.setParallaxPercent(percent);
        }
    }

    //endregion
    public interface OnRetry {
        void fetchInsights();
    }

    public interface InteractionListener {
        void onDismissLoadingIndicator();

        void onSkipQuestion();

        void onAnswerQuestion();

        void onInsightClicked(@NonNull InsightViewHolder viewHolder);

        void onShareUrl(@NonNull String url);

        void showProgress(boolean show);
    }
}
