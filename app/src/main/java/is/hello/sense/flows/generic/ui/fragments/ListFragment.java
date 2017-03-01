package is.hello.sense.flows.generic.ui.fragments;


import android.os.Bundle;
import android.view.View;

import is.hello.sense.flows.generic.ui.activities.ListActivity;
import is.hello.sense.flows.generic.ui.adapters.GenderListAdapter;
import is.hello.sense.flows.generic.ui.views.ListView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.ui.adapter.ArrayRecyclerAdapter;
import is.hello.sense.util.Constants;

public class ListFragment extends PresenterFragment<ListView> {
    private static final String ARG_LIST_TYPE = ListFragment.class.getSimpleName() + ".ARG_LIST_TYPE";

    public static ListFragment newInstance(final int listType) {
        final ListFragment listFragment = new ListFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_LIST_TYPE, listType);
        listFragment.setArguments(args);
        return listFragment;
    }

    @Override
    public void initializePresenterView() {
        if (presenterView == null) {
            presenterView = new ListView(getActivity());
        }
    }

    @Override
    public boolean shouldInject() {
        return false;
    }

    @Override
    public void onViewCreated(final View view,
                              final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle args = getArguments();
        if (args == null) {
            cancelFlow();
            return;
        }
        final int listType = args.getInt(ARG_LIST_TYPE, Constants.NONE);
        if (listType == Constants.NONE) {
            cancelFlow();
            return;
        }
        this.presenterView.setAdapter(getAdapterForType(listType));

    }

    private ArrayRecyclerAdapter getAdapterForType(final int type) {
        switch (type) {
            case ListActivity.GENDER_LIST:
                return new GenderListAdapter(getActivity());
            default:
                throw new IllegalStateException("Invalid list type");
        }
    }
}
