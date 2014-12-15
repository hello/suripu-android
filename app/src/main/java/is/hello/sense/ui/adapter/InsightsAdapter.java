package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import net.danlew.android.joda.DateUtils;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.util.Markdown;

import static is.hello.sense.ui.animation.PropertyAnimatorProxy.animate;

public class InsightsAdapter extends ViewPagerAdapter<Insight> {
    private final Markdown markdown;
    public final View loadingView;

    public InsightsAdapter(@NonNull Context context,
                           @NonNull Markdown markdown,
                           @NonNull View loadingView) {
        super(context, R.layout.item_insight);

        this.markdown = markdown;
        this.loadingView = loadingView;
    }

    public void bindInsights(@NonNull List<Insight> insights) {
        animate(loadingView)
                .fadeOut(View.GONE)
                .start();

        clear();
        addAll(insights);
    }

    @SuppressWarnings("UnusedParameters")
    public void insightsUnavailable(Throwable e) {
        animate(loadingView)
                .fadeOut(View.GONE)
                .start();

        clear();
    }

    @Override
    protected void configureView(@NonNull View view, int position) {
        TextView title = (TextView) view.findViewById(R.id.item_insight_title);
        TextView body = (TextView) view.findViewById(R.id.item_insight_body);
        TextView date = (TextView) view.findViewById(R.id.item_insight_date);

        Insight insight = getItem(position);
        title.setText(insight.getTitle());
        markdown.render(insight.getMessage())
                .subscribe(body::setText, ignored -> body.setText(R.string.missing_data_placeholder));
    }
}
