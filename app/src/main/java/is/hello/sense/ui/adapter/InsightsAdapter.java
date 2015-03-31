package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.danlew.android.joda.DateUtils;

import java.util.List;

import is.hello.sense.R;
import is.hello.sense.api.model.Insight;
import is.hello.sense.util.Logger;
import is.hello.sense.util.Markdown;

public class InsightsAdapter extends ArrayAdapter<Insight> {
    private final LayoutInflater inflater;
    private final Markdown markdown;
    private final Runnable onDismissLoadingIndicator;

    public InsightsAdapter(@NonNull Context context,
                           @NonNull Markdown markdown,
                           @NonNull Runnable onDismissLoadingIndicator) {
        super(context, R.layout.item_insight);

        this.inflater = LayoutInflater.from(context);
        this.markdown = markdown;
        this.onDismissLoadingIndicator = onDismissLoadingIndicator;
    }

    public void bindInsights(@NonNull List<Insight> insights) {
        onDismissLoadingIndicator.run();
        clear();
        addAll(insights);
    }

    public void insightsUnavailable(Throwable e) {
        Logger.error(InsightsAdapter.class.getSimpleName(), "Could not load insights", e);
        onDismissLoadingIndicator.run();
        clear();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_insight, parent, false);
            view.setTag(new ViewHolder(view));
        }

        Insight insight = getItem(position);
        ViewHolder holder = (ViewHolder) view.getTag();

        holder.title.setText(insight.getTitle());
        markdown.renderInto(holder.body, insight.getMessage());

        CharSequence insightDate = DateUtils.getRelativeTimeSpanString(getContext(), insight.getCreated());
        holder.date.setText(insightDate);

        holder.previewDivider.setVisibility(View.GONE);
        holder.preview.setVisibility(View.GONE);

        return view;
    }

    class ViewHolder {
        final TextView title;
        final TextView body;
        final TextView date;
        final View previewDivider;
        final TextView preview;

        ViewHolder(@NonNull View view) {
            this.title = (TextView) view.findViewById(R.id.item_insight_title);
            this.body = (TextView) view.findViewById(R.id.item_insight_body);
            this.date = (TextView) view.findViewById(R.id.item_insight_date);
            this.previewDivider = view.findViewById(R.id.item_insight_preview_divider);
            this.preview = (TextView) view.findViewById(R.id.item_insight_preview);
        }
    }
}
