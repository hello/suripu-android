package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.FragmentManager;

import org.junit.Test;
import org.robolectric.Robolectric;

import is.hello.sense.graph.SenseTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class LoadingDialogFragmentTest extends SenseTestCase{
    @Test
    public void showDoesNotDismissPreExistingDialogWhenSameFlags() throws Exception {

        final Activity testActivity = Robolectric.setupActivity(Activity.class);
        final FragmentManager fragmentManager = testActivity.getFragmentManager();
        final LoadingDialogFragment preExisting = LoadingDialogFragment.show(fragmentManager,
                                                                             "preExisting",
                                                                             LoadingDialogFragment.OPAQUE_BACKGROUND);
        fragmentManager.executePendingTransactions();
        final LoadingDialogFragment newDialogFragment = LoadingDialogFragment.show(fragmentManager,
                                                                                   "newDialog",
                                                                                   LoadingDialogFragment.OPAQUE_BACKGROUND);
        assertThat(newDialogFragment, equalTo(preExisting));
    }

    @Test
    public void getFlagsReturnsDefault() throws Exception {
        final int flags = new LoadingDialogFragment().getFlags();
        assertThat(flags, equalTo(LoadingDialogFragment.DEFAULTS));
    }

}