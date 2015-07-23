package is.hello.sense.ui.fragments.support;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.zendesk.sdk.model.CommentResponse;
import com.zendesk.sdk.model.network.CommentsResponse;

import java.text.DateFormat;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.zendesk.TicketDetailPresenter;

public class TicketDetailFragment extends InjectionFragment {
    private static final String ARG_TICKET_ID = TicketDetailFragment.class.getName() + ".ARG_TICKET_ID";
    private static final String ARG_TICKET_SUBJECT = TicketDetailFragment.class.getName() + ".ARG_TICKET_SUBJECT";

    @Inject TicketDetailPresenter presenter;

    private CommentAdapter adapter;

    //region Lifecycle

    public static TicketDetailFragment newInstance(@NonNull String ticketId, @NonNull String subject) {
        TicketDetailFragment fragment = new TicketDetailFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_TICKET_ID, ticketId);
        arguments.putString(ARG_TICKET_SUBJECT, subject);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String ticketId = getArguments().getString(ARG_TICKET_ID);
        presenter.setTicketId(ticketId);
        addPresenter(presenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket_detail, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        this.adapter = new CommentAdapter(getActivity());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindAndSubscribe(presenter.comments,
                this::bindComments,
                this::presentError);
    }

    //endregion


    public void bindComments(@NonNull CommentsResponse comments) {
        adapter.clear();
        adapter.addAll(comments.getComments());
    }

    public void presentError(Throwable e) {
        ErrorDialogFragment.presentError(getFragmentManager(), e);
    }


    static class CommentAdapter extends ArrayAdapter<CommentResponse> {
        private final LayoutInflater inflater;

        public CommentAdapter(@NonNull Context context) {
            super(context, R.layout.item_support_ticket);

            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_support_ticket, parent, false);
                view.setTag(new ViewHolder(view));
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            CommentResponse comment = getItem(position);
            holder.description.setText(comment.getBody());
            holder.date.setText(DateFormat.getDateTimeInstance().format(comment.getCreatedAt()));

            return view;
        }

        static class ViewHolder {
            final TextView description;
            final TextView date;

            ViewHolder(@NonNull View view) {
                this.description = (TextView) view.findViewById(R.id.item_support_ticket_description);
                this.date = (TextView) view.findViewById(R.id.item_support_ticket_date);
            }
        }
    }
}
