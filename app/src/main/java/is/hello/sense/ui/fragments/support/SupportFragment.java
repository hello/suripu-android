package is.hello.sense.ui.fragments.support;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import is.hello.sense.R;
import is.hello.sense.ui.adapter.SettingsRecyclerAdapter;
import is.hello.sense.ui.common.SenseFragment;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.recycler.FadingEdgesItemDecoration;
import is.hello.sense.ui.recycler.InsetItemDecoration;

public class SupportFragment extends SenseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.static_recycler, container, false);

        final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.static_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final Resources resources = getResources();
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FadingEdgesItemDecoration(layoutManager, resources,
                                                                     FadingEdgesItemDecoration.EDGE_TOP));

        final int verticalPadding = getResources().getDimensionPixelSize(R.dimen.gap_medium);
        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);

        final SettingsRecyclerAdapter adapter = new SettingsRecyclerAdapter(getActivity());
        adapter.setWantsDividers(false);

        decoration.addTopInset(adapter.getItemCount(), verticalPadding);
        final SettingsRecyclerAdapter.DetailItem userGuide =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.action_user_guide),
                                                       this::showUserGuide);
        userGuide.setIcon(R.drawable.icon_settings_user_guide, R.string.action_user_guide);
        adapter.add(userGuide);

        final SettingsRecyclerAdapter.DetailItem contactUs =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.title_contact_us),
                                                       this::contactUs);
        contactUs.setIcon(R.drawable.icon_settings_email, R.string.title_contact_us);
        adapter.add(contactUs);

        decoration.addBottomInset(adapter.getItemCount(), verticalPadding);
        final SettingsRecyclerAdapter.DetailItem myTickets =
                new SettingsRecyclerAdapter.DetailItem(getString(R.string.title_my_tickets),
                                                       this::showMyTickets);
        myTickets.setIcon(R.drawable.icon_settings_my_tickets, R.string.title_my_tickets);
        adapter.add(myTickets);

        recyclerView.setAdapter(adapter);

        return view;
    }


    public void showUserGuide() {
        UserSupport.showUserGuide(getActivity());
    }

    public void contactUs() {
        getFragmentNavigation().pushFragmentAllowingStateLoss(new TicketSelectTopicFragment(),
                                                              getString(R.string.title_select_a_topic), true);
    }

    public void showMyTickets() {
        getFragmentNavigation().pushFragmentAllowingStateLoss(new TicketListFragment(),
                                                              getString(R.string.title_my_tickets), true);
    }
}
