package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.api.model.SupportTopic;

public class SupportTopicAdapter extends ArrayAdapter<SupportTopic> {
    public SupportTopicAdapter(@NonNull Context context) {
        super(context, R.layout.item_simple_text);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView text = (TextView) super.getView(position, convertView, parent);

        SupportTopic topic = getItem(position);
        text.setText(topic.displayName);

        return text;
    }
}
