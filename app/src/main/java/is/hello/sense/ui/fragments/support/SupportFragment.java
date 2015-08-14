package is.hello.sense.ui.fragments.support;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;

public class SupportFragment extends SenseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());
        adapter.addTextItem(R.string.action_user_guide, 0, ignored -> {
            UserSupport.showUserGuide(getActivity());
        });
        adapter.addTextItem(R.string.title_contact_us, 0, ignored -> {
            ((FragmentNavigation) getActivity()).pushFragmentAllowingStateLoss(new TicketSelectTopicFragment(),
                                                                               getString(R.string.title_select_a_topic), true);
        });
        adapter.addTextItem(R.string.title_my_tickets, 0, ignored -> {
            ((FragmentNavigation) getActivity()).pushFragmentAllowingStateLoss(new TicketListFragment(),
                                                                               getString(R.string.title_my_tickets), true);
        });
        listView.setOnItemClickListener(adapter);
        listView.setAdapter(adapter);

        return view;
    }
}
