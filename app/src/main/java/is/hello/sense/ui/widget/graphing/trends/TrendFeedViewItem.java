package is.hello.sense.ui.widget.graphing.trends;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Condition;
import is.hello.sense.api.model.v2.Annotation;
import is.hello.sense.api.model.v2.Graph;
import is.hello.sense.functional.Lists;
import is.hello.sense.ui.widget.RoundedLinearLayout;
import is.hello.sense.ui.widget.ShimmerDividerDrawable;
import is.hello.sense.ui.widget.util.Styles;

@SuppressLint("ViewConstructor")
public class TrendFeedViewItem extends RoundedLinearLayout {
    private final LinearLayout annotationsLayout;
    private final TextView title;
    private final ShimmerDividerDrawable dividerDrawable;
    private final OnBindGraph graphBinder;

    public static ErrorCardView createErrorCard(@NonNull final Context context,
                                                @NonNull final OnRetry onRetry) {
        return new ErrorCardView(context, onRetry);
    }

    public static WelcomeCardView createWelcomeCard(@NonNull final Context context) {
        return new WelcomeCardView(context);
    }

    public static WelcomeBackCardView createWelcomeBackCard(@NonNull final Context context) {
        return new WelcomeBackCardView(context);
    }

    public TrendFeedViewItem(@NonNull final TrendGraphLayout layout) {
        super(layout.getContext());

        LayoutInflater.from(getContext()).inflate(R.layout.item_trend_feed_view, this);

        setOrientation(VERTICAL);
        setBackgroundResource(R.drawable.raised_item_normal);

        final Resources resources = getResources();
        final int padding = resources.getDimensionPixelSize(R.dimen.x2);
        setPadding(padding, 0, padding, padding);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                                         LayoutParams.WRAP_CONTENT));

        final float cornerRadius = resources.getDimension(R.dimen.small_radius);
        setCornerRadii(cornerRadius);

        final View divider = findViewById(R.id.item_trend_feed_view_divider);
        this.dividerDrawable = ShimmerDividerDrawable.createTrendCardDivider(resources);
        divider.setBackground(dividerDrawable);

        this.title = (TextView) findViewById(R.id.item_trend_feed_view_title);
        this.graphBinder = layout;
        this.annotationsLayout = new LinearLayout(getContext());

        final LayoutParams annotationsLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                      LayoutParams.WRAP_CONTENT);
        annotationsLayoutParams.topMargin = resources.getDimensionPixelSize(R.dimen.x2);

        final LayoutParams graphLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                                                                LayoutParams.WRAP_CONTENT);

        addView(layout, graphLayoutParams);
        addView(annotationsLayout, annotationsLayoutParams);
        final Graph graph = layout.getTrendGraphView().getGraph();
        setTitle(graph.getTitle());
        populateAnnotations(graph.getDataType(), graph.getAnnotations());

    }

    public boolean isAnimating() {
        return graphBinder.isAnimating();
    }

    public void setLoading(final boolean loading) {
        if (loading) {
            dividerDrawable.start();
        } else {
            dividerDrawable.stop();
        }
    }

    public void setTitle(final String titleText) {
        title.setText(titleText);
    }

    public void bindGraph(@NonNull final Graph graph) {
        graphBinder.bindGraph(graph);
        populateAnnotations(graph.getDataType(),
                            graph.getAnnotations());
    }

    private void populateAnnotations(@NonNull final Graph.DataType dataType,
                                     @Nullable final List<Annotation> annotations) {
        if (!Lists.isEmpty(annotations)) {
            annotationsLayout.removeAllViews();

            final LayoutInflater inflater = LayoutInflater.from(getContext());
            for (final Annotation annotation : annotations) {
                inflater.inflate(R.layout.item_bargraph_annotation, annotationsLayout);
                final View annotationView = annotationsLayout.getChildAt(annotationsLayout.getChildCount() - 1);

                final TextView titleText =
                        (TextView) annotationView.findViewById(R.id.item_bargraph_annotation_title);
                titleText.setText(annotation.getTitle());

                final @ColorRes int textColor;
                if (dataType.wantsConditionTinting()) {
                    final Condition condition = annotation.getCondition();
                    if (condition == null) {
                        textColor = Condition.UNKNOWN.colorRes;
                    } else {
                        textColor = condition.colorRes;
                    }
                } else {
                    textColor = R.color.trends_bargraph_annotation_text;
                }
                final TextView valueText =
                        ((TextView) annotationView.findViewById(R.id.item_bargraph_annotation_value));
                valueText.setTextColor(ContextCompat.getColor(getContext(), textColor));
                valueText.setText(dataType.renderAnnotation(annotation));
            }

            annotationsLayout.setVisibility(VISIBLE);
        } else {
            annotationsLayout.setVisibility(GONE);
        }
    }

    public interface OnRetry {
        void fetchTrends();
    }

    abstract static class StaticCardLayout extends FrameLayout {
        protected final ImageView image;
        protected final TextView title;
        protected final TextView message;
        protected final Button action;

        StaticCardLayout(@NonNull final Context context) {
            super(context);
            LayoutInflater.from(getContext()).inflate(R.layout.temp_item_message_card, this);
            setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            this.image = (ImageView) findViewById(R.id.item_message_card_image);
            this.title = (TextView) findViewById(R.id.item_message_card_title);
            this.message = (TextView) findViewById(R.id.item_message_card_message);
            this.action = (Button) findViewById(R.id.item_message_card_action);
        }
    }

    static class ErrorCardView extends StaticCardLayout {
        ErrorCardView(@NonNull final Context context, @NonNull final OnRetry onRetry) {
            super(context);

            title.setVisibility(View.GONE);

            action.setText(R.string.action_retry);
            action.setOnClickListener(v -> {
                onRetry.fetchTrends();
            });
            message.setText(R.string.error_trends_unavailable);
        }
    }

    static class WelcomeCardView extends StaticCardLayout {
        WelcomeCardView(@NonNull final Context context) {
            super(context);

            title.setGravity(Gravity.CENTER_HORIZONTAL);
            title.setPadding(0, 0, 0, 0);
            title.setText(getResources().getString(R.string.title_trends_welcome));
            image.setImageResource(R.drawable.trends_first_day);

            ((MarginLayoutParams) message.getLayoutParams()).topMargin = getResources().getDimensionPixelSize(R.dimen.x1);
            message.setText(getResources().getString(R.string.message_trends_welcome));

            Styles.setTextAppearance(message, R.style.Body1_Secondary);
            action.setVisibility(GONE);
        }
    }

    static class WelcomeBackCardView extends WelcomeCardView {
        WelcomeBackCardView(@NonNull final Context context) {
            super(context);
            title.setText(getResources().getString(R.string.title_trends_welcome_back));
            message.setText(getResources().getString(R.string.message_trends_welcome_back));
        }
    }

    public interface OnBindGraph {

        void bindGraph(@NonNull Graph graph);

        boolean isAnimating();

    }
}
