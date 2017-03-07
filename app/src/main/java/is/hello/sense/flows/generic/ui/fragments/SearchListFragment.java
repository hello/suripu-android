package is.hello.sense.flows.generic.ui.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.flows.generic.ui.activities.SearchListActivity;
import is.hello.sense.flows.generic.ui.adapters.SearchListAdapter;
import is.hello.sense.flows.generic.ui.views.SearchListView;
import is.hello.sense.mvp.presenters.PresenterFragment;
import is.hello.sense.util.Constants;

public class SearchListFragment extends PresenterFragment<SearchListView>
        implements
        SearchListAdapter.Listener,
        SearchListView.Listener {
    public static final String EXTRA_SELECTION = SearchListFragment.class.getSimpleName() + ".EXTRA_SELECTION";
    private static final String ARG_LIST_TYPE = SearchListFragment.class.getSimpleName() + ".ARG_LIST_TYPE";
    private static final String ARG_INITIAL_SELECTION = SearchListFragment.class.getSimpleName() + ".ARG_INITIAL_SELECTION";

    public static SearchListFragment newInstance(final int listType,
                                                 @Nullable final String initialSelection) {
        final SearchListFragment searchListFragment = new SearchListFragment();
        final Bundle args = new Bundle();
        args.putInt(ARG_LIST_TYPE, listType);
        args.putString(ARG_INITIAL_SELECTION, initialSelection);
        searchListFragment.setArguments(args);
        return searchListFragment;
    }

    //region PresenterFragment
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void initializePresenterView() {
        if (this.presenterView == null) {
            final SearchListAdapter adapter = new SearchListAdapter();
            adapter.setListener(this);
            this.presenterView = new SearchListView(getActivity(), adapter);
            this.presenterView.setListener(this);
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
        this.presenterView.setInitialSelection(args.getString(ARG_INITIAL_SELECTION));
        this.presenterView.addAll(getListForType(listType));
    }

    //region SearchListView.Listener
    @Override
    public void onSelected(@NonNull final String selection) {
        final Intent data = new Intent();
        data.putExtra(EXTRA_SELECTION, selection);
        finishFlowWithResult(Activity.RESULT_OK, data);
    }
    //endregion

    //region SearchListView.Listener
    @Override
    public void onScrolled() {
        final InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() == null) {
            return;
        }
        inputManager.hideSoftInputFromWindow(
                (getActivity()).getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);

    }
    //endregion

    //region methods
    private List<String> getListForType(final int type) {
        switch (type) {
            case SearchListActivity.GENDER_LIST:
                return Arrays.asList(getResources().getStringArray(R.array.genders));
            default:
                return new ArrayList<>();
        }
    }

    //endregion
}
