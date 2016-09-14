package is.hello.sense.ui.fragments.support;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zendesk.sdk.model.Request;

import java.text.DateFormat;
import java.util.List;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.zendesk.TicketsInteractor;

public class TicketListFragment extends InjectionFragment implements AdapterView.OnItemClickListener {
    @Inject
    TicketsInteractor ticketsPresenter;

    private ProgressBar activityIndicator;
    private TextView empty;
    private TicketAdapter adapter;

    //region Lifecycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ticketsPresenter.update();
        addPresenter(ticketsPresenter);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.list_view_static, container, false);

        this.activityIndicator = (ProgressBar) view.findViewById(R.id.list_view_static_loading);
        this.empty = (TextView) view.findViewById(R.id.list_view_static_empty);

        final ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);
        listView.setDivider(ResourcesCompat.getDrawable(getResources(), R.drawable.divider_horizontal_inset, null));

        this.adapter = new TicketAdapter(getActivity());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activityIndicator.setVisibility(View.VISIBLE);
        bindAndSubscribe(ticketsPresenter.tickets,
                         this::bindTickets,
                         this::presentError);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        this.activityIndicator = null;

        adapter.clear();
        this.adapter = null;
    }

    //endregion


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Request ticket = (Request) parent.getItemAtPosition(position);

        final TicketDetailFragment ticketDetailFragment =
                TicketDetailFragment.newInstance(ticket.getId(),
                                                 ticket.getSubject());
        getFragmentNavigation().pushFragmentAllowingStateLoss(ticketDetailFragment,
                                                              ticket.getSubject(), true);
    }


    public void bindTickets(@NonNull List<Request> tickets) {
        activityIndicator.setVisibility(View.GONE);
        adapter.clear();
        if (tickets.isEmpty()) {
            empty.setText(R.string.ticket_list_empty);
            empty.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.GONE);
            adapter.addAll(tickets);
        }
    }

    public void presentError(Throwable e) {
        activityIndicator.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        empty.setText(R.string.dialog_error_title);
        adapter.clear();
        ErrorDialogFragment.presentError(getActivity(), e);
    }


    public static class TicketAdapter extends ArrayAdapter<Request> {
        private final LayoutInflater inflater;

        public TicketAdapter(@NonNull Context context) {
            super(context, R.layout.item_simple_text);

            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_support_ticket, parent, false);
                view.setTag(new ViewHolder(view));
            }

            final ViewHolder holder = (ViewHolder) view.getTag();
            final Request ticket = getItem(position);
            holder.description.setText(ticket.getDescription());
            holder.date.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(ticket.getCreatedAt()));

            return view;
        }

        static class ViewHolder {
            final TextView description;
            final TextView date;

            ViewHolder(@NonNull View view) {
                this.description = (TextView) view.findViewById(R.id.item_support_ticket_description);
                this.date = (TextView) view.findViewById(R.id.item_support_ticket_date);

                description.setMaxLines(3);
            }
        }
    }
}
