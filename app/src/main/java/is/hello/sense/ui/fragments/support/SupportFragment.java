package is.hello.sense.ui.fragments.support;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.StaticItemAdapter;
import is.hello.sense.ui.common.FragmentNavigationActivity;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;

public class SupportFragment extends SenseFragment implements AdapterView.OnItemClickListener {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_view_static, container, false);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnItemClickListener(this);

        StaticItemAdapter adapter = new StaticItemAdapter(getActivity());
        adapter.addTextItem(R.string.action_user_guide, 0, () -> UserSupport.showUserGuide(getActivity()));
        adapter.addTextItem(R.string.title_contact_us, 0, () -> UserSupport.showContactForm(getActivity()));
        adapter.addTextItem(R.string.title_my_tickets, 0, () -> {
            Bundle arguments = FragmentNavigationActivity.getArguments(getString(R.string.title_my_tickets),
                    TicketListFragment.class, null);

            Intent intent = new Intent(getActivity(), FragmentNavigationActivity.class);
            intent.putExtras(arguments);
            startActivity(intent);
        });
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        StaticItemAdapter.Item item = (StaticItemAdapter.Item) parent.getItemAtPosition(position);
        if (item.getAction() != null) {
            item.getAction().run();
        }
    }
}
