package is.hello.sense.flows.generic.ui.activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import is.hello.sense.flows.generic.ui.fragments.ListFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.util.Constants;

public class ListActivity extends FragmentNavigationActivity {

    public static final int GENDER_LIST = 0;
    private static final String KEY_LIST_TYPE = ListActivity.class.getSimpleName() + ".KEY_LIST_TYPE";

    public static void startActivity(@NonNull final Context context,
                                     final int listType) {
        if (!isValidListType(listType)) {
            return;
        }
        final Intent intent = new Intent(context, ListActivity.class);
        intent.putExtra(KEY_LIST_TYPE, listType);
        context.startActivity(intent);
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onCreateAction() {
        final Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        final int listType = intent.getIntExtra(KEY_LIST_TYPE, Constants.NONE);
        if (!isValidListType(listType)) {
            finish();
            return;
        }
        showListFragment(listType);
    }

    private void showListFragment(final int listType) {
        pushFragment(ListFragment.newInstance(listType), null, false);
    }
}
