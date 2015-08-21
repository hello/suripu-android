package is.hello.sense.ui.adapter;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.Test;

import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.PagerAdapterTesting;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class FragmentPagerAdapterTests extends SenseTestCase {
    private static final int PARENT_ID = View.generateViewId();
    private final ViewGroup fakeParent;

    public FragmentPagerAdapterTests() {
        this.fakeParent = new FrameLayout(getContext());
        fakeParent.setId(PARENT_ID);
    }


    //region Tests

    @Test
    public void userVisibilityHint() throws Exception {
        final FragmentTransaction transaction = PagerAdapterTesting.createMockTransaction();
        final FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(transaction);
        final FragmentPagerAdapter adapter = new TestAdapter(fragmentManager);

        adapter.startUpdate(fakeParent);
        Fragment fragment = (Fragment) adapter.instantiateItem(fakeParent, 0);
        adapter.finishUpdate(fakeParent);

        verify(fragment).setMenuVisibility(false);
        verify(fragment).setUserVisibleHint(false);

        adapter.setPrimaryItem(fakeParent, 0, fragment);

        verify(fragment).setMenuVisibility(true);
        verify(fragment).setUserVisibleHint(true);
    }

    @Test
    public void hasPredictableTags() throws Exception {
        final FragmentTransaction transaction = PagerAdapterTesting.createMockTransaction();
        final FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(transaction);
        final FragmentPagerAdapter adapter = new TestAdapter(fragmentManager);

        adapter.startUpdate(fakeParent);
        Fragment fragment = (Fragment) adapter.instantiateItem(fakeParent, 0);
        adapter.finishUpdate(fakeParent);

        String tag = "is:hello:sense:pager:" + PARENT_ID + ":0";
        verify(transaction).add(PARENT_ID, fragment, tag);
    }

    @Test
    public void currentFragment() throws Exception {
        final FragmentTransaction transaction = PagerAdapterTesting.createMockTransaction();
        final FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(transaction);
        final FragmentPagerAdapter adapter = new TestAdapter(fragmentManager);

        adapter.startUpdate(fakeParent);
        Fragment fragment = (Fragment) adapter.instantiateItem(fakeParent, 0);
        adapter.finishUpdate(fakeParent);

        assertThat(adapter.getCurrentFragment(), is(nullValue()));

        adapter.setPrimaryItem(fakeParent, 0, fragment);

        assertThat(adapter.getCurrentFragment(), is(sameInstance(fragment)));
    }

    @Test
    public void addingFragments() throws Exception {
        final FragmentTransaction transaction = PagerAdapterTesting.createMockTransaction();
        final FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(transaction);
        final FragmentPagerAdapter adapter = new TestAdapter(fragmentManager);

        adapter.startUpdate(fakeParent);
        Fragment fragment = (Fragment) adapter.instantiateItem(fakeParent, 0);
        adapter.finishUpdate(fakeParent);

        verify(transaction).add(anyInt(), eq(fragment), anyString());
        verify(transaction).commitAllowingStateLoss();
        verify(transaction, never()).commit();
        verify(fragmentManager).executePendingTransactions();
    }

    @Test
    public void removingFragments() throws Exception {
        final FragmentTransaction transaction = PagerAdapterTesting.createMockTransaction();
        final FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(transaction);
        final FragmentPagerAdapter adapter = new TestAdapter(fragmentManager);

        adapter.startUpdate(fakeParent);
        Fragment fragment = PagerAdapterTesting.createMockFragment();
        adapter.destroyItem(fakeParent, 0, fragment);
        adapter.finishUpdate(fakeParent);

        verify(transaction).remove(fragment);
        verify(transaction).commitAllowingStateLoss();
        verify(transaction, never()).commit();
        verify(fragmentManager).executePendingTransactions();
    }

    @SuppressLint("CommitTransaction")
    @Test
    public void lazyTransactionCreation() throws Exception {
        final FragmentManager fragmentManager = PagerAdapterTesting.createMockFragmentManager(null);
        final FragmentPagerAdapter adapter = new TestAdapter(fragmentManager);

        adapter.startUpdate(fakeParent);
        adapter.finishUpdate(fakeParent);

        verify(fragmentManager, never()).beginTransaction();
        verify(fragmentManager, never()).executePendingTransactions();
    }

    //endregion


    static class TestAdapter extends FragmentPagerAdapter {
        TestAdapter(@NonNull FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            assertThat(position, is(lessThan(getCount())));
            return PagerAdapterTesting.createMockFragment();
        }
    }
}
