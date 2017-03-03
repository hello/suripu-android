package is.hello.sense.flows.generic.ui.activities;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import is.hello.sense.flows.generic.ui.fragments.ListFragment;
import is.hello.sense.ui.activities.appcompat.FragmentNavigationActivity;
import is.hello.sense.util.Constants;

public class ListActivity extends FragmentNavigationActivity {

    public static final int GENDER_LIST = 0;
    private static final String KEY_LIST_TYPE = ListActivity.class.getSimpleName() + ".KEY_LIST_TYPE";

    public static void startActivityForResult(@NonNull final Fragment fragment,
                                              final int listType,
                                              @Nullable final String initialSelection,
                                              final int requestCode) {
        if (!isValidListType(listType)) {
            return;
        }
        final Intent intent = new Intent(fragment.getActivity(), ListActivity.class);
        intent.putExtra(KEY_LIST_TYPE, listType);
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

    @Override
    public void flowFinished(@NonNull final Fragment fragment,
                             final int responseCode,
                             @Nullable final Intent data) {
        setResult(responseCode, data);
        finish();
    }

    private void showListFragment(final int listType) {
        pushFragment(ListFragment.newInstance(listType), null, false);
    }
}
