package is.hello.sense.flows.generic.ui.activities;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;

import is.hello.sense.R;
import is.hello.sense.flows.generic.ui.fragments.SearchListFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.util.Constants;

public class SearchListActivity extends FragmentNavigationActivity {

    public static final int GENDER_LIST = 0;
    private static final String EXTRA_LIST_TYPE = SearchListActivity.class.getSimpleName() + ".EXTRA_LIST_TYPE";
    private static final String EXTRA_INITIAL_SELECTION = SearchListActivity.class.getSimpleName() + ".EXTRA_INITIAL_SELECTION";
    private int listType = Constants.NONE;

    public static void startActivityForResult(@NonNull final Fragment fragment,
                                              final int listType,
                                              @Nullable final String initialSelection,
                                              final int requestCode) {
        if (!isValidListType(listType)) {
            return;
        }
        final Intent intent = new Intent(fragment.getActivity(), SearchListActivity.class);
        intent.putExtra(EXTRA_LIST_TYPE, listType);
        intent.putExtra(EXTRA_INITIAL_SELECTION, initialSelection);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * Helper function that will check if the given int is one of our supported lists.
     *
     * @param listType represents list to show.
     * @return true if we support this list type.
     */
    private static boolean isValidListType(final int listType) {
        return listType == GENDER_LIST;
    }

    @Override
    protected boolean shouldInjectToMainGraphObject() {
        return false;
    }


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Would be nice if this worked via styles without having to give the activity a parent
        // from the manifest declaration.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getActionbarTitleForType(listType));
        }
        if (savedInstanceState == null){
            showListFragment(listType);
        }
    }

    @Override
    protected void getIntentValues() {
        super.getIntentValues();
        final Intent intent = getIntent();
        if (intent == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " requires an intent.");
        }
        this.listType = intent.getIntExtra(EXTRA_LIST_TYPE, Constants.NONE);
        if (!isValidListType(listType)) {
            throw new IllegalStateException(getClass().getSimpleName() + " requires a list type.");
        }
    }

    @Override
    protected void onCreateAction() {
        showListFragment(listType);
    }

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent data) {
        setResult(responseCode, data);
        finish();
    }

    private void showListFragment(final int listType) {
        pushFragment(SearchListFragment.newInstance(listType, getIntent().getStringExtra(EXTRA_INITIAL_SELECTION)), null, false);
    }


    private String getActionbarTitleForType(final int listType) {
        switch (listType) {
            case GENDER_LIST:
                return getString(R.string.label_gender);
            default:
                return getString(R.string.app_name);
        }
    }
}
