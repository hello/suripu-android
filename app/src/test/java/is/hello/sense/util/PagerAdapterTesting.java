package is.hello.sense.util;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.annotation.Nullable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public final class PagerAdapterTesting {
    public static Fragment createMockFragment() {
        Fragment fragment = spy(new Fragment());
        doNothing()
                .when(fragment)
                .setUserVisibleHint(anyBoolean());
        doNothing()
                .when(fragment)
                .setMenuVisibility(anyBoolean());
        return fragment;
    }

    public static FragmentTransaction createMockTransaction() {
        final FragmentTransaction transaction = mock(FragmentTransaction.class);
        doReturn(transaction)
                .when(transaction)
                .add(anyInt(), any(Fragment.class), anyString());
        doReturn(transaction)
                .when(transaction)
                .attach(any(Fragment.class));
        doReturn(transaction)
                .when(transaction)
                .remove(any(Fragment.class));
        doReturn(0)
                .when(transaction)
                .commitAllowingStateLoss();
        return transaction;
    }

    @SuppressLint("CommitTransaction")
    public static FragmentManager createMockFragmentManager(@Nullable FragmentTransaction mockTransaction) {
        final FragmentManager fragmentManager = mock(FragmentManager.class);
        doReturn(null)
                .when(fragmentManager)
                .findFragmentById(anyInt());

        if (mockTransaction != null) {
            doReturn(mockTransaction)
                    .when(fragmentManager)
                    .beginTransaction();
        }

        doReturn(true)
                .when(fragmentManager)
                .executePendingTransactions();

        return fragmentManager;
    }
}
