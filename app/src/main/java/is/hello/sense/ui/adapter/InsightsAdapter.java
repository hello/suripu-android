package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.squareup.picasso.Target;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.commonsense.util.Errors;
import is.hello.commonsense.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Question;
import is.hello.sense.api.model.v2.Insight;
import is.hello.sense.ui.widget.ParallaxImageView;
import is.hello.sense.ui.widget.util.Styles;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.BaseViewHolder> {
    @VisibleForTesting static final int TYPE_QUESTION = 0;
    @VisibleForTesting static final int TYPE_INSIGHT = 1;
    @VisibleForTesting static final int TYPE_ERROR = 2;

    private final Context context;
    private final Resources resources;
    private final LayoutInflater inflater;
    private final DateFormatter dateFormatter;
    private final InteractionListener interactionListener;
    private final Picasso picasso;

    private OnRetry onRetry;

    private @Nullable List<Insight> insights;
    private Question currentQuestion;
    private int loadingInsightPosition = RecyclerView.NO_POSITION;


    public InsightsAdapter(@NonNull Context context,
                           @NonNull DateFormatter dateFormatter,
                           @NonNull InteractionListener interactionListener,
                           @NonNull Picasso picasso) {
        this.context = context;
        this.resources = context.getResources();
        this.dateFormatter = dateFormatter;
        this.picasso = picasso;
        this.inflater = LayoutInflater.from(context);
        this.interactionListener = interactionListener;
    }


    //region Bindings

    public void bindQuestion(@Nullable Question question) {
        interactionListener.onDismissLoadingIndicator();

        this.currentQuestion = question;
        this.loadingInsightPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    public void questionUnavailable(@Nullable Throwable e) {
        Analytics.trackError(e, "Loading questions");
        Logger.error(getClass().getSimpleName(), "Could not load questions", e);

        this.currentQuestion = null;
        this.loadingInsightPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    public void bindInsights(@NonNull List<Insight> insights) {
        interactionListener.onDismissLoadingIndicator();

        this.insights = insights;
        this.loadingInsightPosition = RecyclerView.NO_POSITION;

        notifyDataSetChanged();
    }

    public void insightsUnavailable(@Nullable Throwable e, @NonNull InsightsAdapter.OnRetry onRetry) {
        Analytics.trackError(e, "Loading Insights");
        Logger.error(getClass().getSimpleName(), "Could not load insights", e);
        interactionListener.onDismissLoadingIndicator();
        this.insights = new ArrayList<>();
        this.loadingInsightPosition = RecyclerView.NO_POSITION;
        this.currentQuestion = null;
        this.onRetry = onRetry;

        final StringRef messageRef = Errors.getDisplayMessage(e);
        final String message = messageRef != null
                ? messageRef.resolve(context)
                : context.getString(R.string.dialog_error_generic_message);
        final Insight errorInsight = Insight.createError(message);
        insights.add(errorInsight);

        notifyDataSetChanged();
    }


    public void clearCurrentQuestion() {
        this.currentQuestion = null;
        notifyDataSetChanged();
    }

    public void setLoadingInsightPosition(int loadingInsightPosition) {
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
        int count = 0;
        if (insights != null) {
            count += insights.size();
        }

        if (currentQuestion != null) {
            count++;
        }

        return count;
    }

    public Insight getInsightItem(int position) {
        if (insights != null) {
            final int adjustedPosition = currentQuestion != null ? position - 1 : position;
            return insights.get(adjustedPosition);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && currentQuestion != null) {
            return TYPE_QUESTION;
        } else {
            if (position == 0 && getInsightItem(0).isError()){
                return TYPE_ERROR;
            }
            return TYPE_INSIGHT;
        }
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
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
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_QUESTION: {
                final View view = inflater.inflate(R.layout.sub_fragment_new_question, parent, false);
                return new QuestionViewHolder(view);
            }
            case TYPE_INSIGHT: {
                final View view = inflater.inflate(R.layout.item_insight, parent, false);
                return new InsightViewHolder(view);
            }
            case TYPE_ERROR:{
                final View view = inflater.inflate(R.layout.item_message_card, parent, false);
                return new ErrorViewHolder(view);
            }
            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        holder.unbind(false);
    }

    abstract class BaseViewHolder extends ParallaxRecyclerViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(int position);
        void unbind(boolean isFinal) {
        }

        @Override
        public void setParallaxPercent(float percent) {

        }
    }

    class QuestionViewHolder extends BaseViewHolder {
        final TextView title;

        QuestionViewHolder(@NonNull View view) {
            super(view);

            this.title = (TextView) view.findViewById(R.id.sub_fragment_new_question_title);

            final Button skip = (Button) view.findViewById(R.id.sub_fragment_new_question_skip);
            Views.setSafeOnClickListener(skip, this::skip);

            final Button answer = (Button) view.findViewById(R.id.sub_fragment_new_question_answer);
            Views.setSafeOnClickListener(answer, this::answer);
        }


        void skip(@NonNull View ignored) {
            interactionListener.onSkipQuestion();
        }

        void answer(@NonNull View ignored) {
            interactionListener.onAnswerQuestion();
        }

        @Override
        void bind(int ignored) {
            title.setText(currentQuestion.getText());
        }
    }

    public class ErrorViewHolder extends BaseViewHolder implements View.OnClickListener {
        final TextView title;
        final TextView message;
        final Button action;

        ErrorViewHolder(@NonNull View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.item_message_card_title);
            message = (TextView) view.findViewById(R.id.item_message_card_message);
            action = (Button) view.findViewById(R.id.item_message_card_action);
            action.setOnClickListener(this);
        }

        @Override
        void bind(int position) {
            title.setText(R.string.dialog_error_title);
            action.setText(R.string.action_retry);
            message.setText(getInsightItem(position).getMessage());
        }

        @Override
        public void onClick(View v) {
            if (onRetry != null) {
                onRetry.fetchInsights();
            }
        }

    }
    public class InsightViewHolder extends BaseViewHolder implements View.OnClickListener {
        final TextView body;
        final TextView date;
        final TextView category;
        public final ParallaxImageView image;
        final Target target;

        InsightViewHolder(@NonNull View view) {
            super(view);
            this.body = (TextView) view.findViewById(R.id.item_insight_body);
            this.date = (TextView) view.findViewById(R.id.item_insight_date);
            this.category = (TextView) view.findViewById(R.id.item_insight_category);
            this.image = (ParallaxImageView) view.findViewById(R.id.item_insight_image);
            this.target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    image.setDrawable(new BitmapDrawable(context.getResources(), bitmap), false);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    image.setDrawable(placeHolderDrawable, true);
                }
            };
            image.setTag(target);

            view.setOnClickListener(this);
        }

        @Override
        void bind(int position) {
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
                           .into(target);

                } else {
                    picasso.cancelRequest(image);
                    image.setDrawable(null, true);
                }
                image.setVisibility(View.VISIBLE);
                category.setText(insight.getCategoryName());
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
        void unbind(boolean isFinal) {
            image.clearAnimation();

            if (isFinal) {
                image.setDrawable(null, false);
            } else {
                image.showLoadingView();
            }
        }

        @Override
        public void onClick(View ignored) {
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
        public void setParallaxPercent(float percent) {
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
    }
}
