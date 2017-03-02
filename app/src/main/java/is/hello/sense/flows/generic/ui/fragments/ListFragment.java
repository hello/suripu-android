package is.hello.sense.flows.generic.ui.fragments;


import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.flows.generic.ui.activities.ListActivity;
import is.hello.sense.flows.generic.ui.adapters.SimpleListAdapter;
import is.hello.sense.flows.generic.ui.views.ListView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.util.Constants;
import is.hello.sense.util.SearchViewStyle;

public class ListFragment extends PresenterFragment<ListView>
        implements SearchView.OnQueryTextListener {
    private static final String ARG_LIST_TYPE = ListFragment.class.getSimpleName() + ".ARG_LIST_TYPE";
    private final SimpleListAdapter adapter = new SimpleListAdapter();

    public static ListFragment newInstance(final int listType) {
        final ListFragment listFragment = new ListFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_LIST_TYPE, listType);
        listFragment.setArguments(args);
        return listFragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            this.presenterView = new ListView(getActivity(), this.adapter);
        }
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu,
                                    final MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);

        // temp solution. todo build a custom inflater in night mode release. Maybe even a custom SearchView too.
        final MenuItem searchItem = menu.findItem(R.id.search);
        final int colorWhite = ContextCompat.getColor(getActivity(), R.color.white);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView == null){
            return; // Safety first. Shouldn't ever happen.
        }
        searchView.setOnQueryTextListener(this);
        SearchViewStyle.on(searchView)
                       .setCursorColor(colorWhite)
                       .setTextColor(colorWhite)
                       .setHintTextColor(colorWhite)
                       .setCloseBtnImageResource(R.drawable.close_white)
                       .setSearchHintDrawable(R.drawable.icon_search_white,
                                              getString(R.string.label_search))
                       .setSearchButtonImageResource(R.drawable.icon_search_white)
                       .setCommitIcon(0);
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
        adapter.addAll(getListForType(listType));

    }

    private List<String> getListForType(final int type) {
        switch (type) {
            case ListActivity.GENDER_LIST:
                return Arrays.asList(getResources().getStringArray(R.array.genders));
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        adapter.setSearchParameters(newText);
        return true;
    }
}
