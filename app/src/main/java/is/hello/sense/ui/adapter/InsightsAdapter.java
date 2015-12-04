package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.buruberi.util.Errors;
import is.hello.buruberi.util.StringRef;
import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.Question;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Logger;

public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.BaseViewHolder> {
    @VisibleForTesting static final int TYPE_QUESTION = 0;
    @VisibleForTesting static final int TYPE_INSIGHT = 1;

    private final Context context;
    private final LayoutInflater inflater;
    private final DateFormatter dateFormatter;
    private final InteractionListener interactionListener;
    private final Picasso picasso;

    private @Nullable List<Insight> insights;
    private Question currentQuestion;


    public InsightsAdapter(@NonNull Context context,
                           @NonNull DateFormatter dateFormatter,
                           @NonNull InteractionListener interactionListener,
                           @NonNull Picasso picasso) {
        this.context = context;
        this.dateFormatter = dateFormatter;
        this.picasso = picasso;
        this.inflater = LayoutInflater.from(context);
        this.interactionListener = interactionListener;
    }


    //region Bindings

    public void bindQuestion(@Nullable Question question) {
        interactionListener.onDismissLoadingIndicator();

        this.currentQuestion = question;

        notifyDataSetChanged();
    }

    public void questionUnavailable(Throwable e) {
        Analytics.trackError(e, "Loading questions");
        Logger.error(getClass().getSimpleName(), "Could not load questions", e);

        this.currentQuestion = null;

        notifyDataSetChanged();
    }

    public void bindInsights(@NonNull List<Insight> insights) {
        interactionListener.onDismissLoadingIndicator();

        this.insights = insights;

        notifyDataSetChanged();
    }

    public void insightsUnavailable(Throwable e) {
        Analytics.trackError(e, "Loading Insights");
        Logger.error(getClass().getSimpleName(), "Could not load insights", e);

        interactionListener.onDismissLoadingIndicator();
        this.insights = new ArrayList<>();
        this.currentQuestion = null;

        StringRef messageRef = Errors.getDisplayMessage(e);
        String message = messageRef != null
                ? messageRef.resolve(context)
                : context.getString(R.string.dialog_error_generic_message);
        Insight errorInsight = Insight.createError(message);
        insights.add(errorInsight);

        notifyDataSetChanged();
    }


    public void clearCurrentQuestion() {
        this.currentQuestion = null;
        notifyDataSetChanged();
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
            int adjustedPosition = currentQuestion != null ? position - 1 : position;
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
            return TYPE_INSIGHT;
        }
    }

    //endregion


    //region Views


    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_QUESTION: {
                View view = inflater.inflate(R.layout.sub_fragment_new_question, parent, false);
                return new QuestionViewHolder(view);
            }
            case TYPE_INSIGHT: {
                View view = inflater.inflate(R.layout.item_insight, parent, false);
                return new InsightViewHolder(view);
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


    abstract class BaseViewHolder extends ParallaxRecyclerViewHolder {
        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract void bind(int position);

        @Override
        public void setParallaxPercent(float percent) {

        }
    }

    class QuestionViewHolder extends BaseViewHolder {
        final TextView title;

        QuestionViewHolder(@NonNull View view) {
            super(view);

            this.title = (TextView) view.findViewById(R.id.sub_fragment_new_question_title);

            Button skip = (Button) view.findViewById(R.id.sub_fragment_new_question_skip);
            Views.setSafeOnClickListener(skip, this::skip);

            Button answer = (Button) view.findViewById(R.id.sub_fragment_new_question_answer);
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

    class InsightViewHolder extends BaseViewHolder implements View.OnClickListener {
        private static final float ASPECT_RATIO_SCALE = 0.5f /* 1:2 */;
        private final int parallaxClip, parallaxMaxTranslation;

        final FrameLayout imageAperture; // Contains and clips the image
        final TextView body;
        final TextView date;
        final TextView category;
        final ImageView image;
        final ProgressBar progressBar;

        InsightViewHolder(@NonNull View view) {
            super(view);
            this.parallaxClip = context.getResources().getDimensionPixelSize(R.dimen.parallax_clip);
            this.parallaxMaxTranslation = parallaxClip / 2;
            this.body = (TextView) view.findViewById(R.id.item_insight_body);
            this.date = (TextView) view.findViewById(R.id.item_insight_date);
            this.category = (TextView) view.findViewById(R.id.item_insight_category);
            this.image = (ImageView) view.findViewById(R.id.item_insight_image);
            this.progressBar = (ProgressBar) view.findViewById(R.id.item_insight_progress);
            this.imageAperture = (FrameLayout) view.findViewById(R.id.item_insight_frame);

            view.setOnClickListener(this);

            Views.runWhenLaidOut(itemView, this::onLaidOut);
        }

        @Override
        void bind(int position) {
            Insight insight = getInsightItem(position);
            DateTime insightCreated = insight.getCreated();
            if (insightCreated == null && Insight.CATEGORY_IN_APP_ERROR.equals(insight.getCategory())) {
                date.setText(R.string.dialog_error_title);
                image.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            } else {
                CharSequence insightDate = dateFormatter.formatAsRelativeTime(insightCreated);
                date.setText(insightDate);
                String url = insight.getImageUrl(context.getResources());
                if (url == null) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    picasso.load(url)
                           .fit()
                           .into(image, new Callback() {
                               @Override
                               public void onSuccess() {
                                   progressBar.setVisibility(View.GONE);
                               }

                               @Override
                               public void onError() {
                                   progressBar.setVisibility(View.GONE);
                               }
                           });
                }
                category.setText(insight.getCategory());
            }

            body.setText(insight.getMessage());
        }


        @Override
        public void onClick(View ignored) {
            // View dispatches OnClickListener#onClick(View) calls on
            // the next looper cycle. It's possible for the adapter's
            // containing recycler view to update and invalidate a
            // view holder before the callback fires.
            final int adapterPosition = getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Insight insight = getInsightItem(adapterPosition);
                interactionListener.onInsightClicked(insight);
            }
        }


        private void onLaidOut() {
            // We know our aspect ratio ahead of time due to the images being
            // made in-house, so we use that to our advantage to avoid having
            // to calculate the height of the image in a layout pass.
            final int width = image.getMeasuredWidth();
            final int height = Math.round(width * ASPECT_RATIO_SCALE);

            image.getLayoutParams().height = height;

            // The container is slightly smaller than the image
            // so that we have an aperture to move the image within.
            imageAperture.getLayoutParams().height = height - parallaxClip;

            // Ensure the image is centered, not sitting at the top of
            // the parallax container. See below for more discussion.
            image.setTranslationY(-parallaxMaxTranslation);

            imageAperture.requestLayout();
        }

        @Override
        public void setParallaxPercent(float percent) {
            // Conceptually, the image's natural position is in the center of the parallax
            // container. However, FrameLayout does not layout views that are larger than
            // itself in the center even if told to, so we have to manually adjust our
            // parallax translation to ensure the image moves relative to the center of
            // the container and not the top of the container (which would result in the
            // image having unwanted whitespace; remove -parallaxMaxTranslation to see.)
            //
            // A better implementation of this parallax effect would probably be a custom
            // view that draws an image in its center, and adjusts that center value based
            // on the parallax percentage we get from our scroll listener.
            image.setTranslationY(-parallaxMaxTranslation + (parallaxMaxTranslation * percent));
        }
    }

    //endregion


    public interface InteractionListener {
        void onDismissLoadingIndicator();
        void onSkipQuestion();
        void onAnswerQuestion();
        void onInsightClicked(@NonNull Insight insight);
    }

}
