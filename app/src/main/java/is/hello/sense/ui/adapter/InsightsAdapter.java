package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.api.model.InsightCategory;
import is.hello.sense.api.model.Question;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.DateFormatter;
import is.hello.sense.util.Errors;
import is.hello.sense.util.Logger;
import is.hello.sense.util.StringRef;

public class InsightsAdapter extends BaseAdapter {
    private static final int TYPE_QUESTION = 0;
    private static final int TYPE_INSIGHT = 1;
    private static final int TYPE_COUNT = 2;

    private final Context context;
    private final LayoutInflater inflater;
    private final DateFormatter dateFormatter;
    private final Listener listener;

    private @Nullable List<Insight> insights;
    private Question currentQuestion;

    public InsightsAdapter(@NonNull Context context,
                           @NonNull DateFormatter dateFormatter,
                           @NonNull Listener listener) {
        this.context = context;
        this.dateFormatter = dateFormatter;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }


    //region Bindings

    public void bindInsights(@NonNull List<Insight> insights) {
        listener.onDismissLoadingIndicator();
        this.insights = insights;
        notifyDataSetChanged();
    }

    public void insightsUnavailable(Throwable e) {
        Analytics.trackError(e, "Loading Insights");
        Logger.error(getClass().getSimpleName(), "Could not load insights", e);

        listener.onDismissLoadingIndicator();
        this.insights = new ArrayList<>();

        StringRef messageRef = Errors.getDisplayMessage(e);
        String message = messageRef != null
                ? messageRef.resolve(context)
                : context.getString(R.string.dialog_error_generic_message);
        Insight errorInsight = Insight.createError(message);
        insights.add(errorInsight);

        notifyDataSetChanged();
    }


    public void bindCurrentQuestion(@Nullable Question currentQuestion) {
        this.currentQuestion = currentQuestion;
        notifyDataSetChanged();
    }

    public void currentQuestionUnavailable(Throwable e) {
        Analytics.trackError(e, "Loading Question");
        Logger.error(getClass().getSimpleName(), "Could not load question", e);

        this.currentQuestion = null;
        notifyDataSetChanged();
    }

    public void clearCurrentQuestion() {
        this.currentQuestion = null;
        notifyDataSetChanged();
    }

    //endregion


    //region Adapter

    @Override
    public int getCount() {
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
    public Object getItem(int position) {
        if (position == 0 && currentQuestion != null) {
            return currentQuestion;
        } else if (insights != null) {
            return getInsightItem(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return TYPE_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && currentQuestion != null) {
            return TYPE_QUESTION;
        } else {
            return TYPE_INSIGHT;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case TYPE_QUESTION: {
                return getQuestionView(convertView, parent);
            }

            case TYPE_INSIGHT: {
                return getInsightView(position, convertView, parent);
            }

            default: {
                throw new IllegalArgumentException();
            }
        }
    }

    //endregion


    //region Views

    private View getQuestionView(View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.sub_fragment_new_question, parent, false);
            view.setTag(new QuestionViewHolder(view));
        }

        QuestionViewHolder holder = (QuestionViewHolder) view.getTag();
        holder.title.setText(currentQuestion.getText());

        return view;
    }

    class QuestionViewHolder {
        final TextView title;

        QuestionViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.sub_fragment_new_question_title);

            Button skip = (Button) view.findViewById(R.id.sub_fragment_new_question_skip);
            Views.setSafeOnClickListener(skip, this::skip);

            Button answer = (Button) view.findViewById(R.id.sub_fragment_new_question_answer);
            Views.setSafeOnClickListener(answer, this::answer);
        }

        public void skip(@NonNull View ignored) {
            listener.onSkipQuestion();
        }

        public void answer(@NonNull View ignored) {
            listener.onAnswerQuestion();
        }
    }


    private View getInsightView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_insight, parent, false);
            view.setTag(new InsightViewHolder(view));
        }

        Insight insight = getInsightItem(position);
        InsightViewHolder holder = (InsightViewHolder) view.getTag();

        DateTime insightCreated = insight.getCreated();
        if (insightCreated == null && insight.getCategory() == InsightCategory.IN_APP_ERROR) {
            holder.date.setText(R.string.dialog_error_title);
        } else {
            CharSequence insightDate = dateFormatter.formatAsRelativeTime(insightCreated);
            holder.date.setText(insightDate);
        }

        if (!TextUtils.isEmpty(insight.getInfoPreview())) {
            holder.previewDivider.setVisibility(View.VISIBLE);
            holder.preview.setVisibility(View.VISIBLE);
            holder.preview.setText(insight.getInfoPreview());
        } else {
            holder.previewDivider.setVisibility(View.GONE);
            holder.preview.setVisibility(View.GONE);
        }

        holder.body.setText(insight.getMessage());

        return view;
    }

    class InsightViewHolder {
        final TextView body;
        final TextView date;
        final View previewDivider;
        final TextView preview;

        InsightViewHolder(@NonNull View view) {
            this.body = (TextView) view.findViewById(R.id.item_insight_body);
            this.date = (TextView) view.findViewById(R.id.item_insight_date);
            this.previewDivider = view.findViewById(R.id.item_insight_preview_divider);
            this.preview = (TextView) view.findViewById(R.id.item_insight_preview);
        }
    }

    //endregion


    public interface Listener {
        void onDismissLoadingIndicator();
        void onSkipQuestion();
        void onAnswerQuestion();
    }
}
